package io.donbee.jade.rest.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import io.donbee.jade.core.Profile;
import io.donbee.jade.core.ProfileImpl;
import io.donbee.jade.core.Runtime;
import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;

/**
 * Manages lifecycle of scenario instances: discovers available scenarios via
 * {@link ServiceLoader}, starts instances in their own dedicated agent
 * container (one container per instance, so instances are isolated and can be
 * stopped atomically) and tracks them in memory.
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent; multi-agent
 * demos were previously launched through hand-written {@code Boot -agents}
 * command lines. Delegates to {@link PlatformService#deployAgent(String,
 * String, Object[], String)} and {@link PlatformService#killContainer(String)}.</p>
 */
public class ScenarioService {

    /** Instance names become agent-name prefixes and container-name suffixes. */
    private static final Pattern INSTANCE_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String CONTAINER_PREFIX = "scenario-";

    /** One running scenario instance. */
    public record InstanceInfo(String instance, String scenarioId, String container, List<String> agents) {
    }

    public static class StartResult {
        public final String instance;
        public final String scenarioId;
        public final String container;
        public final List<String> agents;

        public StartResult(String instance, String scenarioId, String container, List<String> agents) {
            this.instance = instance;
            this.scenarioId = scenarioId;
            this.container = container;
            this.agents = agents;
        }
    }

    private final PlatformService platformService;
    private final Map<String, Scenario> scenarios;
    private final Map<String, InstanceInfo> instances = new ConcurrentHashMap<>();

    public ScenarioService(PlatformService platformService) {
        this(platformService, discoverScenarios());
    }

    /** Visible for tests: inject a fixed set of scenarios. */
    ScenarioService(PlatformService platformService, Map<String, Scenario> scenarios) {
        this.platformService = platformService;
        this.scenarios = Collections.unmodifiableMap(new TreeMap<>(scenarios));
    }

    private static Map<String, Scenario> discoverScenarios() {
        Map<String, Scenario> found = new TreeMap<>();
        for (Scenario s : ServiceLoader.load(Scenario.class)) {
            found.put(s.id(), s);
        }
        return found;
    }

    public List<Scenario> list() {
        return List.copyOf(scenarios.values());
    }

    public Scenario get(String scenarioId) {
        return scenarios.get(scenarioId);
    }

    public List<InstanceInfo> listInstances() {
        pruneDeadInstances();
        return List.copyOf(instances.values());
    }

    /**
     * Drop tracked instances that disappeared outside the scenarios API —
     * e.g. the user killed the instance container from the Containers page.
     * A dedicated-container instance is dead when its container is gone; a
     * Main-Container fallback instance when none of its agents exist anymore.
     */
    private void pruneDeadInstances() {
        if (instances.isEmpty()) {
            return;
        }
        try {
            java.util.Set<String> liveContainers = new java.util.HashSet<>();
            for (PlatformService.ContainerInfo c : platformService.getContainers()) {
                liveContainers.add(c.name);
            }
            if (liveContainers.isEmpty()) {
                // Platform info unavailable -> cannot verify, keep current view.
                return;
            }
            java.util.Set<String> liveAgents = new java.util.HashSet<>();
            for (PlatformService.AgentInfo a : platformService.getAgents(true)) {
                liveAgents.add(a.name);
            }
            instances.entrySet().removeIf(entry -> {
                InstanceInfo info = entry.getValue();
                boolean dedicated = info.container() != null && info.container().startsWith(CONTAINER_PREFIX);
                boolean dead = dedicated
                    ? !liveContainers.contains(info.container())
                    : info.agents().stream().noneMatch(liveAgents::contains);
                if (dead) {
                    System.err.println("[ScenarioService] Pruning instance '" + entry.getKey()
                        + "': its " + (dedicated ? "container" : "agents") + " are gone");
                }
                return dead;
            });
        } catch (RuntimeException e) {
            // Platform info unavailable right now -> keep current view.
            System.err.println("[ScenarioService] Skipping instance pruning: " + e.getMessage());
        }
    }

