package io.donbee.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LangChain4jBrainTest {

    private HttpServer server;
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = successfulChoice("ok");

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            lastRequestBody.set(readBody(exchange.getRequestBody()));
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    private static String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String successfulChoice(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"}}]}";
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1";
    }

    @Test
    public void Given_FakeProvider_When_Respond_Then_SendsOpenAiShapedRequestAndReturnsContent() {
        // --- Arrange ---
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "test-model",
            () -> "test-key", Duration.ofSeconds(5));

        // --- Act ---
        String answer = brain.respond("You are a coder.", "Write hello world");

        // --- Assert ---
        assertThat(answer).isEqualTo("ok");
        assertThat(lastRequestBody.get())
            .contains("test-model")
            .contains("system")
            .contains("You are a coder.")
            .contains("user")
            .contains("Write hello world");
        assertThat(lastAuthHeader.get()).isEqualTo("Bearer test-key");
    }

    @Test
    public void Given_NinerouterPrefix_When_Respond_Then_StripsPrefixInRequest() {
        // --- Arrange ---
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "9router/oc/laguna-s-2.1-free",
            () -> "test-key", Duration.ofSeconds(5));

        // --- Act ---
        brain.respond(null, "hi");

        // --- Assert ---
        assertThat(lastRequestBody.get())
            .contains("oc/laguna-s-2.1-free")
            .doesNotContain("9router/");
        assertThat(brain.model()).isEqualTo("oc/laguna-s-2.1-free");
    }

    @Test
    public void Given_ProviderError_When_Respond_Then_BrainExceptionCarriesStatusAndBody() {
        // --- Arrange ---
        responseStatus = 429;
        responseBody = "{\"error\":{\"message\":\"rate limited\"}}";
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "m",
            () -> "key", Duration.ofSeconds(5));

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond("s", "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("model m");
    }

    @Test
    public void Given_NoApiKey_When_Respond_Then_UsesDummyKey() {
        // --- Arrange ---
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "m",
            () -> null, Duration.ofSeconds(5));

        // --- Act ---
        String answer = brain.respond("s", "u");

        // --- Assert ---
        assertThat(answer).isEqualTo("ok");
    }

    @Test
    public void Given_BlankSystemPrompt_When_Respond_Then_SystemMessageOmitted() {
        // --- Arrange ---
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "m",
            () -> "key", Duration.ofSeconds(5));

        // --- Act ---
        brain.respond("", "just this");

        // --- Assert ---
        assertThat(lastRequestBody.get())
            .contains("just this")
            .doesNotContain("system");
    }

    @Test
    public void Given_ValidCommand_When_BashToolCalled_Then_RunsAndReturnsOutput() {
        // --- Arrange ---
        Brain.Tool bash = new BashTool(java.nio.file.Path.of("/tmp"), Duration.ofSeconds(10));

        // --- Act ---
        String result = bash.call("{\"command\": \"echo hello-bash-tool\", \"timeout\": \"5s\"}");

        // --- Assert ---
        assertThat(result).contains("hello-bash-tool").contains("exit_code: 0");
    }

    @Test
    public void Given_MissingCommand_When_BashToolCalled_Then_ErrorReturned() {
        // --- Arrange ---
        Brain.Tool bash = new BashTool(java.nio.file.Path.of("/tmp"), Duration.ofSeconds(10));

        // --- Act ---
        String result = bash.call("{\"timeout\": \"5s\"}");

        // --- Assert ---
        assertThat(result).contains("Error");
    }

    @Test
    public void Given_EscapedQuotesInCommand_When_BashToolCalled_Then_CommandUnescapesFully() throws IOException {
        // --- Arrange ---
        // Regression: naive regex stopped at the first \" and truncated the
        // command, making the LLM retry the same write 20+ times.
        Brain.Tool bash = new BashTool(java.nio.file.Path.of("/tmp"), Duration.ofSeconds(10));
        String args = "{\"command\": \"printf '%s' \\\"FizzBuzz\\\" > /tmp/bashtool-quote-test.txt\"}";

        // --- Act ---
        bash.call(args);

        // --- Assert ---
        assertThat(java.nio.file.Files.readString(
            java.nio.file.Path.of("/tmp/bashtool-quote-test.txt"))).isEqualTo("FizzBuzz");
    }

    @Test
    public void Given_NewlineEscapesInCommand_When_BashToolCalled_Then_HeredocWritesRealLines() throws IOException {
        // --- Arrange ---
        // Regression: the scanner used to drop the backslash before unescape(),
        // turning JSON \n into a literal 'n' and fusing heredoc lines together.
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("bashtool-nl");
        Brain.Tool bash = new BashTool(dir, Duration.ofSeconds(10));
        String args = "{\"command\": \"cat > note.txt <<'EOF'\\nline one\\nline two\\nEOF\\ncat note.txt\"}";

        // --- Act ---
        String result = bash.call(args);

        // --- Assert ---
        assertThat(result).contains("line one").contains("line two");
        assertThat(java.nio.file.Files.readString(dir.resolve("note.txt")))
            .isEqualTo("line one\nline two\n");
    }

    @Test
    public void Given_ResetWindowInMessage_When_Parsed_Then_ReturnsMatchingMillis() {
        // --- Arrange / Act / Assert ---
        assertThat(LangChain4jBrain.announcedResetMs(
            "... 429 ... (reset after 2m 45s)")).isEqualTo((2 * 60 + 45) * 1000L + 5000);
        assertThat(LangChain4jBrain.announcedResetMs(
            "... 429 ... (reset after 7s)")).isEqualTo(7_000L + 5000);
        assertThat(LangChain4jBrain.announcedResetMs("no window here")).isEqualTo(-1);
        assertThat(LangChain4jBrain.announcedResetMs(null)).isEqualTo(-1);
    }

    @Test
    public void Given_ProviderRateLimited_When_Respond_Then_RetriesUntilSuccess() {
        // --- Arrange ---
        // First call: 429 with reset window; second call: success.
        java.util.List<String> bodies = new java.util.ArrayList<>(java.util.List.of(
            "{\"error\":{\"message\":\"[429] rate limited (reset after 1s)\"}}",
            successfulChoice("ok-after-retry")));
        java.util.List<Integer> statuses = new java.util.ArrayList<>(java.util.List.of(429, 200));
        java.util.concurrent.atomic.AtomicInteger call = new java.util.concurrent.atomic.AtomicInteger();
        server.removeContext("/v1/chat/completions");
        server.createContext("/v1/chat/completions", exchange -> {
            int i = Math.min(call.getAndIncrement(), bodies.size() - 1);
            byte[] bytes = bodies.get(i).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statuses.get(i), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        LangChain4jBrain brain = LangChain4jBrain.build(baseUrl(), "m",
            () -> "key", Duration.ofSeconds(5));

        // --- Act ---
        String answer = brain.respond("s", "u");

        // --- Assert ---
        assertThat(answer).isEqualTo("ok-after-retry");
        assertThat(call.get()).isEqualTo(2);
    }
}
