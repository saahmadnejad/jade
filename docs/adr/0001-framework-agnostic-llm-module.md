# LLM brains live in a framework-agnostic module

Role agents whose reasoning is delegated to an LLM get that capability from a
dedicated Maven module `io.donbee:llm` (`backend/llm`) with **zero JADE
dependencies**: a plain Java 21 library exposing a `Brain` interface (prompt
in → text out, optional tool-calling agent loop, per-call model selection,
optional SOCKS5 proxy, blocking with timeout). The main implementation
(`LangChain4jBrain`) uses langchain4j's OpenAI-compatible client, so any
provider works by changing config: 9router (free via 9router), OpenRouter
(free or paid), OpenAI, DeepSeek direct, Ollama/LM Studio on localhost, vLLM.
`BashTool` runs shell commands in a workspace dir for the tool-calling loop;
`FallbackBrain` chains brains on failure; `CliBrain` wraps a CLI agent as a
brain.

## Considered Options

- Embedding the client inside the examples scenario module — rejected: the
  owner plans to reuse LLM "brains" in future projects beyond Jade, and a
  framework-free jar is directly consumable anywhere.
- Provider-specific SDKs — rejected: 9router and most gateways speak the
  OpenAI wire format; langchain4j's OpenAI client covers them all via
  configurable baseUrl/model/key.
- Spawning CLI agent tools (opencode/claude-code/gemini-cli) as brains — rejected for the
  dev-team scenario: it uses langchain4j for direct HTTP calls to 9router
  instead of spawning a CLI process. A `CliBrain` implementation remains in the
  module for callers that prefer wrapping a CLI.

## Consequences

- `backend/llm` must never import `io.donbee.jade.*`; doing so would couple
  every future brain consumer to the agent platform.
- Secrets are supplied to the brain via a key supplier; resolution from
  environment/local files is the caller's concern (see ADR-0002).
