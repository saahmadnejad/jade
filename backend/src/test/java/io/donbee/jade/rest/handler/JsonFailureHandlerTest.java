package io.donbee.jade.rest.handler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonFailureHandlerTest {

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private JsonFailureHandler handler;

    @Before
    public void setUp() {
        handler = new JsonFailureHandler();
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_404FailureWithMessage_When_HandleError_Then_Returns404WithErrorBody() {
        // Arrange
        when(mockContext.statusCode()).thenReturn(404);
        when(mockContext.failure()).thenReturn(new RuntimeException("Resource not found"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(404);
        verify(mockResponse).putHeader("Content-Type", "application/json");
        verify(mockResponse).end(any(String.class));
    }

    @Test
    public void Given_403FailureWithMessage_When_HandleError_Then_Returns403WithErrorBody() {
        // Arrange
        when(mockContext.statusCode()).thenReturn(403);
        when(mockContext.failure()).thenReturn(new RuntimeException("Forbidden"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(403);
        verify(mockResponse).end(any(String.class));
    }

    @Test
    public void Given_500FailureWithMessage_When_HandleError_Then_Returns500WithErrorBody() {
        // Arrange
        when(mockContext.statusCode()).thenReturn(500);
        when(mockContext.failure()).thenReturn(new RuntimeException("Internal server error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(500);
        verify(mockResponse).end(any(String.class));
    }

    @Test
    public void Given_NoFailureSet_When_HandleError_Then_Returns500WithDefaultMessage() {
        // Arrange
        when(mockContext.statusCode()).thenReturn(500);
        when(mockContext.failure()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(500);
        verify(mockResponse).end(any(String.class));
    }

    @Test
    public void Given_StatusCodeBelow400_When_HandleError_Then_UpgradesTo500() {
        // Arrange
        when(mockContext.statusCode()).thenReturn(200);
        when(mockContext.failure()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(500);
    }
}
