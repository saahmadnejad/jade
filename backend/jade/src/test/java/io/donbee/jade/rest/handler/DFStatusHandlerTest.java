package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFStatus;

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
public class DFStatusHandlerTest {

    @Mock
    private DFService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Before
    public void setUp() {
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        doAnswer(inv -> {
            return null;
        }).when(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_DFRunning_When_GetStatus_Then_Returns200WithStatus() {
        // Arrange
        DFStatusHandler handler = new DFStatusHandler(mockService);
        DFStatus status = new DFStatus(true, "df@host", "Main-Container", 5, 2, 3);
        when(mockService.getDFStatus()).thenReturn(status);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFStatus();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_DFNotRunning_When_GetStatus_Then_Returns200WithRunningFalse() {
        // Arrange
        DFStatusHandler handler = new DFStatusHandler(mockService);
        when(mockService.getDFStatus())
            .thenThrow(new IllegalStateException("Not a Main Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ServiceError_When_GetStatus_Then_Returns500() {
        // Arrange
        DFStatusHandler handler = new DFStatusHandler(mockService);
        when(mockService.getDFStatus()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
