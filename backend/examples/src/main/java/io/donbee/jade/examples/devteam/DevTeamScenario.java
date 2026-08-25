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

    private static final String DEFAULT_BRIEF =
        "Build a command-line To-Do list application in Python: add/list/done/remove "
            + "tasks, persisted to a JSON file, with unit tests.";

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
            + "collaborate to build a small project from a brief. The Manager routes work in "
            + "bounded rounds; the Reviewer decides when it is done. Watch the whole "
            + "conversation on the Messages page and the produced files via the workspace "
            + "mirror directory.";
    }

    @Override
    public List<ScenarioParam> params() {
        return List.of(
            ScenarioParam.stringParam("brief", DEFAULT_BRIEF, "What the team must build"),
            ScenarioParam.intParam("maxRounds", 3, 1, 10, "Max implement/review rounds"),
            ScenarioParam.intParam("maxTotalCalls", 12, 4, 500,
                "Hard cap on LLM calls per instance (free tiers allow ~50/day)"),
            ScenarioParam.intParam("callTimeoutSec", 120, 10, 900, "Timeout per LLM call"),
            ScenarioParam.stringParam("baseUrl", "https://openrouter.ai/api/v1",
                "OpenAI-compatible endpoint of the provider"),
            ScenarioParam.stringParam("keyEnvVar", "OPENROUTER_API_KEY",
                "Environment variable holding the API key (never put keys here)"),
            ScenarioParam.boolParam("proxyEnabled", true, "Route LLM traffic through SOCKS5 proxy"),
            ScenarioParam.stringParam("proxyHost", "192.168.1.151", "SOCKS5 proxy host"),
            ScenarioParam.intParam("proxyPort", 10808, 1, 65535, "SOCKS5 proxy port"),
            ScenarioParam.stringParam("managerModel", "thinkingmachines/inkling-small:free", "Manager model (reserved)"),
            ScenarioParam.stringParam("architectModel", "thinkingmachines/inkling:free", "Architect model"),
            ScenarioParam.stringParam("implementerModel", "poolside/laguna-s-2.1:free", "Implementer model"),
            ScenarioParam.stringParam("testerModel", "minimax/minimax-m3:free", "Tester model"),
            ScenarioParam.stringParam("reviewerModel", "z-ai/glm-5.2:free", "Reviewer model"),
            ScenarioParam.stringParam("workspaceDir", "",
                "Optional directory mirroring the team's produced files (empty = memory only)"));
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

        List<AgentSpec> specs = new ArrayList<>();
        specs.add(new AgentSpec("manager", MANAGER_CLASS, List.of(
            brief, String.valueOf(maxRounds), String.valueOf(maxTotalCalls),
            String.valueOf(Math.max(1, callTimeoutSec / 60)),
            str(config, "workspaceDir"))));

        specs.add(new AgentSpec("architect", ARCHITECT_CLASS,
            brainArgs(baseUrl, str(config, "architectModel"), proxyEnabled, proxyHost, proxyPort, callTimeoutSec, keyEnvVar)));
        specs.add(new AgentSpec("implementer", IMPLEMENTER_CLASS,
            brainArgs(baseUrl, str(config, "implementerModel"), proxyEnabled, proxyHost, proxyPort, callTimeoutSec, keyEnvVar)));
        specs.add(new AgentSpec("tester", TESTER_CLASS,
            brainArgs(baseUrl, str(config, "testerModel"), proxyEnabled, proxyHost, proxyPort, callTimeoutSec, keyEnvVar)));
        specs.add(new AgentSpec("reviewer", REVIEWER_CLASS,
            brainArgs(baseUrl, str(config, "reviewerModel"), proxyEnabled, proxyHost, proxyPort, callTimeoutSec, keyEnvVar)));
        return specs;
    }

    private static List<Object> brainArgs(String baseUrl, String model, boolean proxyEnabled,
                                          String proxyHost, int proxyPort, int timeoutSec, String keyEnvVar) {
        return List.of(
            baseUrl,
            model,
            String.valueOf(proxyEnabled),
            proxyHost,
            String.valueOf(proxyPort),
            String.valueOf(timeoutSec),
            keyEnvVar);
    }

    private static String str(Map<String, Object> config, String key) {
        return String.valueOf(config.get(key));
    }
}
