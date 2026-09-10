# LLM API keys are never committed

The development-team scenario authenticates to LLM providers at runtime, but
no API key may ever enter the repository. The tokenrouter provider key is
injected from the environment (`TOKENROUTER_API_KEY`) directly into the
workspace `opencode.json` (see ADR-0003). The GitHub publish token is resolved
by `SecretsResolver`:

1. Environment variable (`GH_TOKEN`)
2. A gitignored local properties file
   (`backend/examples/conf/secrets-local.properties`; template with
   placeholder values only: `secrets-local.properties.example`, which holds
   only the optional `github.token` line)

If a publish run has neither, publishing is skipped (or fails fast with an
actionable error when a `githubOrg` was requested). The same rule covers any
proxy credentials.

## Consequences

- CI and fresh clones cannot run LLM-backed scenarios until a key is provided;
  this is deliberate.
- `.gitignore` carries the secrets file pattern so accidental commits fail at
  the ignore stage, not at push time.
