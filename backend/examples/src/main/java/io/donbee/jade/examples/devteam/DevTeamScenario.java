package io.donbee.jade.examples.devteam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;

/**
 * Scenario template for a virtual software development team: five Role Agents
 * (Manager, Architect, Implementer, Tester, Reviewer) collaborate over a
 * shared {@link Workspace} to turn a Brief into a small working project. Each
 * Role Agent reasons through the platform-agnostic LLM module; every
 * conversation hop is visible on the MessagesPage.
 */
public class DevTeamScenario implements Scenario {

    static final String MANAGER_CLASS = ManagerAgent.class.getName();
    static final String ARCHITECT_CLASS = ArchitectAgent.class.getName();
    static final String IMPLEMENTER_CLASS = ImplementerAgent.class.getName();
    static final String TESTER_CLASS = TesterAgent.class.getName();
    static final String REVIEWER_CLASS = ReviewerAgent.class.getName();

    private static final String DEFAULT_BRIEF = ""; // the user must say what to build

    @Override
    public String id() {
        return "dev-team";
    }

    @Override
    public String title() {
        return "Software Development Team";
    }

    @Override
    public String description() {
        return "Five LLM-powered agents (Manager, Architect, Implementer, Tester, Reviewer) "
            + "collaborate to build whatever YOU ask for: describe your project in the 'brief' "
            + "field - it is required. The team works in bounded review rounds inside skilled "
            + "opencode sessions and publishes the result to GitHub.";
    }

    @Override
    public List<ScenarioParam> params() {
        return List.of(
            ScenarioParam.stringParam("brief", DEFAULT_BRIEF,
                "REQUIRED: what the team must build - describe it like a README"),
            ScenarioParam.intParam("maxRounds", 3, 1, 10, "Max implement/review rounds"),
            ScenarioParam.intParam("maxTotalCalls", 12, 4, 500,
                "Hard cap on LLM calls per instance (free tiers allow ~50/day)"),
            ScenarioParam.intParam("callTimeoutSec", 300, 10, 1800, "Timeout per LLM call"),
            ScenarioParam.stringParam("fallbackModel", "opencode/nemotron-3-ultra-free",
                "Fallback brain model when the role model fails or times out (9router model prefix)"),
            ScenarioParam.boolParam("clarify", true,
                "Run a clarification pass before design (set false to skip it and start faster)"),
            ScenarioParam.stringParam("brainType", "http",
                "Agent reasoning backend: 'cli' (opencode CLI) or 'http' (OpenAI-compatible endpoint)"),
            ScenarioParam.stringParam("cliCommand", "opencode run --auto",
                "CLI + subcommand + flags used when brainType=cli"),
            ScenarioParam.stringParam("workspaceDir", "",
                "Optional directory mirroring the team's produced files; also the CLI working directory"),
            ScenarioParam.stringParam("baseUrl", "http://localhost:20128/v1",
                "OpenAI-compatible endpoint of the provider (brainType=http only). Defaults to 9router local proxy."),
            ScenarioParam.stringParam("keyEnvVar", "NINEROUTER_API_KEY",
                "Environment variable holding the 9router API key from dashboard (brainType=http only)"),
            ScenarioParam.boolParam("proxyEnabled", false, "Route LLM traffic through SOCKS5 proxy (brainType=http only)"),
            ScenarioParam.stringParam("proxyHost", "192.168.1.151", "SOCKS5 proxy host"),
            ScenarioParam.intParam("proxyPort", 10808, 1, 65535, "SOCKS5 proxy port"),
            ScenarioParam.stringParam("managerModel", "kr/claude-sonnet-4.5", "Manager model (9router prefix)"),
            ScenarioParam.stringParam("architectModel", "kr/claude-sonnet-4.5", "Architect model"),
            ScenarioParam.stringParam("implementerModel", "kr/claude-sonnet-4.5", "Implementer model"),
            ScenarioParam.stringParam("testerModel", "opencode/nemotron-3-ultra-free", "Tester model"),
            ScenarioParam.stringParam("reviewerModel", "opencode/nemotron-3-ultra-free", "Reviewer model"),
            ScenarioParam.stringParam("githubOrg", "moreshco-agents",
                "GitHub org the team publishes to (empty = skip GitHub)"),
            ScenarioParam.stringParam("githubVisibility", "private",
                "Created repository visibility: private or public"));
    }

    @Override
    public List<AgentSpec> agents(Map<String, Object> config) {
        String brief = str(config, "brief");
        int maxRounds = (Integer) config.get("maxRounds");
        int maxTotalCalls = (Integer) config.get("maxTotalCalls");
        int callTimeoutSec = (Integer) config.get("callTimeoutSec");
        boolean proxyEnabled = (Boolean) config.get("proxyEnabled");
        String proxyHost = str(config, "proxyHost");
        int proxyPort = (Integer) config.get("proxyPort");
        String baseUrl = str(config, "baseUrl");
        String keyEnvVar = str(config, "keyEnvVar");
        String brainType = str(config, "brainType");
        String cliCommand = str(config, "cliCommand");
        String workspaceDir = str(config, "workspaceDir");

        List<AgentSpec> specs = new ArrayList<>();
        specs.add(new AgentSpec("manager", MANAGER_CLASS, List.of(
            brief, String.valueOf(maxRounds), String.valueOf(maxTotalCalls),
            String.valueOf(Math.max(1, callTimeoutSec / 60)),
            workspaceDir,
            str(config, "githubOrg"),
            str(config, "githubVisibility"),
            String.valueOf(config.get("clarify")))));

        specs.add(roleSpec("architect", ARCHITECT_CLASS, config));
        specs.add(roleSpec("implementer", IMPLEMENTER_CLASS, config));
        specs.add(roleSpec("tester", TESTER_CLASS, config));
        specs.add(roleSpec("reviewer", REVIEWER_CLASS, config));
        return specs;
    }

    private static AgentSpec roleSpec(String suffix, String className, Map<String, Object> config) {
        return new AgentSpec(suffix, className, List.of(
            str(config, "brainType"),
            str(config, "baseUrl"),
            str(config, modelKey(suffix)),
            str(config, "fallbackModel"),
            String.valueOf(config.get("proxyEnabled")),
            str(config, "proxyHost"),
            String.valueOf(config.get("proxyPort")),
            String.valueOf(config.get("callTimeoutSec")),
            str(config, "keyEnvVar"),
            str(config, "cliCommand"),
            str(config, "workspaceDir")));
    }

    private static String modelKey(String roleSuffix) {
        return roleSuffix + "Model";
    }

    private static String str(Map<String, Object> config, String key) {
        return String.valueOf(config.get(key));
    }
}
