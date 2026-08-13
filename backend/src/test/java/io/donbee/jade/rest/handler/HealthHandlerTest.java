package io.donbee.jade.rest.handler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HealthHandlerTest {

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private HealthHandler handler;

    @Before
    public void setUp() {
        handler = new HealthHandler();
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_Request_When_HealthCheck_Then_Returns200WithOkStatus() {
        // Arrange

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).putHeader("Content-Type", "application/json");
        verify(mockResponse).end(any(Buffer.class));
    }
}
