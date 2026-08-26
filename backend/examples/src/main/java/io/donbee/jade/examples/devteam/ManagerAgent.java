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

    private enum Phase { CLARIFY, DESIGN, IMPLEMENT, TEST, REVIEW, PUBLISH, DONE }

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
            io.donbee.jade.domain.DFService.register(this, dfd);
        } catch (io.donbee.jade.domain.FIPAException e) {
            System.err.println("[devteam:" + teamId + "] DF registration failed: " + e.getMessage());
        }

        if (brief == null || brief.isBlank()) {
            finish("FAILED", "No brief provided. Tell the team what to develop: fill the "
                + "'brief' field in the scenario config (like a README) and start again.");
            return;
        }

        try {
            TeamScaffolder.scaffold(workDir, githubOrg, teamId + "-project", githubVisibility);
        } catch (IOException e) {
            System.err.println("[devteam:" + teamId + "] scaffolding failed: " + e.getMessage());
        }

        boolean githubWanted = githubOrg != null && !githubOrg.isBlank();
        boolean ghTokenMissing = githubWanted && !githubTokenAvailable();
        if (ghTokenMissing) {
            finish("FAILED", "githubOrg='" + githubOrg + "' requires the GH_TOKEN environment "
                + "variable (a PAT with Administration+Contents+Issues access to the org). "
                + "Set it and restart the instance.");
            return;
        }

        System.out.println("[devteam:" + teamId + "] goal: " + firstLine(brief)
            + " (maxRounds=" + maxRounds + ", maxCalls=" + maxTotalCalls
            + ", brain=opencode, dir=" + workDir + ")"
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
                if (!reply.getConversationId().startsWith("dt-")) {
                    return; // not ours
                }
                callsUsed++;
                handleReply(reply);
            }
        });

        // Kick off phase 1: optionally clarify, then design.
        if (Boolean.parseBoolean(str(args, 7, "true"))) {
            sendTo("architect", clarifyTask(), "dt-clarify-" + round);
            phase = Phase.CLARIFY;
        } else {
            sendTo("architect", designTask(), "dt-design-" + round);
            phase = Phase.DESIGN;
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
                sendTo("architect", designTask(), "dt-design-" + round);
                phase = Phase.DESIGN;
            }
            case DESIGN -> {
                designDoc = reply.getContent();
                saveArtifacts(designDoc, "design");
                workspace.save("DESIGN.md", designDoc);
                sendTo("implementer", implementTask(), "dt-implement-" + round);
                phase = Phase.IMPLEMENT;
            }
            case IMPLEMENT -> {
                saveArtifacts(reply.getContent(), "src");
                sendTo("tester", testTask(reply.getContent()), "dt-test-" + round);
                phase = Phase.TEST;
            }
            case TEST -> {
                String report = reply.getContent();
                saveArtifacts(report, "tests");
                workspace.save("TEST-REPORT-round" + round + ".md", report);
                sendTo("reviewer", reviewTask(report), "dt-review-" + round);
                phase = Phase.REVIEW;
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

    private void evaluateNextRound(boolean approved, String reviewText) {
        if (approved && githubWanted() && callsUsed < maxTotalCalls) {
            // One extra call: the implementer publishes the project to GitHub itself.
            System.out.println("[devteam:" + teamId + "] approved - asking implementer to publish to GitHub");
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
        System.out.println("[devteam:" + teamId + "] starting round " + round + " with reviewer feedback");
        sendTo("implementer", implementTask(), "dt-implement-" + round);
        phase = Phase.IMPLEMENT;
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
        System.out.println("[devteam:" + teamId + "] FINISHED status=" + status
            + " rounds=" + round + " llmCalls=" + callsUsed + " - " + reason);
    }

    // ---- task builders -------------------------------------------------

    private void sendTo(String roleSuffix, String content, String conversationId) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(teamId + "-" + roleSuffix, AID.ISLOCALNAME));
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        msg.setConversationId(conversationId);
        msg.setContent(content);
        send(msg);
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
