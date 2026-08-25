package io.donbee.jade.examples.devteam;

/**
 * Turns designs into working code Artifacts following the file-block output
 * convention understood by {@link ArtifactParser}.
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
}
