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
            + "field - it is required. The team works in bounded review rounds "
            + "and publishes the result to GitHub.";
    }

    @Override
    public List<ScenarioParam> params() {
        return List.of(
            ScenarioParam.stringParam("brief", DEFAULT_BRIEF,
                "REQUIRED: what the team must build - describe it like a README"),
            ScenarioParam.intParam("maxRounds", 5, 1, 100, "Max implement/review rounds"),
            ScenarioParam.intParam("maxTotalCalls", 40, 4, 500,
                "Hard cap on LLM calls per instance (free tiers allow ~50/day)"),
            ScenarioParam.intParam("callTimeoutSec", 600, 10, 1800, "Timeout per CLI call"),
            ScenarioParam.boolParam("clarify", true,
                "Run a clarification pass before design (set false to skip it and start faster)"),
            ScenarioParam.stringParam("workspaceDir", "",
                "Optional directory mirroring the team's produced files; also the CLI working directory"),
            ScenarioParam.stringParam("fallbackModel", "",
                "Fallback opencode model id (empty = no fallback brain; must differ from the role models to activate the chain)"),
            ScenarioParam.stringParam("managerModel", DEFAULT_MODEL, "Manager model (unused: manager is deterministic)"),
            ScenarioParam.stringParam("architectModel", DEFAULT_MODEL, "Architect model (opencode provider/model id)"),
            ScenarioParam.stringParam("implementerModel", DEFAULT_MODEL, "Implementer model (opencode provider/model id)"),
            ScenarioParam.stringParam("testerModel", DEFAULT_MODEL, "Tester model (opencode provider/model id)"),
            ScenarioParam.stringParam("reviewerModel", DEFAULT_MODEL, "Reviewer model (opencode provider/model id)"),
            ScenarioParam.stringParam("githubOrg", "",
                "GitHub org the team publishes to (empty = skip GitHub publishing)"),
            ScenarioParam.stringParam("githubVisibility", "private",
                "Created repository visibility: private or public"));
    }

    /** Default opencode model id (tokenrouter provider, see ADR-0003). */
    static final String DEFAULT_MODEL = "tokenrouter/z-ai/glm-5.3-free";

    @Override
    public List<AgentSpec> agents(Map<String, Object> config) {
        String brief = str(config, "brief");
        int maxRounds = (Integer) config.get("maxRounds");
        int maxTotalCalls = (Integer) config.get("maxTotalCalls");
        int callTimeoutSec = (Integer) config.get("callTimeoutSec");
        String workspaceDir = str(config, "workspaceDir");

        List<AgentSpec> specs = new ArrayList<>();
        specs.add(new AgentSpec("manager", MANAGER_CLASS, List.of(
            brief, String.valueOf(maxRounds), String.valueOf(maxTotalCalls),
            String.valueOf(Math.max(10, callTimeoutSec / 60)),
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
            str(config, modelKey(suffix)),
            str(config, "fallbackModel"),
            String.valueOf(config.get("callTimeoutSec")),
            str(config, "workspaceDir")));
    }

    private static String modelKey(String roleSuffix) {
        return roleSuffix + "Model";
    }

    private static String str(Map<String, Object> config, String key) {
        return String.valueOf(config.get(key));
    }
}
