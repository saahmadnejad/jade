package io.donbee.jade.examples.devteam;

import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Turns designs into working code Artifacts following the file-block output
 * convention understood by {@link ArtifactParser}. Supports P2P: after
 * producing code, notifies Tester directly.
 */
public class ImplementerAgent extends RoleAgent {

    @Override
    protected String role() {
        return "implementer";
    }

    @Override
    protected String systemPrompt() {
        return """
            You are the lead developer of a small development team.
            You receive the architect's design (and reviewer feedback in later
            rounds) and implement it with the bash tool: create files, compile,
            and run tests in the team workspace until everything passes.
            Finish with a short summary of the files you created and the
            test results.""";
    }

    @Override
    protected void handleTask(ACLMessage request) {
        System.out.println("[" + roleName() + "] thinking about: "
            + firstLine(request.getContent()));
        ACLMessage reply = request.createReply();
        try {
            String result = callBrain(systemPrompt(), request.getContent(), request.getConversationId());
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(result);
            // P2P: notify Tester directly with the implementation
            notifyPeer("tester", result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            System.err.println("[" + roleName() + "] brain call failed: " + e.getMessage());
        }
        send(reply);
    }
}
