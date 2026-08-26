package io.donbee.jade.examples.devteam;

import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Judges each round and issues the Verdict that drives the workflow.
 * Supports P2P: after review, sends verdict to Manager (for phase tracking)
 * and Implementer (for next round feedback).
 */
public class ReviewerAgent extends RoleAgent {

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
    protected void handleTask(ACLMessage request) {
        System.out.println("[" + roleName() + "] thinking about: "
            + firstLine(request.getContent()));
        ACLMessage reply = request.createReply();
        try {
            String result = brain.respond(systemPrompt(), request.getContent());
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(result);
            // P2P: notify Manager and Implementer with the verdict
            notifyPeer("manager", "Verdict: " + result);
            notifyPeer("implementer", result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            System.err.println("[" + roleName() + "] brain call failed: " + e.getMessage());
        }
        send(reply);
    }
}