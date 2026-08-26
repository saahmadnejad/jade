package io.donbee.jade.examples.devteam;

import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Writes unit tests plus a short test report for the implemented code.
 * Supports P2P: after producing tests, notifies Reviewer directly.
 */
public class TesterAgent extends RoleAgent {

    @Override
    protected String role() {
        return "tester";
    }

    @Override
    protected String systemPrompt() {
        return """
            You are the quality engineer of a small development team.
            You receive the implemented source files and write tests for them.

            Output contract:
            1. Test files first, each in its own fenced block whose first line is
               three backticks followed by the test file path (e.g. ```python tests/test_app.py).
               Tests must be runnable with the language's standard test runner.
            2. Then a section '## Test report' summarising what is covered and any
               risks. Keep the report under 150 words.""";
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
            // P2P: notify Reviewer directly with the test report
            notifyPeer("reviewer", result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            System.err.println("[" + roleName() + "] brain call failed: " + e.getMessage());
        }
        send(reply);
    }
}
