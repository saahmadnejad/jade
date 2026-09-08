package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

/**
 * Deterministic coordinator of the development team (no LLM calls). Walks the
 * team through Rounds - design, implement, test, review - routing Artifacts
 * between Role Agents and stopping on Verdict approval or when caps are hit.
 */
public class ManagerAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(ManagerAgent.class.getName());

    private enum Phase { CLARIFY, DESIGN, IMPLEMENT, TEST, REVIEW, PUBLISH, DONE }

    /**
     * Protocol for task assignment messages sent to Role Agents. Same
     * FIPA-standard value ({@code fipa-request}) as the peer leg
     * ({@link RoleAgent#PEER_PROTOCOL}); both must reference the spec
     * constant, never hand-written literals (protocol names are
     * case-sensitive).
     */
    static final String TASK_PROTOCOL =
        FIPANames.InteractionProtocol.FIPA_REQUEST;

    private String teamId;
    private Workspace workspace;
    private Phase phase = Phase.CLARIFY;
    private int round = 1;
    private int callsUsed = 0;
    private long deadlineAt;
    private String brief;
    private String designDoc = "";
    private String clarifications = "";
    private String reviewFeedback = "";
    private String githubOrg;
    private String githubVisibility;
    private java.nio.file.Path workDir;
    private int maxRounds;
    private int maxTotalCalls;
    private long roundStartTimeoutMin;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        brief = str(args, 0, "");
        maxRounds = intArg(args, 1, 3);
        maxTotalCalls = intArg(args, 2, 12);
        roundStartTimeoutMin = intArg(args, 3, 30);
        String workspaceDirParam = str(args, 4, null);
        githubOrg = str(args, 5, null);
        githubVisibility = str(args, 6, "private");

        // Agent local name is "<instance>-manager"; instance names never end
        // in "-manager", so stripping the suffix recovers the team id.
        String local = getLocalName();
        teamId = local.endsWith("-manager") ? local.substring(0, local.length() - "-manager".length()) : local;
        workDir = TeamDirs.resolve(teamId, workspaceDirParam);
        workspace = WorkspaceStore.getOrCreate(teamId,
            workspaceDirParam != null && !workspaceDirParam.isBlank()
                ? java.nio.file.Path.of(workspaceDirParam.trim()) : null);
        deadlineAt = System.currentTimeMillis() + roundStartTimeoutMin * 60_000L;

        // Visible on the DF page as the team's coordinator.
        try {
            io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription dfd =
                new io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription();
            dfd.setName(getAID());
            io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription sd =
                new io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription();
            sd.setType("devteam-manager");
            sd.setName("coordinator:" + teamId);
            dfd.addServices(sd);
            // Also register as peer-discoverable "manager" role
            io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription sdPeer =
                new io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription();
            sdPeer.setType("devteam-role");
            sdPeer.setName("manager");
            sdPeer.addProperties(new io.donbee.jade.domain.FIPAAgentManagement.Property("team", teamId));
            dfd.addServices(sdPeer);
            io.donbee.jade.domain.DFService.register(this, dfd);
        } catch (io.donbee.jade.domain.FIPAException e) {
            LOG.warning("team=" + teamId + ": DF registration failed: " + e.getMessage());
        }

        if (brief == null || brief.isBlank()) {
            finish("FAILED", "No brief provided. Tell the team what to develop: fill the "
                + "'brief' field in the scenario config (like a README) and start again.");
            return;
        }

        try {
            TeamScaffolder.scaffold(workDir, githubOrg, teamId + "-project", githubVisibility);
        } catch (IOException e) {
            LOG.warning("team=" + teamId + ": scaffolding failed: " + e.getMessage());
        }

        boolean githubWanted = githubOrg != null && !githubOrg.isBlank();
        boolean ghTokenMissing = githubWanted && !githubTokenAvailable();
        if (ghTokenMissing) {
            finish("FAILED", "githubOrg='" + githubOrg + "' requires the GH_TOKEN environment "
                + "variable (a PAT with Administration+Contents+Issues access to the org). "
                + "Set it and restart the instance.");
            return;
        }

        LOG.info("team=" + teamId + ": goal: " + firstLine(brief)
            + " (maxRounds=" + maxRounds + ", maxCalls=" + maxTotalCalls
            + ", brain=langchain4j, dir=" + workDir + ")"
            + (githubWanted ? " github=" + githubOrg : ""));
        workspace.save("BRIEF.md", "# Brief\n\n" + brief + "\n");

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate teamReplies = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchPerformative(ACLMessage.FAILURE));

            @Override
            public void action() {
                ACLMessage reply = myAgent.receive(teamReplies);
                if (reply == null) {
                    block();
                    return;
                }
                String convId = reply.getConversationId();
                // Only handle Manager's own phase messages, ignore peer (dt-peer-*) messages
                if (convId == null || !convId.startsWith("dt-") || convId.startsWith("dt-peer-")) {
                    return; // not ours
                }
                callsUsed++;
                handleReply(reply);
            }
        });

        // Watchdog: a role agent that never replies (provider hang) must not
        // stall the team forever — enforce the phase deadline even while
        // blocked on receive().
        addBehaviour(new io.donbee.jade.core.behaviours.TickerBehaviour(this, 60_000L) {
            @Override
            protected void onTick() {
                if (phase != Phase.DONE && System.currentTimeMillis() > deadlineAt) {
                    finish("TIMEOUT", "Time budget exhausted during " + phase
                        + " (no reply from role agent)");
                }
            }
        });

        // Kick off phase 1: optionally clarify, then design.
        if (Boolean.parseBoolean(str(args, 7, "true"))) {
            sendTo("architect", clarifyTask(), "dt-clarify-" + round);
            enterPhase(Phase.CLARIFY);
        } else {
            sendTo("architect", designTask(), "dt-design-" + round);
            enterPhase(Phase.DESIGN);
        }
    }

    private void handleReply(ACLMessage reply) {
        if (reply.getPerformative() == ACLMessage.FAILURE) {
            finish("FAILED", "A role agent failed: " + reply.getContent());
            return;
        }
        if (System.currentTimeMillis() > deadlineAt) {
            finish("TIMEOUT", "Time budget exhausted during " + phase);
            return;
        }

        switch (phase) {
            case CLARIFY -> {
                clarifications = reply.getContent();
                workspace.save("CLARIFICATIONS.md", clarifications);
                enterPhase(Phase.DESIGN);
                sendTo("architect", designTask(), "dt-design-" + round);
            }
            case DESIGN -> {
                designDoc = reply.getContent();
                saveArtifacts(designDoc, "design");
                workspace.save("DESIGN.md", designDoc);
                enterPhase(Phase.IMPLEMENT);
                sendTo("implementer", implementTask(), "dt-implement-" + round);
            }
            case IMPLEMENT -> {
                saveArtifacts(reply.getContent(), "src");
                enterPhase(Phase.TEST);
                sendTo("tester", testTask(reply.getContent()), "dt-test-" + round);
            }
            case TEST -> {
                String report = reply.getContent();
                saveArtifacts(report, "tests");
                workspace.save("TEST-REPORT-round" + round + ".md", report);
                enterPhase(Phase.REVIEW);
                sendTo("reviewer", reviewTask(report), "dt-review-" + round);
            }
            case REVIEW -> {
                String reviewText = reply.getContent();
                workspace.save("REVIEW-round" + round + ".md", reviewText);
                boolean approved = reviewText.toUpperCase()
                    .contains(ReviewerAgent.VERDICT_APPROVED.toUpperCase());
                if (!approved) {
                    reviewFeedback = extractChanges(reviewText);
                }
                evaluateNextRound(approved, reviewText);
            }
            case PUBLISH -> {
                String url = firstLine(reply.getContent());
                workspace.save("GITHUB.md",
                    "# Published\n\n" + url + "\n\n" + reply.getContent());
                finish("PUBLISHED", "Team result published to " + url);
            }
            default -> { /* DONE */ }
        }
    }

    /**
     * Enter a phase and restart its budget. The deadline is per phase, not
     * for the whole team lifetime: long multi-round runs otherwise die in
     * round 2+ even while healthy.
     */
    private void enterPhase(Phase p) {
        phase = p;
        deadlineAt = System.currentTimeMillis() + roundStartTimeoutMin * 60_000L;
    }

    private void evaluateNextRound(boolean approved, String reviewText) {
        if (approved && githubWanted() && callsUsed < maxTotalCalls) {
            // One extra call: the implementer publishes the project to GitHub itself.
            LOG.info("team=" + teamId + ": approved - asking implementer to publish to GitHub");
            sendTo("implementer", publishTask(), "dt-publish-" + round);
            phase = Phase.PUBLISH;
            return;
        }
        if (approved) {
            finish("APPROVED", "The reviewer approved the result in round " + round
                + "; call budget too low for the GitHub publish step.");
            return;
        }
        if (round >= maxRounds) {
            finish("CAP_ROUNDS", "Reached maxRounds=" + maxRounds + " without approval.");
            return;
        }
        if (callsUsed + 4 > maxTotalCalls) { // a full remaining round needs ~4 calls
            finish("CAP_CALLS", "Stopping before round " + (round + 1)
                + ": call budget would exceed maxTotalCalls=" + maxTotalCalls + ".");
            return;
        }
        round++;
        LOG.info("team=" + teamId + ": starting round " + round + " with reviewer feedback");
        enterPhase(Phase.IMPLEMENT);
        sendTo("implementer", implementTask(), "dt-implement-" + round);
    }

    private boolean githubWanted() {
        return githubOrg != null && !githubOrg.isBlank();
    }

    @Override
    protected void takeDown() {
        try {
            io.donbee.jade.domain.DFService.deregister(this);
        } catch (Exception ignored) {
            // Already gone
        }
    }

    private static boolean githubTokenAvailable() {
        try {
            return !SecretsResolver.resolveGithubToken(System::getenv, java.nio.file.Path.of("")).isBlank();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void finish(String status, String reason) {
        phase = Phase.DONE;
        workspace.save("STATUS.md", "# Status: " + status + "\n\n" + reason + "\n\n"
            + "- rounds used: " + round + "\n- LLM calls used: " + callsUsed
            + "\n- finished: " + Instant.now() + "\n");
        LOG.info("team=" + teamId + ": FINISHED status=" + status
            + " rounds=" + round + " llmCalls=" + callsUsed + " - " + reason);
    }

    // ---- task builders -------------------------------------------------

    private void sendTo(String roleSuffix, String content, String conversationId) {
        AID peer = findPeer(roleSuffix);
        if (peer == null) {
            LOG.warning("team=" + teamId + ": sendTo: peer not found for " + roleSuffix
                + ", falling back to hardcoded AID");
            peer = new AID(teamId + "-" + roleSuffix, AID.ISLOCALNAME);
        }
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(peer);
        msg.setProtocol(TASK_PROTOCOL);
        msg.setConversationId(conversationId);
        msg.setContent(content);
        send(msg);
    }

    /** Find a peer agent by role suffix on the same team using DF. */
    private AID findPeer(String roleSuffix) {
        try {
            io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription template =
                new io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription();
            io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription sd =
                new io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription();
            sd.setType("devteam-role");
            sd.setName(roleSuffix);
            sd.addProperties(new io.donbee.jade.domain.FIPAAgentManagement.Property("team", teamId));
            template.addServices(sd);
            io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription[] results =
                io.donbee.jade.domain.DFService.search(this, null, template, null);
            if (results.length > 0) {
                return results[0].getName();
            }
        } catch (io.donbee.jade.domain.FIPAException e) {
            LOG.warning("team=" + teamId + ": DF lookup failed for " + roleSuffix + ": " + e.getMessage());
        }
        return null;
    }

    private String clarifyTask() {
        return "Project brief from the product side:\n\n" + brief
            + "\n\nBefore designing, run a short self-interview (grilling skill): list up to 5 "
            + "clarifying questions the brief leaves open. For each question, also state the "
            + "answer you will assume, so the team can proceed without waiting.\n"
            + "Output format:\n\n## Clarifying questions\n\n"
            + "1. Q: <question>\n   Assumed answer: <your assumption>\n\n"
            + "Then stop - the design comes in your next task.";
    }

    private String publishTask() {
        return "The team approved your implementation. Publish it to GitHub now:\n\n"
            + "- Repository: `" + githubOrg + "/" + teamId + "-project` (" + githubVisibility + ")\n"
            + "- `git init`, commit ALL workspace files (src, tests, docs), create the repo "
            + "under the org with `gh repo create`, push the main branch.\n"
            + "- `gh` and `git` are authenticated via GH_TOKEN already.\n"
            + "- Reply with the repository URL as the first line of your answer.";
    }

    private String designTask() {
        StringBuilder sb = new StringBuilder("Project brief:\n\n").append(brief).append("\n\n");
        if (!clarifications.isBlank()) {
            sb.append("Clarified questions and assumed answers (from your previous task):\n\n")
              .append(clarifications).append("\n\n");
        }
        sb.append("Produce the technical design following your output contract.");
        return sb.toString();
    }

    private String implementTask() {
        StringBuilder sb = new StringBuilder();
        sb.append("Technical design:\n\n").append(designDoc).append("\n\n");
        if (!reviewFeedback.isBlank()) {
            sb.append("Reviewer feedback from the previous round (address it):\n")
              .append(reviewFeedback).append("\n\n");
        }
        sb.append("Implement ALL files now, following your output contract.");
        return sb.toString();
    }

    private String testTask(String implementedCode) {
        return "Design:\n\n" + designDoc + "\n\nImplemented source files:\n\n"
            + implementedCode + "\n\nWrite the tests and the test report following your output contract.";
    }

    private String reviewTask(String testReport) {
        StringBuilder files = new StringBuilder();
        for (Map.Entry<String, String> e : workspace.snapshot().entrySet()) {
            if (e.getKey().startsWith("src/") || e.getKey().startsWith("tests/")) {
                files.append("--- ").append(e.getKey()).append(" ---\n")
                     .append(e.getValue()).append("\n\n");
            }
        }
        return "Design:\n\n" + designDoc + "\n\nAll source and test files:\n\n"
            + files + "\nTest report:\n\n" + testReport
            + "\n\nGive your review and end with the verdict line.";
    }

    private void saveArtifacts(String text, String prefix) {
        for (Map.Entry<String, String> e : ArtifactParser.parse(text).entrySet()) {
            workspace.save(prefix + "/" + e.getKey(), e.getValue());
        }
    }

    /** The bullet list after 'changes-requested', if any. */
    static String extractChanges(String reviewText) {
        int idx = reviewText == null ? -1 : reviewText.indexOf("changes-requested");
        if (idx < 0) {
            return "";
        }
        String tail = reviewText.substring(idx);
        int verdictEnd = tail.indexOf('\n');
        return verdictEnd >= 0 && verdictEnd < tail.length() - 1
            ? tail.substring(verdictEnd + 1).trim()
            : "";
    }

    static String firstLine(String s) {
        if (s == null) return "";
        int nl = s.indexOf('\n');
        String line = nl > 0 ? s.substring(0, nl) : s;
        return line.length() > 100 ? line.substring(0, 100) + "..." : line;
    }

    static String str(Object[] args, int i, String def) {
        return args != null && args.length > i && args[i] != null ? args[i].toString() : def;
    }

    static int intArg(Object[] args, int i, int def) {
        try {
            return Integer.parseInt(str(args, i, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
