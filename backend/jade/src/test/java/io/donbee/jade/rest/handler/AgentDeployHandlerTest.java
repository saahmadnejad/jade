package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentDeployHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    private AgentDeployHandler handler;

    @Before
    public void setUp() {
        handler = new AgentDeployHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ValidRequestBody_When_DeployAgent_Then_Returns201() {
        // Arrange
        JsonObject body = new JsonObject().put("name", "test-agent").put("class", "com.example.TestAgent");
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "test-agent@host", null, null, "Main-Container", new String[]{});
        when(mockService.deployAgent("test-agent", "com.example.TestAgent", new Object[0]))
            .thenReturn(agent);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("test-agent", "com.example.TestAgent", new Object[0]);
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_RequestBodyWithArgs_When_DeployAgent_Then_PassesArgs() {
        // Arrange
        io.vertx.core.json.JsonArray args = new io.vertx.core.json.JsonArray().add("arg1").add("arg2");
        JsonObject body = new JsonObject().put("name", "test-agent").put("class", "com.example.TestAgent").put("args", args);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "test-agent@host", null, null, "Main-Container", new String[]{});
        when(mockService.deployAgent(anyString(), anyString(), any(Object[].class)))
            .thenReturn(agent);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent(eq("test-agent"), eq("com.example.TestAgent"), any(Object[].class));
    }

    @Test
    public void Given_MissingName_When_DeployAgent_Then_Returns400() {
        // Arrange
        JsonObject body = new JsonObject().put("class", "com.example.TestAgent");
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_MissingClass_When_DeployAgent_Then_Returns400() {
        // Arrange
        JsonObject body = new JsonObject().put("name", "test-agent");
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_NullBody_When_DeployAgent_Then_Returns400() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_DeployAgent_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_AgentAlreadyExists_When_DeployAgent_Then_Returns409() {
        // Arrange
        JsonObject body = new JsonObject().put("name", "existing-agent").put("class", "com.example.TestAgent");
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.deployAgent("existing-agent", "com.example.TestAgent", new Object[0]))
            .thenThrow(new IllegalArgumentException("Agent already exists: existing-agent"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(409), any(Throwable.class));
    }

    @Test
    public void Given_ServiceThrowsRuntimeException_When_DeployAgent_Then_Returns500() {
        // Arrange
        JsonObject body = new JsonObject().put("name", "test-agent").put("class", "com.example.TestAgent");
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.deployAgent("test-agent", "com.example.TestAgent", new Object[0]))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
