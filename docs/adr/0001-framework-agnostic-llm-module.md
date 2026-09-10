# LLM brains live in a framework-agnostic module

Role agents whose reasoning is delegated to an LLM get that capability from a
dedicated Maven module `io.donbee:llm` (`backend/llm`) with **zero JADE
dependencies**: a plain Java 21 library exposing a `Brain` interface (prompt
in → text out, per-call model selection, blocking with timeout).
`CliBrain` shells the opencode CLI agent; `RetryingBrain` retries with growing
gaps; `FallbackBrain` chains brains on failure; `LangChain4jBrain` +
`BashTool` (langchain4j OpenAI-compatible HTTP client) remain as a library
option for non-CLI callers. The dev-team scenario uses the CLI stack — see
ADR-0003, which supersedes the original HTTP choice for that scenario.

## Considered Options

- Embedding the client inside the examples scenario module — rejected: the
  owner plans to reuse LLM "brains" in future projects beyond Jade, and a
  framework-free jar is directly consumable anywhere.
- Provider-specific SDKs — rejected: most gateways speak the OpenAI wire
  format; langchain4j's OpenAI client covers them all via configurable
  baseUrl/model/key.

## Consequences

- `backend/llm` must never import `io.donbee.jade.*`; doing so would couple
  every future brain consumer to the agent platform.
- Secrets reach brains via environment (CLI auth, see ADR-0002/0003);
  resolution from environment/local files is the caller's concern.
