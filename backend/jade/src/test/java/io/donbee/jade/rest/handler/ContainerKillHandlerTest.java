package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.MultiMap;
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
public class ContainerKillHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ContainerKillHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerKillHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        queryParams.add("confirm", "true");
        when(mockContext.queryParams()).thenReturn(queryParams);
    }

    @Test
    public void Given_ValidContainerName_When_KillContainer_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("Node1");
        doNothing().when(mockService).killContainer("Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).killContainer("Node1");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(String.class));
    }

    @Test
    public void Given_ContainerNotFound_When_KillContainer_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        doThrow(new IllegalArgumentException("Container not found: nonexistent"))
            .when(mockService).killContainer("nonexistent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_MainContainer_When_KillContainer_Then_Returns403() {
        // Arrange
        mockPathParam("Main-Container");
        doThrow(new IllegalStateException("Cannot kill the Main Container"))
            .when(mockService).killContainer("Main-Container");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_KillContainer_Then_Returns403() {
        // Arrange
        mockPathParam("Node1");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_NullContainerName_When_KillContainer_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_KillContainer_Then_Returns500() {
        // Arrange
        mockPathParam("Node1");
        doThrow(new RuntimeException("Connection refused"))
            .when(mockService).killContainer("Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }

    @Test
    public void Given_MissingConfirmParam_When_KillContainer_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of("name", "Node1"));
        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        when(mockContext.queryParams()).thenReturn(queryParams);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).killContainer(anyString());
    }
}
