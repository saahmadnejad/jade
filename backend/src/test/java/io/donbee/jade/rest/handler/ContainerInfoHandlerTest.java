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

import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerInfoHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ContainerInfoHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerInfoHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    @Test
    public void Given_ContainerExists_When_GetContainer_Then_ReturnsContainerDetails() {
        // Arrange
        mockPathParam("Main-Container");
        PlatformService.ContainerInfo container = new PlatformService.ContainerInfo(
            "Main-Container", "127.0.0.1", "1099", true);
        when(mockService.getContainer("Main-Container")).thenReturn(container);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getContainer("Main-Container");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ContainerDoesNotExist_When_GetContainer_Then_Returns404() {
        // Arrange
        mockPathParam("NonExistent");
        when(mockService.getContainer("NonExistent")).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_GetContainer_Then_Returns403() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_EmptyContainerName_When_GetContainer_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of("name", ""));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_ServiceThrowsException_When_GetContainer_Then_Returns500() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.getContainer("Main-Container"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
