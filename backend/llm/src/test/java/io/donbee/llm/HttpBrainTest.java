package io.donbee.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HttpBrainTest {

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

    private HttpBrain brainAtLocalServer() {
        return new HttpBrain(LlmConfig
            .builder("http://localhost:" + server.getAddress().getPort() + "/v1", "test-model")
            .apiKey(() -> "test-key")
            .timeoutMs(2000)
            .build());
    }

    @Test
    public void Given_FakeProvider_When_Respond_Then_SendsOpenAiShapedRequestAndReturnsContent() {
        // --- Arrange ---
        HttpBrain brain = brainAtLocalServer();

        // --- Act ---
        String answer = brain.respond("You are a coder.", "Write hello world");

        // --- Assert ---
        assertThat(answer).isEqualTo("ok");
        assertThat(lastRequestBody.get())
            .contains("\"model\":\"test-model\"")
            .contains("\"role\":\"system\"")
            .contains("You are a coder.")
            .contains("\"role\":\"user\"")
            .contains("Write hello world");
        assertThat(lastAuthHeader.get()).isEqualTo("Bearer test-key");
    }

    @Test
    public void Given_ProviderError_When_Respond_Then_BrainExceptionCarriesStatusAndBody() {
        // --- Arrange ---
        HttpBrain brain = brainAtLocalServer();
        responseStatus = 429;
        responseBody = "{\"error\":{\"message\":\"rate limited\"}}";

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond("s", "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("429")
            .hasMessageContaining("rate limited");
    }

    @Test
    public void Given_MalformedSuccessBody_When_Respond_Then_BrainExceptionAboutMissingContent() {
        // --- Arrange ---
        HttpBrain brain = brainAtLocalServer();
        responseStatus = 200;
        responseBody = "{\"choices\":[]}";

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond(null, "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("content");
    }

    @Test
    public void Given_NoApiKey_When_Respond_Then_FailsFastWithoutCallingProvider() {
        // --- Arrange ---
        HttpBrain brain = new HttpBrain(LlmConfig
            .builder("http://localhost:" + server.getAddress().getPort() + "/v1", "m")
            .apiKey(() -> null)
            .build());

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond("s", "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("No API key");
        assertThat(lastRequestBody.get()).isNull(); // provider never called
    }

    @Test
    public void Given_BlankSystemPrompt_When_BuildPayload_Then_SystemMessageOmitted() {
        // --- Arrange ---
        HttpBrain brain = brainAtLocalServer();

        // --- Act ---
        ObjectNode payload = (ObjectNode) brain.buildPayload("", "just this");

        // --- Assert ---
        assertThat(payload.path("messages")).hasSize(1);
        assertThat(payload.path("messages").get(0).path("role").asText()).isEqualTo("user");
    }
}
