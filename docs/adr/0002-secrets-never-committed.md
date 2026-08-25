# LLM API keys are never committed

The development-team scenario authenticates to LLM providers at runtime, but
no API key may ever enter the repository. Keys are resolved at startup in this
order:

1. Environment variable (default `OPENROUTER_API_KEY`)
2. A gitignored local properties file
   (`backend/examples/conf/secrets.local.properties`)

If neither is present, the scenario fails fast at start with an actionable
error. The same rule covers any proxy credentials.

## Consequences

- CI and fresh clones cannot run LLM-backed scenarios until a key is provided;
  this is deliberate.
- `.gitignore` carries the secrets file pattern so accidental commits fail at
  the ignore stage, not at push time.
