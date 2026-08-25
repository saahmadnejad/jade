# LLM brains live in a framework-agnostic module

Role agents whose reasoning is delegated to an LLM get that capability from a
dedicated Maven module `io.donbee:llm` (`backend/llm`) with **zero JADE
dependencies**: a plain Java 21 library exposing a `Brain` interface (prompt
in → text out, per-call model selection, optional SOCKS5 proxy, blocking with
timeout). v1 ships `HttpBrain`, speaking the OpenAI-compatible chat-completions
API so any provider works by changing config: OpenRouter (free or paid),
OpenAI, DeepSeek direct, Ollama/LM Studio on localhost, vLLM.

## Considered Options

- Embedding the client inside the examples scenario module — rejected: the
  owner plans to reuse LLM "brains" in future projects beyond Jade, and a
  framework-free jar is directly consumable anywhere.
- Provider-specific SDKs — rejected: OpenRouter and most gateways speak the
  OpenAI wire format; one HTTP client covers them all via configurable
  baseUrl/model/key.
- Spawning CLI agent tools (opencode/claude-code) as brains — deferred: the
  `Brain` interface leaves room for a future `CliBrain` implementation without
  touching scenarios.

## Consequences

- `backend/llm` must never import `io.donbee.jade.*`; doing so would couple
  every future brain consumer to the agent platform.
- Secrets are supplied to `HttpBrain` via a key supplier; resolution from
  environment/local files is the caller's concern (see ADR-0002).
