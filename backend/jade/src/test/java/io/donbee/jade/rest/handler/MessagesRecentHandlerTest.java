package io.donbee.jade.rest.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.MessageTrafficService;

@RunWith(MockitoJUnitRunner.class)
public class MessagesRecentHandlerTest {

    private static final JsonObject SAMPLE = new JsonObject()
        .put("id", "1")
        .put("sender", "shop")
        .put("receiver", "inventory")
        .put("performative", "request")
        .put("content", "(buy sku-1 1)");

    @Mock
    private MessageTrafficService mockTrafficService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private MessagesRecentHandler handler;

    @Before
    public void setUp() {
        handler = new MessagesRecentHandler(mockTrafficService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockQueryParams(String limit, String from, String to) {
        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        if (limit != null) params.add("limit", limit);
        if (from != null) params.add("from", from);
        if (to != null) params.add("to", to);
        when(mockContext.queryParams()).thenReturn(params);
    }

    @Test
    public void Given_NoParams_When_RecentRequested_Then_ReturnsDefaultHistoryWithDroppedCount() {
        // --- Arrange ---
        mockQueryParams(null, null, null);
        when(mockTrafficService.recent(100, null, null)).thenReturn(List.of(SAMPLE));
        when(mockTrafficService.getDroppedCount()).thenReturn(7L);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockResponse).end(Buffer.buffer(new JsonObject()
            .put("messages", new io.vertx.core.json.JsonArray().add(SAMPLE))
            .put("total", 1)
            .put("dropped", 7)
            .encode()));
    }

    @Test
    public void Given_LimitAndFilters_When_RecentRequested_Then_PassedToService() {
        // --- Arrange ---
        mockQueryParams("25", "shop", "inventory");
        when(mockTrafficService.recent(25, "shop", "inventory")).thenReturn(List.of());

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockTrafficService).recent(25, "shop", "inventory");
        verify(mockResponse).end(org.mockito.ArgumentMatchers.any(io.vertx.core.buffer.Buffer.class));
    }

    @Test
    public void Given_InvalidLimit_When_RecentRequested_Then_FailsWith400() {
        // --- Arrange ---
        mockQueryParams("not-a-number", null, null);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockContext).fail(org.mockito.ArgumentMatchers.eq(400), any());
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    private static Exception any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
