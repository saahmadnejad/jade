package io.donbee.jade.examples.devteam;

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

    private enum Phase { DESIGN, IMPLEMENT, TEST, REVIEW, DONE }

    private String teamId;
    private Workspace workspace;
    private Phase phase = Phase.DESIGN;
    private int round = 1;
    private int callsUsed = 0;
    private long deadlineAt;
    private String brief;
    private String designDoc = "";
    private String reviewFeedback = "";
    private String lastVerdictRaw = "";
    private int maxRounds;
    private int maxTotalCalls;
    private long roundStartTimeoutMin;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        brief = str(args, 0, "Build a small CLI to-do app in Python.");
        maxRounds = intArg(args, 1, 3);
        maxTotalCalls = intArg(args, 2, 12);
        roundStartTimeoutMin = intArg(args, 3, 30);

        // Agent local name is "<instance>-manager"; instance names never end
        // in "-manager", so stripping the suffix recovers the team id.
        String local = getLocalName();
        teamId = local.endsWith("-manager") ? local.substring(0, local.length() - "-manager".length()) : local;
        String mirrorDir = str(args, 4, null);
        workspace = WorkspaceStore.getOrCreate(teamId,
            mirrorDir != null && !mirrorDir.isBlank() ? java.nio.file.Path.of(mirrorDir.trim()) : null);
        deadlineAt = System.currentTimeMillis() + roundStartTimeoutMin * 60_000L;

        System.out.println("[devteam:" + teamId + "] goal: " + firstLine(brief)
            + " (maxRounds=" + maxRounds + ", maxCalls=" + maxTotalCalls + ")"
            + (mirrorDir != null && !mirrorDir.isBlank() ? " mirror=" + mirrorDir : ""));
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

        // Kick off phase 1
        sendTo("architect", designTask(), "dt-design-" + round);
        phase = Phase.DESIGN;
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
                lastVerdictRaw = reply.getContent();
                workspace.save("REVIEW-round" + round + ".md", lastVerdictRaw);
                boolean approved = lastVerdictRaw.toUpperCase().contains(ReviewerAgent.VERDICT_APPROVED.toUpperCase());
                if (!approved) {
                    reviewFeedback = extractChanges(lastVerdictRaw);
                }
                evaluateNextRound(approved);
            }
            default -> { /* DONE */ }
        }
    }

    private void evaluateNextRound(boolean approved) {
        if (approved) {
            finish("APPROVED", "The reviewer approved the result in round " + round + ".");
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

    private String designTask() {
        return "Project brief from the product side:\n\n" + brief
            + "\n\nProduce the technical design following your output contract.";
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
