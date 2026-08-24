package io.donbee.jade.rest;

/**
 * Centralized route path constants for the REST API.
 * Ensures URL strings are defined in a single location.
 */
public final class ApiRoutes {

    private ApiRoutes() {
    }

    // Health
    public static final String HEALTH = "/api/health";

    // Version
    public static final String VERSION = "/api/version";

    // Platform
    public static final String PLATFORM = "/api/platform";
    public static final String PLATFORM_SHUTDOWN = "/api/platform/shutdown";

    // Containers
    public static final String CONTAINERS = "/api/containers";
    public static final String CONTAINER_BY_NAME = "/api/containers/:name";
    public static final String CONTAINER_BY_NAME_SAVE = "/api/containers/:name/save";
    public static final String CONTAINER_BY_NAME_LOAD = "/api/containers/:name/load";
    public static final String CONTAINER_MTPS = "/api/containers/:name/mtps";
    public static final String CONTAINER_MTP_BY_ADDRESS = "/api/containers/:name/mtps/:address";

    // Agents
    public static final String AGENTS = "/api/agents";
    public static final String AGENT_BY_NAME = "/api/agents/:name";
    public static final String AGENT_SUSPEND = "/api/agents/:name/suspend";
    public static final String AGENT_RESUME = "/api/agents/:name/resume";
    public static final String AGENT_FREEZE = "/api/agents/:name/freeze";
    public static final String AGENT_THAW = "/api/agents/:name/thaw";
    public static final String AGENT_CLONE = "/api/agents/clone";
    public static final String AGENT_MOVE = "/api/agents/:name/move";
    public static final String AGENT_SAVE = "/api/agents/:name/save";
    public static final String AGENT_LOAD = "/api/agents/load";
    public static final String AGENT_REGISTER_REMOTE = "/api/agents/register-remote";

    // Remote Platforms
    public static final String PLATFORMS = "/api/platforms";
    public static final String PLATFORM_FETCH = "/api/platforms/fetch";
    public static final String PLATFORM_BY_NAME = "/api/platforms/:name";
    public static final String PLATFORM_DESCRIPTION = "/api/platforms/:name/description";
    public static final String PLATFORM_REFRESH = "/api/platforms/:name/refresh";
    public static final String PLATFORM_AGENTS = "/api/platforms/:name/agents";

    // Tools
    public static final String TOOLS_START = "/api/tools/:tool/start";

    // DF (Directory Facilitator)
    public static final String DF_REGISTRATIONS = "/api/df/registrations";
    public static final String DF_REGISTRATION_BY_NAME = "/api/df/registrations/:agentName";
    public static final String DF_SEARCH = "/api/df/search";
    public static final String DF_DESCRIPTION = "/api/df/description";
    public static final String DF_REFRESH = "/api/df/refresh";
    public static final String DF_GUI_STATUS = "/api/tools/df-gui/status";

    // DF Federation
    public static final String DF_FEDERATION = "/api/df/federation";
    public static final String DF_FEDERATION_PARENTS = "/api/df/federation/parents";
    public static final String DF_FEDERATION_CHILDREN = "/api/df/federation/children";
    public static final String DF_FEDERATION_PARENT_BY_NAME = "/api/df/federation/:parentDFName";
    public static final String DF_FEDERATION_CHILDREN_BY_NAME = "/api/df/federation/children/:childDFName";
}
