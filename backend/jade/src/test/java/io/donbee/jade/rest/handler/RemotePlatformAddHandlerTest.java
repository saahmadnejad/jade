package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemotePlatformAddHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;
    @Mock
    private RequestBody mockBody;

    private RemotePlatformAddHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformAddHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ValidRequestBody_When_AddPlatform_Then_CallsServiceAndReturns201() {
        // Arrange
        JsonObject body = new JsonObject()
            .put("ams", "ams@remote")
            .put("addresses", new JsonArray().add("127.0.0.1:1099"));
        when(mockBody.asJsonObject()).thenReturn(body);
        PlatformService.RemotePlatformInfo info = new PlatformService.RemotePlatformInfo(
            "remote", "ams@remote", new String[]{"127.0.0.1:1099"}, new String[]{"FIPAAgentManagement"});
        when(mockService.addRemotePlatform("ams@remote", new String[]{"127.0.0.1:1099"})).thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).addRemotePlatform("ams@remote", new String[]{"127.0.0.1:1099"});
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingAms_When_AddPlatform_Then_Returns400() {
        // Arrange
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).addRemotePlatform(anyString(), any());
    }

    @Test
    public void Given_NonMainContainer_When_AddPlatform_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ServiceThrows_When_AddPlatform_Then_Returns400() {
        // Arrange
        JsonObject body = new JsonObject()
            .put("ams", "ams@remote")
            .put("addresses", new JsonArray().add("127.0.0.1:1099"));
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new RuntimeException("Connection refused"))
            .when(mockService).addRemotePlatform("ams@remote", new String[]{"127.0.0.1:1099"});

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }
}
