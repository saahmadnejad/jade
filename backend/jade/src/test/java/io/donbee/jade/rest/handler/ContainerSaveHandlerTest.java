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
public class ContainerSaveHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    private ContainerSaveHandler handler;

    @Before
    public void setUp() {
        handler = new ContainerSaveHandler(mockService);
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
    public void Given_ValidRequestBody_When_SaveContainer_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("Main-Container");
        JsonObject body = new JsonObject().put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        doNothing().when(mockService).saveContainer("Main-Container", "file://./store");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).saveContainer("Main-Container", "file://./store");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingRepository_When_SaveContainer_Then_Returns400() {
        // Arrange
        mockPathParam("Main-Container");
        JsonObject body = new JsonObject();
        when(mockBody.asJsonObject()).thenReturn(body);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).saveContainer(anyString(), anyString());
    }

    @Test
    public void Given_NonMainContainer_When_SaveContainer_Then_Returns403() {
        // Arrange
        mockPathParam("Main-Container");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ContainerNotFound_When_SaveContainer_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        JsonObject body = new JsonObject().put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new IllegalArgumentException("Container not found: nonexistent"))
            .when(mockService).saveContainer("nonexistent", "file://./store");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_SaveContainer_Then_Returns500() {
        // Arrange
        mockPathParam("Node1");
        JsonObject body = new JsonObject().put("repository", "file://./store");
        when(mockBody.asJsonObject()).thenReturn(body);
        doThrow(new RuntimeException("Internal error"))
            .when(mockService).saveContainer("Node1", "file://./store");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
