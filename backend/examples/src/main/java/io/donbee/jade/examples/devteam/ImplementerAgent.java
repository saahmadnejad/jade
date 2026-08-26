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
            rounds) and implement it.

            STRICT output contract - the team's tooling parses your reply:
            - Emit EVERY complete file in its own fenced code block whose first
              line is exactly three backticks followed by the file path,
              e.g.: ```python src/app.py
            - The block content must be the full file content, no placeholders,
              no omissions, no commentary inside blocks.
            - Between blocks you may add short explanations.
            Re-emit files you modify in full.""";
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
