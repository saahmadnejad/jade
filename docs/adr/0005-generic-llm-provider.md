# ADR-0005: Generic OpenAI-compatible LLM Provider Configuration for Dev-Team Scenario

## Status

Accepted (supersedes tokenrouter-specific sections of ADR-0003).

## Context

ADR-0003 introduced the `opencode` CLI runtime for dev-team role agents but hardcoded `tokenrouter` as the exclusive provider:
- `TeamScaffolder` baked `https://tokenrouter.com/v1` and `{env:TOKENROUTER_API_KEY}` into each generated instance workspace `opencode.json`.
- `DEFAULT_MODEL` was hardcoded across `DevTeamScenario.java` and `RoleAgent.java`.
- Free pool models churned or expired (e.g. `glm-5.3-free` and quota blocks), breaking builds when credentials or model endpoints change.
- Developers running local models (e.g. Ollama, local proxies, Antigravity Manager, or OpenRouter) could not use their existing host configuration because workspace `opencode.json` shadowed global configurations.

## Decision

1. **Generic Provider Resolution**: Dev-team LLM configuration is generalized to any OpenAI-compatible endpoint resolved via `LLM_PROVIDER_ID` (default `custom`), `LLM_BASE_URL` (optional), and `LLM_API_KEY`.
2. **Deprecated Alias**: `TOKENROUTER_API_KEY` is preserved as a deprecated alias for `LLM_API_KEY`. If `LLM_API_KEY` is unset and `TOKENROUTER_API_KEY` is present, it is used with a deprecation warning, referencing `{env:TOKENROUTER_API_KEY}`.
3. **Empty Model Defaults**: All `*Model` parameters default to `""` (empty string). `CliBrain` omits `-m` when the model argument is blank, enabling OpenCode to automatically resolve models from host or global configuration.
4. **Empty Provider Skeleton when Unconfigured**: When all `LLM_*` variables (and `TOKENROUTER_API_KEY`) are unset, `TeamScaffolder` writes an empty provider skeleton (`"providers": {}, "provider": {}`) in the workspace `opencode.json`. This allows host-level OpenCode provider authentication and global configuration to flow through seamlessly.
5. **No Secrets on Disk**: In accordance with ADR-0002, keys are only referenced via `{env:...}` interpolation and never written to disk in plain text.
6. **Real IT Gating**: `DevTeamRealIT` is gated on `LLM_API_KEY` (or `TOKENROUTER_API_KEY`) and `-Dit.real=true`, omitting `-m` from the LLM judge invocation so the default configured model evaluates the artifact.

## Consequences

- The platform is completely provider-agnostic: any OpenAI-compatible provider, cloud gateway (OpenRouter, Groq, Together), or local inference server (Ollama, vLLM, Antigravity Manager) can drive dev-team agents.
- Local runs with existing host OpenCode credentials work with zero additional configuration.
- Backward compatibility is maintained for existing setups relying on `TOKENROUTER_API_KEY`.
