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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemotePlatformListHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;

    private RemotePlatformListHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformListHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_MainContainer_When_ListPlatforms_Then_Returns200WithPlatformArray() {
        // Arrange
        List<PlatformService.RemotePlatformInfo> platforms = new ArrayList<>();
        platforms.add(new PlatformService.RemotePlatformInfo("jade-main", "ams@main",
            new String[]{"127.0.0.1:1099"}, new String[]{"FIPAAgentManagement"}));
        when(mockService.getRemotePlatforms()).thenReturn(platforms);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getRemotePlatforms();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_ListPlatforms_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_NoPlatforms_When_ListPlatforms_Then_Returns200WithEmptyArray() {
        // Arrange
        when(mockService.getRemotePlatforms()).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }
}
