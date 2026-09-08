package io.donbee.jade.examples.devteam;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link Workspace}s keyed by team-instance id. Static because
 * role agents of the same instance are separate agents sharing only the JVM.
 */
public final class WorkspaceStore {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(WorkspaceStore.class.getName());

    private static final Map<String, Workspace> WORKSPACES = new ConcurrentHashMap<>();

    private WorkspaceStore() {
    }

    public static Workspace getOrCreate(String id, java.nio.file.Path mirrorDir) {
        return WORKSPACES.computeIfAbsent(id,
            wid -> new Workspace(wid, mirrorDir));
    }

    public static Workspace get(String id) {
        return WORKSPACES.get(id);
    }
}
