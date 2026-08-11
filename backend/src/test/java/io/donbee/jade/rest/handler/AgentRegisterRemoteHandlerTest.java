package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.RequestBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentRegisterRemoteHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private AgentRegisterRemoteHandler handler;

    @Before
    public void setUp() {
        handler = new AgentRegisterRemoteHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ValidAid_When_RegisterRemoteAgent_Then_Returns200() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject()
            .put("aid", "foreign-agent@foreign-platform")
            .put("addresses", new io.vertx.core.json.JsonArray().add("jades://192.168.1.10:1200")));
        doNothing().when(mockService).registerRemoteAgent(anyString(), any());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).registerRemoteAgent("foreign-agent@foreign-platform",
            new String[]{"jades://192.168.1.10:1200"});
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_RegisterRemoteAgent_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_MissingAid_When_RegisterRemoteAgent_Then_Returns400() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("addresses",
            new io.vertx.core.json.JsonArray().add("jades://192.168.1.10:1200")));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_NoAddresses_When_RegisterRemoteAgent_Then_Returns200() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("aid", "foreign-agent@foreign"));
        doNothing().when(mockService).registerRemoteAgent(anyString(), any());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).registerRemoteAgent("foreign-agent@foreign", null);
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }
}
