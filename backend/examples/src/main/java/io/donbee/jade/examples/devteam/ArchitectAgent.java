package io.donbee.jade.examples.devteam;

/** Produces the technical design for the Brief as a design-document Artifact. */
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
}
