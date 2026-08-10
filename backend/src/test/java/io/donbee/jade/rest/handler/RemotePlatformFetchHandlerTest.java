package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.RequestBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemotePlatformFetchHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private RemotePlatformFetchHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformFetchHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ValidUrl_When_FetchPlatform_Then_Returns201() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("url", "host:1099"));
        PlatformService.RemotePlatformInfo platform =
            new PlatformService.RemotePlatformInfo("host:1099/FIPA", "host:1099/FIPA",
                new String[]{"host:1099"}, new String[]{"FIPAAgentManagement"});
        when(mockService.fetchRemotePlatform("host:1099")).thenReturn(platform);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).fetchRemotePlatform("host:1099");
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_FetchPlatform_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_MissingUrl_When_FetchPlatform_Then_Returns400() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_InvalidUrl_When_FetchPlatform_Then_Returns400() {
        // Arrange
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject().put("url", "invalid"));
        when(mockService.fetchRemotePlatform("invalid"))
            .thenThrow(new IllegalArgumentException("Invalid URL format"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }
}
