package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.rest.service.PlatformService.ContainerInfo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerListHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ContainerListHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerListHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ContainersAvailable_When_GetContainerList_Then_Returns200WithContainerData() {
        // Arrange
        ContainerInfo ci1 = new ContainerInfo("Main-Container", "127.0.0.1", "1099", true);
        ContainerInfo ci2 = new ContainerInfo("Node1", "192.168.1.1", "1099", false);
        when(mockService.getContainers()).thenReturn(List.of(ci1, ci2));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getContainers();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_EmptyContainerList_When_GetContainerList_Then_Returns200WithEmptyArray() {
        // Arrange
        when(mockService.getContainers()).thenReturn(List.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getContainers();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_GetContainerList_Then_FailsWith403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
        verify(mockService, never()).getContainers();
    }

    @Test
    public void Given_ServiceThrowsException_When_GetContainerList_Then_FailsWith500() {
        // Arrange
        when(mockService.getContainers()).thenThrow(new RuntimeException("Connection refused"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
