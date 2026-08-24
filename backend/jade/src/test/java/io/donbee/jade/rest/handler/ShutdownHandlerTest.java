package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

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
public class ShutdownHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ShutdownHandler handler;

    @Before
    public void setUp() {
        handler = new ShutdownHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_MainContainer_When_Shutdown_Then_CallsServiceAndReturns200() {
        // Arrange
        doNothing().when(mockService).shutdownPlatform();

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).shutdownPlatform();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_Shutdown_Then_FailsWith403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
        verify(mockService, never()).shutdownPlatform();
    }

    @Test
    public void Given_ServiceThrowsIllegalStateException_When_Shutdown_Then_FailsWith403() {
        // Arrange
        doThrow(new IllegalStateException("Already shutting down")).when(mockService).shutdownPlatform();

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_Shutdown_Then_FailsWith500() {
        // Arrange
        doThrow(new RuntimeException("Connection refused")).when(mockService).shutdownPlatform();

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
