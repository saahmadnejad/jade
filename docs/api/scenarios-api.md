# Scenarios API

Manage multi-agent demo scenarios (like the bundled dev team) on a running
platform. A **scenario** is a named template describing a set of agents and
their configurable parameters. An **instance** is one running copy of a
scenario.

Each instance runs in its own dedicated agent container
(`scenario-<instanceName>`) on the platform, so instances are isolated from
each other and can be stopped atomically by killing their container. Multiple
instances — of the same or different scenarios — can run side by side on one
platform core; agent local names are prefixed with the instance name to avoid
clashes.

Scenarios are discovered through Java's `ServiceLoader` mechanism: any jar on
the platform classpath can provide implementations of
`io.donbee.jade.rest.scenario.Scenario` (registered via
`META-INF/services/io.donbee.jade.rest.scenario.Scenario`) and they appear
here automatically. The bundled examples live in the `examples` module.

Errors follow the global format: `{"error": "message", "code": <status>}`.

---

## GET /api/scenarios

List all available scenario templates with their configurable parameters.

### Response — 200 OK

```json
{
  "scenarios": [
    {
      "id": "dev-team",
      "title": "Software Development Team",
      "description": "Five LLM-powered agents build a small project...",
      "params": [
        {
          "name": "maxRounds",
          "type": "int",
          "defaultValue": 5,
          "minValue": 1,
          "maxValue": 10,
          "description": "Review rounds cap"
        }
      ]
    }
  ]
}
```

Param `type` is one of `"int"`, `"string"`, `"boolean"`.

---

## POST /api/scenarios/{id}/instances

Start a new instance of a scenario.

### Request body

```json
{
  "instanceName": "team-demo-1",
  "config": {
    "brief": "Build a tiny python string-utils library with tests",
    "maxRounds": 3
  }
}
```

- `instanceName` optional; defaults to `<scenarioId>-<nextFreeNumber>`.
  Must be alphanumeric plus `-` / `_` (used as agent-name prefix and container
  name suffix).
- `config` optional; missing params fall back to their defaults. Unknown
  keys are rejected with 400.

### Response — 201 Created

```json
{
  "message": "Scenario 'dev-team' started as instance 'team-demo-1'",
  "instance": "team-demo-1",
  "container": "scenario-team-demo-1",
  "agents": ["team-demo-1-manager", "team-demo-1-architect", "..."]
}
```

### Errors

- `404` unknown scenario id
- `400` invalid config values (out of min/max range, wrong type) or bad
  instance name
- `409` instance name already in use or an agent/container name clash on the
  platform

### Dev-team notes

- The `brief` config param is required for the dev-team scenario: an empty
  brief fails the instance immediately.
- The `*Model` params are `provider/model` ids resolved by the `opencode`
  CLI; the default provider is tokenrouter (configured per instance
  workspace, see `docs/adr/0003`). Role agents fail fast when the CLI or the
  provider key (`TOKENROUTER_API_KEY`) is missing.

---

## GET /api/scenarios/instances

List currently tracked running instances. Tracking is in-memory only:
instances started before a platform restart are not listed after it.
Instances whose container disappears outside this API (e.g. killed from
`DELETE /api/containers/:name`) are pruned automatically on the next list.

### Response — 200 OK

```json
{
  "instances": [
    {
      "instance": "team-demo-1",
      "scenarioId": "dev-team",
      "container": "scenario-team-demo-1",
      "agents": [
        { "name": "team-demo-1-manager" },
        { "name": "team-demo-1-architect" }
      ]
    }
  ]
}
```

---

## DELETE /api/scenarios/instances/{instance}

Stop an instance: kills its dedicated container, terminating all of its
agents. Other instances are unaffected.

### Response — 200 OK

```json

{ "message": "Instance 'team-demo-1' stopped" }
```

### Errors

- `404` unknown instance

---

## Old GUI equivalent

No direct Swing GUI equivalent: launching multi-agent demos previously meant
hand-writing `Boot -agents` command lines or `-conf` property files. The
closest old context is the RMA's ad-hoc agent launch dialog
(`io.donbee.jade.tools.rma.StartNewAgentAction`). These endpoints delegate to
`PlatformService#deployAgent()` / `killContainer()`.
