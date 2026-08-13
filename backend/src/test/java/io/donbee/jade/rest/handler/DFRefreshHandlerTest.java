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
public class DFRefreshHandlerTest {

    @Mock
    private DFService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private DFRefreshHandler handler;

    @Before
    public void setUp() {
        handler = new DFRefreshHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_DFStatusAvailable_When_Refresh_Then_Returns200WithCounts() {
        // Arrange
        DFStatus status = new DFStatus(true, "df-agent@host", "Main-Container", 3, 1, 2);
        when(mockService.getDFStatus()).thenReturn(status);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFStatus();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NotMainContainer_When_Refresh_Then_FailsWith403() {
        // Arrange
        when(mockService.getDFStatus()).thenThrow(new IllegalStateException("Not a Main Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ServiceThrowsUnexpectedError_When_Refresh_Then_FailsWith500() {
        // Arrange
        when(mockService.getDFStatus()).thenThrow(new RuntimeException("DF unreachable"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
