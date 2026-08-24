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

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentActionHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private AgentActionHandler handler;

    @Before
    public void setUp() {
        handler = new AgentActionHandler(mockService, AgentActionHandler.Action.KILL);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    // ===== KILL =====

    @Test
    public void Given_ValidAgentName_When_KillAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        mockPathParam("test-agent");
        doNothing().when(mockService).killAgent("test-agent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).killAgent("test-agent");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonExistentAgent_When_KillAgent_Then_FailsWith404() {
        // Arrange
        mockPathParam("nonexistent");
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).killAgent("nonexistent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_KillAgent_Then_FailsWith403() {
        // Arrange
        mockPathParam("test-agent");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_KillAgent_Then_FailsWith500() {
        // Arrange
        mockPathParam("test-agent");
        doThrow(new RuntimeException("Connection refused"))
            .when(mockService).killAgent("test-agent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }

    @Test
    public void Given_NullAgentName_When_KillAgent_Then_FailsWith400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Collections.emptyMap());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    // ===== SUSPEND =====

    @Test
    public void Given_ValidAgentName_When_SuspendAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        handler = new AgentActionHandler(mockService, AgentActionHandler.Action.SUSPEND);
        mockPathParam("test-agent");
        doNothing().when(mockService).suspendAgent("test-agent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).suspendAgent("test-agent");
        verify(mockResponse).setStatusCode(200);
    }

    @Test
    public void Given_NonExistentAgent_When_SuspendAgent_Then_FailsWith404() {
        // Arrange
        handler = new AgentActionHandler(mockService, AgentActionHandler.Action.SUSPEND);
        mockPathParam("nonexistent");
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).suspendAgent("nonexistent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    // ===== RESUME =====

    @Test
    public void Given_ValidAgentName_When_ResumeAgent_Then_CallsServiceAndReturns200() {
        // Arrange
        handler = new AgentActionHandler(mockService, AgentActionHandler.Action.RESUME);
        mockPathParam("test-agent");
        doNothing().when(mockService).resumeAgent("test-agent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).resumeAgent("test-agent");
        verify(mockResponse).setStatusCode(200);
    }

    @Test
    public void Given_NonExistentAgent_When_ResumeAgent_Then_FailsWith404() {
        // Arrange
        handler = new AgentActionHandler(mockService, AgentActionHandler.Action.RESUME);
        mockPathParam("nonexistent");
        doThrow(new IllegalArgumentException("Agent not found: nonexistent"))
            .when(mockService).resumeAgent("nonexistent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    // ===== Container suffix stripping =====

    @Test
    public void Given_AgentNameWithContainerSuffix_When_KillAgent_Then_StripsSuffixAndCallsService() {
        // Arrange
        mockPathParam("test-agent@container");
        doNothing().when(mockService).killAgent("test-agent");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).killAgent("test-agent");
    }
}
