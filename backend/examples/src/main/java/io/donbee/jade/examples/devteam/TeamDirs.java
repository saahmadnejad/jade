package io.donbee.jade.examples.devteam;

import java.nio.file.Path;

/**
 * Resolves the instance working directory: the configured workspace dir when
 * given, otherwise a deterministic temp directory shared by all role agents
 * of the same team.
 */
final class TeamDirs {

    private TeamDirs() {
    }

    static Path resolve(String teamId, String workspaceDirParam) {
        if (workspaceDirParam != null && !workspaceDirParam.isBlank()) {
            return Path.of(workspaceDirParam.trim());
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "jade-devteam-" + teamId);
    }
}
