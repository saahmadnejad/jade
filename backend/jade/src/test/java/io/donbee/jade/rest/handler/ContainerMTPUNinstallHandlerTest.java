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

import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerMTPUNinstallHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ContainerMTPUNinstallHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerMTPUNinstallHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParams(String name, String address) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name, "address", address));
    }

    @Test
    public void Given_ValidPathParams_When_UninstallMTP_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParams("Main-Container", "127.0.0.1:1100");
        doNothing().when(mockService).uninstallMTP("Main-Container", "127.0.0.1:1100");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).uninstallMTP("Main-Container", "127.0.0.1:1100");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingAddress_When_UninstallMTP_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of("name", "Main-Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).uninstallMTP(anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_UninstallMTP_Then_Returns403() {
        // Arrange
        mockPathParams("Main-Container", "127.0.0.1:1100");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ContainerNotFound_When_UninstallMTP_Then_Returns404() {
        // Arrange
        mockPathParams("nonexistent", "127.0.0.1:1100");
        doThrow(new IllegalArgumentException("Container not found: nonexistent"))
            .when(mockService).uninstallMTP("nonexistent", "127.0.0.1:1100");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_UninstallMTP_Then_Returns500() {
        // Arrange
        mockPathParams("Node1", "127.0.0.1:1100");
        doThrow(new RuntimeException("MTP uninstall error"))
            .when(mockService).uninstallMTP("Node1", "127.0.0.1:1100");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