    /**
     * Start a new instance of the given scenario.
     *
     * @param instanceName optional; auto-generated when null/blank
     * @param rawConfig    user-provided config values; missing params fall back to defaults
     */
    public synchronized StartResult start(String scenarioId, String instanceName, Map<String, Object> rawConfig) {
        Scenario scenario = scenarios.get(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Unknown scenario: " + scenarioId);
        }
        pruneDeadInstances();
        String name = resolveInstanceName(scenarioId, instanceName);
        if (instances.containsKey(name)) {
            throw new IllegalStateException("Instance already exists: " + name);
        }

        Map<String, Object> config = validatedConfig(scenario, rawConfig);

        // Verify brain provider (9router) is reachable before deploying agents.
        checkBrainAvailability(config);

        // Dedicated container per instance; fall back to the Main Container on failure.
        String container = null;
        try {
            container = createScenarioContainer(name);
        } catch (Exception e) {
            System.err.println("[ScenarioService] Could not create dedicated container for '"
                + name + "', deploying into Main Container: " + e.getMessage());
        }

        List<String> deployedAgents = new ArrayList<>();
        try {
            for (AgentSpec spec : scenario.agents(config)) {
                String agentName = name + "-" + spec.getNameSuffix();
                platformService.deployAgent(
                    agentName,
                    spec.getClassName(),
                    spec.getArgs().toArray(),
                    container);
                deployedAgents.add(agentName);
            }
        } catch (RuntimeException e) {
            rollback(deployedAgents);
            if (container != null) {
                try {
                    platformService.killContainer(container);
                } catch (RuntimeException ignored) {
                    // Best-effort cleanup
                }
            }
            throw e;
        }

        String actualContainer = container != null ? container : findMainContainerName();
        InstanceInfo info = new InstanceInfo(name, scenarioId, actualContainer, List.copyOf(deployedAgents));
        instances.put(name, info);
        return new StartResult(name, scenarioId, actualContainer, info.agents());
    }

    /** Stop an instance by killing its whole container. */
    public synchronized void stop(String instanceName) {
        InstanceInfo info = instances.remove(instanceName);
        if (info == null) {
            throw new IllegalArgumentException("Unknown instance: " + instanceName);
        }
        boolean dedicatedContainer = info.container() != null && info.container().startsWith(CONTAINER_PREFIX);
        if (dedicatedContainer) {
            try {
                platformService.killContainer(info.container());
                return;
            } catch (RuntimeException e) {
                System.err.println("[ScenarioService] Killing container " + info.container()
                    + " failed, killing agents individually: " + e.getMessage());
            }
        }
        for (String agent : info.agents()) {
            try {
                platformService.killAgent(agent);
            } catch (RuntimeException ignored) {
                // Agent may already be dead; continue with the rest.
            }
        }
    }

