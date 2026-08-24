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

import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentSaveLoadHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private AgentSaveLoadHandler handler;

    @Before
    public void setUp() {
        handler = new AgentSaveLoadHandler(mockService, true);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    // ===== Save Agent =====

    @Test
    public void Given_ValidRequestBody_When_SaveAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("my-agent");
        JsonObject body = new JsonObject().put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        doNothing().when(mockService).saveAgent("my-agent", "file://./store");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).saveAgent("my-agent", "file://./store");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingRepository_When_SaveAgent_Then_Returns400() {
        // Arrange
        mockPathParam("my-agent");
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).saveAgent(anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_SaveAgent_Then_Returns403() {
        // Arrange
        mockPathParam("my-agent");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_AgentNotFound_When_SaveAgent_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        JsonObject body = new JsonObject().put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).saveAgent("nonexistent", "file://./store");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    // ===== Load Agent =====

    @Test
    public void Given_ValidRequestBody_When_LoadAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        handler = new AgentSaveLoadHandler(mockService, false);
        JsonObject body = new JsonObject()
            .put("name", "my-agent")
            .put("container", "Main-Container")
            .put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        PlatformService.AgentInfo info = new PlatformService.AgentInfo("my-agent@host", "ACTIVE", null, "Main-Container", new String[]{});
        when(mockService.loadAgent("my-agent", "Main-Container", "file://./store")).thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).loadAgent("my-agent", "Main-Container", "file://./store");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }
}
