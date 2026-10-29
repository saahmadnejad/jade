package io.donbee.jade.rest.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@RunWith(MockitoJUnitRunner.class)
public class MessagesStreamHandlerTest {

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerRequest mockRequest;

    @Mock
    private ServerWebSocket mockSocket;

    private Vertx vertx;
    private MessagesStreamHandler handler;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        handler = new MessagesStreamHandler();
        when(mockContext.request()).thenReturn(mockRequest);
    }

    @After
    public void tearDown() throws Exception {
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(ar -> latch.countDown());
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void Given_WebSocketUpgradeFails_When_HandleCalled_Then_FailsRoutingContext() {
        // --- Arrange ---
        Throwable cause = new RuntimeException("Upgrade failure");
        when(mockRequest.toWebSocket()).thenReturn(Future.failedFuture(cause));

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockContext).fail(cause);
    }

    @Test
    public void Given_ConnectedWebSocket_When_MessagePublishedToEventBus_Then_WritesTruncatedFrameToSocket() throws Exception {
        // --- Arrange ---
        when(mockContext.vertx()).thenReturn(vertx);
        when(mockRequest.toWebSocket()).thenReturn(Future.succeededFuture(mockSocket));
        when(mockSocket.isClosed()).thenReturn(false);
        when(mockSocket.writeQueueFull()).thenReturn(false);

        CountDownLatch writeLatch = new CountDownLatch(1);
        when(mockSocket.writeTextMessage(any())).thenAnswer(invocation -> {
            writeLatch.countDown();
            return null;
        });

        handler.handle(mockContext);

        String longContent = "payload-" + "x".repeat(300);
        JsonObject message = new JsonObject()
            .put("id", "stream-1")
            .put("content", longContent);

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, message);

        // --- Assert ---
        writeLatch.await(3, TimeUnit.SECONDS);
        verify(mockSocket).writeTextMessage(argThat(payload -> {
            JsonObject frame = new JsonObject(payload);
            return frame.getString("content").endsWith("...") && frame.getString("content").length() == 256 + 3;
        }));
    }

    @Test
    public void Given_WriteQueueFull_When_MessagePublished_Then_RegistersDrainHandler() throws Exception {
        // --- Arrange ---
        when(mockContext.vertx()).thenReturn(vertx);
        when(mockRequest.toWebSocket()).thenReturn(Future.succeededFuture(mockSocket));
        when(mockSocket.isClosed()).thenReturn(false);
        when(mockSocket.writeQueueFull()).thenReturn(true);

        CountDownLatch drainLatch = new CountDownLatch(1);
        when(mockSocket.drainHandler(any())).thenAnswer(invocation -> {
            drainLatch.countDown();
            return mockSocket;
        });

        handler.handle(mockContext);

        JsonObject message = new JsonObject().put("id", "backpressure-1").put("content", "hello");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, message);

        // --- Assert ---
        assertThat(drainLatch.await(3, TimeUnit.SECONDS)).isTrue();
        verify(mockSocket).drainHandler(any());
    }

    @Test
    public void Given_SocketClosed_When_CloseHandlerFires_Then_ConsumerUnregistered() throws Exception {
        // --- Arrange ---
        when(mockContext.vertx()).thenReturn(vertx);
        when(mockRequest.toWebSocket()).thenReturn(Future.succeededFuture(mockSocket));

        handler.handle(mockContext);

        ArgumentCaptor<Handler<Void>> captor = ArgumentCaptor.forClass(Handler.class);
        verify(mockSocket).closeHandler(captor.capture());

        // --- Act: fire socket close ---
        captor.getValue().handle(null);

        // Publish message after close
        JsonObject message = new JsonObject().put("id", "post-close").put("content", "should-not-arrive");
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, message);

        // Allow EventBus dispatch to settle
        Thread.sleep(200);

        // --- Assert ---
        verify(mockSocket, never()).writeTextMessage(any());
    }

    @Test
    public void Given_SocketException_When_ExceptionHandlerFires_Then_ConsumerUnregistered() throws Exception {
        // --- Arrange ---
        when(mockContext.vertx()).thenReturn(vertx);
        when(mockRequest.toWebSocket()).thenReturn(Future.succeededFuture(mockSocket));

        handler.handle(mockContext);

        ArgumentCaptor<Handler<Throwable>> captor = ArgumentCaptor.forClass(Handler.class);
        verify(mockSocket).exceptionHandler(captor.capture());

        // --- Act: fire socket exception (e.g. broken pipe) ---
        captor.getValue().handle(new RuntimeException("Connection reset"));

        // Publish message after exception
        JsonObject message = new JsonObject().put("id", "post-exception").put("content", "should-not-arrive");
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, message);

        // Allow EventBus dispatch to settle
        Thread.sleep(200);

        // --- Assert ---
        verify(mockSocket, never()).writeTextMessage(any());
    }
}
