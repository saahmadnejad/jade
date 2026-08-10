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
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerMTPListHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private ContainerMTPListHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerMTPListHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    @Test
    public void Given_ContainerWithMTPs_When_ListMTPs_Then_Returns200WithMTPArray() {
        // Arrange
        mockPathParam("Main-Container");
        List<PlatformService.MTPInfo> mtps = new ArrayList<>();
        mtps.add(new PlatformService.MTPInfo("127.0.0.1:1099", "jade.mtp.tcl.TcpMTP"));
        when(mockService.getMTPs("Main-Container")).thenReturn(mtps);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getMTPs("Main-Container");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ContainerWithNoMTPs_When_ListMTPs_Then_Returns200WithEmptyArray() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.getMTPs("Main-Container")).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_ListMTPs_Then_Returns403() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ContainerNotFound_When_ListMTPs_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        doThrow(new IllegalArgumentException("Container not found: nonexistent"))
            .when(mockService).getMTPs("nonexistent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_ListMTPs_Then_Returns500() {
        // Arrange
        mockPathParam("Node1");
        doThrow(new RuntimeException("Internal error"))
            .when(mockService).getMTPs("Node1");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
