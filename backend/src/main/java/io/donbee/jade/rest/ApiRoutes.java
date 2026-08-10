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
}
