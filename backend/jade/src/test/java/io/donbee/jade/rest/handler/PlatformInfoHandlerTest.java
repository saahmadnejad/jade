package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.rest.service.PlatformService.PlatformInfo;

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
public class PlatformInfoHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private PlatformInfoHandler handler;

    @Before
    public void setUp() {
        handler = new PlatformInfoHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_PlatformInfoAvailable_When_GetPlatformInfo_Then_Returns200WithInfo() {
        // Arrange
        PlatformInfo pi = new PlatformInfo(
            "jade-platform-001", "Main-Container", true, "ams@host", "df@host");
        when(mockService.getPlatformInfo()).thenReturn(pi);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getPlatformInfo();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ServiceThrowsIllegalStateException_When_GetPlatformInfo_Then_FailsWith500() {
        // Arrange
        when(mockService.getPlatformInfo()).thenThrow(new RuntimeException("Platform not ready"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
