package io.donbee.jade.rest.scenario;

/**
 * A configurable multi-agent scenario template.
 *
 * <p>Implementations live in scenario-providing modules (e.g. the bundled
 * {@code examples} module) and are discovered via
 * {@link java.util.ServiceLoader}: each jar registers its scenarios in
 * {@code META-INF/services/io.donbee.jade.rest.scenario.Scenario}. This keeps
 * the platform free of any hard-wired knowledge about concrete scenarios.</p>
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent; demos were
 * previously started by hand-written {@code Boot -agents} command lines or
 * {@code -conf} property files.</p>
 */
public interface Scenario {

    /** Unique scenario id, e.g. {@code online-shop}. Lowercase, no spaces. */
    String id();

    /** Human-readable title shown in the UI. */
    String title();

    /** Short description of what the scenario demonstrates. */
    String description();

    /**
     * Configurable parameters with their defaults and (optional) bounds.
     * Rendered as a form by the frontend; values are passed back to
     * {@link #agents(java.util.Map)}.
     */
    java.util.List<ScenarioParam> params();

    /**
     * Compute the agent set for one instance of this scenario.
     *
     * @param config parameter values keyed by {@link ScenarioParam#name()};
     *               guaranteed to contain every declared param (defaults filled in)
     * @return agent specs; {@code nameSuffix} must be unique within the list
     *         and is prefixed with the instance name on deployment
     */
    java.util.List<AgentSpec> agents(java.util.Map<String, Object> config);
}
