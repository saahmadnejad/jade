package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
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
public class RemotePlatformRemoveHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;

    private RemotePlatformRemoveHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformRemoveHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ExistingPlatform_When_RemovePlatform_Then_Returns200() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of("name", "jade-main"));
        doNothing().when(mockService).removeRemotePlatform("jade-main");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).removeRemotePlatform("jade-main");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_RemovePlatform_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_NotSupported_When_RemovePlatform_Then_Returns501() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of("name", "jade-main"));
        doThrow(new UnsupportedOperationException("not supported"))
            .when(mockService).removeRemotePlatform("jade-main");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(501), any(Throwable.class));
    }

    @Test
    public void Given_MissingPlatformName_When_RemovePlatform_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }
}