    private String resolveInstanceName(String scenarioId, String requested) {
        if (requested == null || requested.isBlank()) {
            int n = 1;
            while (instances.containsKey(scenarioId + "-" + n)) {
                n++;
            }
            return scenarioId + "-" + n;
        }
        String trimmed = requested.trim();
        if (!INSTANCE_NAME.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                "Invalid instance name '" + trimmed + "': allowed are letters, digits, '-' and '_'");
        }
        return trimmed;
    }

    /** Merge user config over defaults; reject unknown keys, bad types and out-of-range ints. */
    private Map<String, Object> validatedConfig(Scenario scenario, Map<String, Object> rawConfig) {
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> provided = rawConfig != null ? rawConfig : Map.of();
        for (String key : provided.keySet()) {
            boolean known = scenario.params().stream().anyMatch(p -> p.getName().equals(key));
            if (!known) {
                throw new IllegalArgumentException("Unknown config parameter: " + key);
            }
        }
        for (ScenarioParam param : scenario.params()) {
            Object value = provided.get(param.getName());
            if (value == null) {
                value = param.getDefaultValue();
            } else if (param.getType() == ScenarioParam.Type.INT) {
                value = toInt(value, param.getName(), param.getMinValue(), param.getMaxValue());
            } else if (param.getType() == ScenarioParam.Type.BOOLEAN) {
                value = toBoolean(value, param.getName());
            } else {
                value = String.valueOf(value);
            }
            config.put(param.getName(), value);
        }
        return Collections.unmodifiableMap(config);
    }

    private Integer toInt(Object raw, String name, Long min, Long max) {
        Integer value;
        if (raw instanceof Number number) {
            value = number.intValue();
        } else {
            try {
                value = Integer.parseInt(String.valueOf(raw).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Config parameter '" + name + "' must be an integer");
            }
        }
        if (min != null && value < min) {
            throw new IllegalArgumentException("Config parameter '" + name + "' must be >= " + min);
        }
        if (max != null && value > max) {
            throw new IllegalArgumentException("Config parameter '" + name + "' must be <= " + max);
        }
        return value;
    }

    private Boolean toBoolean(Object raw, String name) {
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(raw).trim();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(s);
        }
        throw new IllegalArgumentException("Config parameter '" + name + "' must be true or false");
    }

    /**
     * Probe the LLM provider endpoint before deploying agents, so failures
     * surface immediately instead of after all agents are started.
     *
     * <p><b>Old GUI:</b> No direct Swing equivalent. The old RMA tool dispatched
     * agents without pre-flight provider checks. This validates the LLM base URL
     * and API key before any agent is deployed.</p>
     */
    private void checkBrainAvailability(Map<String, Object> config) {
        String baseUrl = config.containsKey("baseUrl")
            ? String.valueOf(config.get("baseUrl")) : null;
        if (baseUrl == null || baseUrl.isBlank() || "null".equals(baseUrl)) {
            return; // scenario does not use an LLM brain (e.g. pure shop demo)
        }
        String url = baseUrl + "/models";
        System.out.println("[ScenarioService] Probing brain provider: " + url);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // Some OpenAI-compatible servers mishandle the h2c upgrade and
                // hang; force HTTP/1.1 which every provider supports.
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .GET();
            String apiKey = System.getenv("NINEROUTER_API_KEY");
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> resp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("Brain provider returned HTTP "
                    + resp.statusCode() + " for " + url
                    + " — verify 9router is running and configured via browser at http://localhost:20129/dashboard");
            }
            System.out.println("[ScenarioService] Brain provider OK (HTTP "
                + resp.statusCode() + ")");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reach brain provider at " + url
                + " — is the 9router container running? Error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Brain provider probe interrupted", e);
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    /**
     * Create an in-process container joining the running platform.
     * Returns the container name, or throws on failure.
     * Protected so tests can substitute container creation.
     */
    protected String createScenarioContainer(String instanceName) {
        String containerName = CONTAINER_PREFIX + instanceName;
        ProfileImpl profile = new ProfileImpl(false);
        profile.setParameter(Profile.MAIN, "false");
        profile.setParameter(Profile.MAIN_HOST,
            System.getProperty("jade.main.host", "localhost"));
        profile.setParameter(Profile.MAIN_PORT,
            System.getProperty("jade.main.port", "1099"));
        profile.setParameter(Profile.CONTAINER_NAME, containerName);

        io.donbee.jade.wrapper.AgentContainer wrapper = Runtime.instance().createAgentContainer(profile);
        if (wrapper == null) {
            throw new RuntimeException("Container failed to join the platform");
        }
        return extractContainerIdName(wrapper);
    }

    /**
     * {@code wrapper.AgentContainer#getName()} returns the platform name, not
     * the container name, so pull the ContainerID out of the internal impl
     * (same access pattern as {@code RestAPIVerticle#extractImpl()}).
     */
    private String extractContainerIdName(io.donbee.jade.wrapper.AgentContainer wrapper) {
        try {
            java.lang.reflect.Field f = io.donbee.jade.wrapper.ContainerController.class.getDeclaredField("myImpl");
            f.setAccessible(true);
            io.donbee.jade.core.AgentContainer impl =
                (io.donbee.jade.core.AgentContainer) f.get(wrapper);
            return impl.getID().getName();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve created container name: " + e.getMessage(), e);
        }
    }

    private String findMainContainerName() {
        return platformService.getPlatformInfo().containerName;
    }

    private void rollback(List<String> deployedAgents) {
        for (String agent : deployedAgents) {
            try {
                platformService.killAgent(agent);
            } catch (RuntimeException ignored) {
                // Best-effort cleanup
            }
        }
    }
}
