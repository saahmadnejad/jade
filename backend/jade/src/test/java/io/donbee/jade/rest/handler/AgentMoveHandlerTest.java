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
public class AgentMoveHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private AgentMoveHandler handler;

    @Before
    public void setUp() {
        handler = new AgentMoveHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    @Test
    public void Given_ValidRequestBody_When_MoveAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("my-agent");
        JsonObject body = new JsonObject().put("container", "Node1");
        when(mockBody.asJsonObject()).thenReturn(body);
        doNothing().when(mockService).moveAgent("my-agent", "Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).moveAgent("my-agent", "Node1");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingContainer_When_MoveAgent_Then_Returns400() {
        // Arrange
        mockPathParam("my-agent");
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).moveAgent(anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_MoveAgent_Then_Returns403() {
        // Arrange
        mockPathParam("my-agent");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_AgentNotFound_When_MoveAgent_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        JsonObject body = new JsonObject().put("container", "Node1");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).moveAgent("nonexistent", "Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }
}
