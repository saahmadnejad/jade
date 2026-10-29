package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageSendHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    @Mock
    private Vertx mockVertx;

    private MessageSendHandler handler;

    @Before
    public void setUp() {
        handler = new MessageSendHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockContext.vertx()).thenReturn(mockVertx);

        when(mockVertx.executeBlocking(any(Callable.class)))
            .thenAnswer(inv -> {
                Callable<?> callable = inv.getArgument(0);
                return Future.succeededFuture(callable.call());
            });
    }

    @Test
    public void Given_NotMainContainer_When_SendMessage_Then_FailsWith403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_MissingBody_When_SendMessage_Then_Returns400() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockBody.asJsonObject()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(400);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_InvalidPerformative_When_SendMessage_Then_Returns400() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        JsonObject body = new JsonObject()
            .put("performative", "INVALID_PERFORMATIVE")
            .put("receivers", new JsonArray().add("ams@jade-main"));
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(400);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingReceivers_When_SendMessage_Then_Returns400() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        JsonObject body = new JsonObject()
            .put("performative", "INFORM")
            .put("receivers", new JsonArray());
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(400);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ValidMessage_When_SendMessage_Then_Returns201AndCallsService() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        JsonObject body = new JsonObject()
            .put("performative", "INFORM")
            .put("sender", "sender@jade-main")
            .put("receivers", new JsonArray().add("ams@jade-main"))
            .put("content", "hello")
            .put("conversationId", "conv-1")
            .put("protocol", "fipa-request")
            .put("ontology", "test-onto")
            .put("language", "fipa-sl");
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).sendMessage(any());
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ValidNumericPerformative_When_SendMessage_Then_Returns201() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        JsonObject body = new JsonObject()
            .put("performative", 7) // 7 = INFORM
            .put("receivers", new JsonArray().add("ams@jade-main"))
            .put("content", "numeric test");
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).sendMessage(any());
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }
}
