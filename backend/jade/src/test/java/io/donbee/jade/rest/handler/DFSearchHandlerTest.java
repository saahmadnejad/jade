package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;

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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DFSearchHandlerTest {

    @Mock
    private DFService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    @Before
    public void setUp() {
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        doAnswer(inv -> mockResponse).when(mockResponse).end(any(Buffer.class));
    }

    private DFRegistrationInfo sampleResult(String name) {
        List<DFServiceInfo> services = new ArrayList<>();
        services.add(new DFServiceInfo("weather-forecast", "weather-service", ""));
        return new DFRegistrationInfo(name, new ArrayList<>(List.of("jades://127.0.0.1:1099")), services, "");
    }

    @Test
    public void Given_SearchReturnsResults_When_SearchDF_Then_Returns200WithResults() {
        // Arrange
        DFSearchHandler handler = new DFSearchHandler(mockService);
        JsonObject body = new JsonObject()
            .put("description", new JsonObject())
            .put("constraints", new JsonObject().put("maxResults", -1).put("maxDepth", 0));
        when(mockBody.asJsonObject()).thenReturn(body);
        List<DFRegistrationInfo> results = new ArrayList<>();
        results.add(sampleResult("weather-agent@host"));
        when(mockService.searchDF(any(), any())).thenReturn(results);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).searchDF(any(), any());
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_SearchReturnsEmpty_When_SearchDF_Then_Returns200WithEmptyResults() {
        // Arrange
        DFSearchHandler handler = new DFSearchHandler(mockService);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());
        when(mockService.searchDF(any(), any())).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).searchDF(any(), any());
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NoBody_When_SearchDF_Then_Returns400() {
        // Arrange
        DFSearchHandler handler = new DFSearchHandler(mockService);
        when(mockBody.asJsonObject()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).searchDF(any(), any());
    }

    @Test
    public void Given_NonMainContainer_When_SearchDF_Then_Returns403() {
        // Arrange
        DFSearchHandler handler = new DFSearchHandler(mockService);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());
        when(mockService.searchDF(any(), any()))
            .thenThrow(new IllegalStateException("Not a Main Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }
}
