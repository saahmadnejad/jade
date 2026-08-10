package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerMTPInstallHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    private ContainerMTPInstallHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerMTPInstallHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    @Test
    public void Given_ValidRequestBody_When_InstallMTP_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("Main-Container");
        JsonObject body = new JsonObject()
            .put("className", "jade.mtp.tcl.TcpMTP")
            .put("address", "127.0.0.1:1100");
        when(mockBody.asJsonObject()).thenReturn(body);
        io.donbee.jade.mtp.MTPDescriptor desc = mock(io.donbee.jade.mtp.MTPDescriptor.class);
        when(mockService.installMTP("Main-Container", "127.0.0.1:1100", "jade.mtp.tcl.TcpMTP"))
            .thenReturn(desc);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).installMTP("Main-Container", "127.0.0.1:1100", "jade.mtp.tcl.TcpMTP");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingClassName_When_InstallMTP_Then_Returns400() {
        // Arrange
        mockPathParam("Main-Container");
        JsonObject body = new JsonObject().put("address", "127.0.0.1:1100");
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).installMTP(anyString(), anyString(), anyString());
    }

    @Test
    public void Given_MissingAddress_When_InstallMTP_Then_Returns400() {
        // Arrange
        mockPathParam("Main-Container");
        JsonObject body = new JsonObject().put("className", "jade.mtp.tcl.TcpMTP");
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).installMTP(anyString(), anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_InstallMTP_Then_Returns403() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ContainerNotFound_When_InstallMTP_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        JsonObject body = new JsonObject()
            .put("className", "jade.mtp.tcl.TcpMTP")
            .put("address", "127.0.0.1:1100");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new IllegalArgumentException("Container not found: nonexistent"))
            .when(mockService).installMTP("nonexistent", "127.0.0.1:1100", "jade.mtp.tcl.TcpMTP");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }
}
