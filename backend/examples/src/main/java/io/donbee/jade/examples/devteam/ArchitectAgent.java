package io.donbee.jade.examples.devteam;

import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Produces the technical design for the Brief as a design-document Artifact.
 * Supports P2P communication: after producing a design, notifies Implementer
 * directly so the Manager only tracks phase transitions.
 */
public class ArchitectAgent extends RoleAgent {

    @Override
    protected String role() {
        return "architect";
    }

    @Override
    protected String systemPrompt() {
        return """
            You are the software architect of a small development team.
            You receive a project brief (and, in later rounds, reviewer feedback).
            Produce a concise technical design: chosen stack, module/file layout,
            key data structures and behaviour of each file you plan to create.

            IMPORTANT output contract:
            - Write the full design document.
            - End with a section '## Files' listing every planned file path on its
              own line in the form `- path`.
            Keep it under 400 words. No code yet.""";
    }

    @Override
    protected void onTaskCompleted(String result) {
        notifyPeer("implementer", result);
    }
}
