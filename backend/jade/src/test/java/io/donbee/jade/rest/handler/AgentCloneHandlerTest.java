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
public class AgentCloneHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private AgentCloneHandler handler;

    @Before
    public void setUp() {
        handler = new AgentCloneHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ValidRequestBody_When_CloneAgent_Then_CallsServiceAndReturns201() {
        // Arrange
        JsonObject body = new JsonObject()
            .put("name", "original-agent")
            .put("newName", "cloned-agent")
            .put("container", "Node1");
        when(mockBody.asJsonObject()).thenReturn(body);
        PlatformService.AgentInfo info = new PlatformService.AgentInfo("cloned-agent@host", "ACTIVE", null, "Node1", new String[]{});
        when(mockService.cloneAgent("original-agent", "cloned-agent", "Node1")).thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).cloneAgent("original-agent", "cloned-agent", "Node1");
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingNewName_When_CloneAgent_Then_Returns400() {
        // Arrange
        JsonObject body = new JsonObject()
            .put("name", "original-agent")
            .put("container", "Node1");
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).cloneAgent(anyString(), anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_CloneAgent_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_AgentNotFound_When_CloneAgent_Then_Returns404() {
        // Arrange
        JsonObject body = new JsonObject()
            .put("name", "nonexistent")
            .put("newName", "cloned-agent")
            .put("container", "Node1");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).cloneAgent("nonexistent", "cloned-agent", "Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }
}
