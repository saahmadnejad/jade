# Dev-team agents reason through the opencode CLI (tokenrouter)

## Status

Accepted (supersedes the dev-team decision in ADR-0001 — the HTTP/langchain4j
stack remains in the `llm` module as a library option).

## Context

The dev-team scenario originally called a 9router container (OpenAI-compatible
HTTP gateway) via `LangChain4jBrain`. Problems:

- 9router is not CLI-friendly: provider/model configuration lives in its own
  dashboard and sqlite DB, free-pool models churn (dead defaults surface as
  401/429 at the first role call), and the extra container adds a healthcheck
  + startup ordering dependency to every compose run.
- Role agents were limited to a single tool (`BashTool`) inside a
  langchain4j tool-calling loop. A CLI agent runtime (opencode) gives each
  role a full agent harness: personas, skills, bash/edit/grep tools,
  permissions — and `TeamScaffolder` already wrote `.opencode/agent/<role>.md`
  personas that nothing consumed.

## Decision

1. **`CliBrain` is the dev-team brain.** Each role agent runs
   `opencode run --auto --agent <role> -m <model> <prompt>` in the instance
   workspace. The CLI (and its personas/skills) does the reasoning; the JADE
   agent handles the FIPA conversation and artifact bookkeeping.
2. **`RetryingBrain` decorator**: any `BrainException` (timeout, exit≠0,
   empty output) is retried — 4 attempts total (1 initial + 3 retries) with
   growing gaps (5s/15s/45s).
3. **`FallbackBrain` chains primary → fallback model** when retries are
   exhausted. Both layers compose: `FallbackBrain([RetryingBrain(primary),
   RetryingBrain(fallback)])`. By default `fallbackModel` is empty (single
   brain); configure a distinct model id to activate the chain.
4. **tokenrouter provider** reaches opencode through an `opencode.json`
   written into each instance workspace by `TeamScaffolder`
   (`@ai-sdk/openai-compatible`, `baseURL` default `https://tokenrouter.com/v1`).
   The API key is injected from the environment (`{env:TOKENROUTER_API_KEY}`),
   never stored on disk. Default model: `tokenrouter/z-ai/glm-5.3-free`;
   per-role `*Model` scenario params are `provider/model` ids.
5. **No pre-deploy provider probe.** The old HTTP `/models` check (409 on
   failure) is deleted; resilience is retry → fallback → STATUS.md FAILED with
   the actionable reason.
6. **Fenced-block artifact contract stays.** The CLI returns text; the Manager
   remains the sole writer, parsing ```lang path``` blocks from replies
   (`ArtifactParser`). No direct workspace writes by the CLI.
7. **Scenario param changes**: `baseUrl`, `proxyEnabled`, `proxyHost`,
   `proxyPort` are removed; `callTimeoutSec` (default 600s) maps to the CLI
   process timeout. `SecretsResolver` resolves `GH_TOKEN`/`github.token` only.

## Testing

- `DevTeamFlowIntegrationTest` (default suite, offline): boots a real
  platform, swaps the CLI for a stub script (via the `opencode.cli` system
  property / `OPENCODE_CLI` env) that rejects round 1 and approves round 2 —
  proving the full implement→test→review loop and artifact layout.
- `DevTeamRealIT` (gated by `-Dit.real=true` + `TOKENROUTER_API_KEY`): real
  tokenrouter run; gates are terminal STATUS APPROVED/PUBLISHED,
  `py_compile` of the produced library, `unittest discover` exit 0, and an
  LLM-as-judge score ≥ 7/10. Scheduled nightly/manual in CI.

## Consequences

- The `llm` module keeps `LangChain4jBrain`/`BashTool` (ADR-0001 stack) as an
  unused library option; dev-team no longer imports them.
- CLI calls are slower than HTTP (agentic multi-step runs); the 600s default
  timeout and per-phase deadlines account for this.
- No 9router container in compose; the backend image already bundles the
  opencode binary. Credentials are one env var, mirroring ADR-0002's
  no-committed-secrets rule.
- `opencode.cli` system property / `OPENCODE_CLI` env override the binary
  path — the test seam for stubbing the CLI.
