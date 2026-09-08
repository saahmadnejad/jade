package io.donbee.jade.examples.devteam;

import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Judges each round and issues the Verdict that drives the workflow.
 * Supports P2P: after review, sends verdict to Manager (for phase tracking)
 * and Implementer (for next round feedback).
 */
public class ReviewerAgent extends RoleAgent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(ReviewerAgent.class.getName());

    /** Machine-readable verdict lines the Manager looks for. */
    public static final String VERDICT_APPROVED = "VERDICT: approved";
    public static final String VERDICT_CHANGES = "VERDICT: changes-requested";

    @Override
    protected String role() {
        return "reviewer";
    }

    @Override
    protected String systemPrompt() {
        return """
            You are the code reviewer of a small development team.
            You receive the design, the implemented files and the tester's report.

            Judge correctness, completeness against the design, and obvious bugs.
            Be pragmatic: this team ships small tools, not enterprise systems.

            STRICT ending contract - your reply MUST end with one line:
            VERDICT: approved
            or
            VERDICT: changes-requested

            Before that line, if changes are requested, list at most three concrete,
            actionable corrections.""";
    }

    @Override
    protected void onTaskCompleted(String result) {
        notifyPeer("manager", "Verdict: " + result);
        notifyPeer("implementer", result);
    }
}