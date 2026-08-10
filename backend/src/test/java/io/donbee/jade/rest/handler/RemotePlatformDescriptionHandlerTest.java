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
public class RemotePlatformDescriptionHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;

    private RemotePlatformDescriptionHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformDescriptionHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ExistingPlatform_When_GetDescription_Then_Returns200WithDetails() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of("name", "jade-main"));
        PlatformService.RemotePlatformInfo platform =
            new PlatformService.RemotePlatformInfo("jade-main", "ams@main",
                new String[]{"127.0.0.1:1099"}, new String[]{"FIPAAgentManagement"});
        when(mockService.getRemotePlatformDescription("jade-main")).thenReturn(platform);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getRemotePlatformDescription("jade-main");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_GetDescription_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_MissingPlatformName_When_GetDescription_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_UnknownPlatform_When_GetDescription_Then_Returns404() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(java.util.Map.of("name", "unknown"));
        when(mockService.getRemotePlatformDescription("unknown"))
            .thenThrow(new IllegalArgumentException("Remote platform not found: unknown"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }
}
