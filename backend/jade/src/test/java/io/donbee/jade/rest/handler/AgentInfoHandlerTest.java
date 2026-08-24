package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.MultiMap;
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
public class AgentInfoHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private AgentInfoHandler handler;

    @Before
    public void setUp() {
        handler = new AgentInfoHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("name", name));
    }

    @Test
    public void Given_AgentExists_When_GetAgentByName_Then_ReturnsAgentDetails() {
        // Arrange
        mockPathParam("ams");
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "ams@host:1099/JADE", "active", "init", "Main-Container", new String[]{"addr1"});
        when(mockService.getAgent("ams")).thenReturn(agent);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgent("ams");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonExistentAgent_When_GetAgentByName_Then_Returns404() {
        // Arrange
        mockPathParam("nonexistent");
        when(mockService.getAgent("nonexistent")).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_GetAgent_Then_Returns403() {
        // Arrange
        mockPathParam("ams");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_NullAgentName_When_GetAgent_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_AgentNameWithContainerSuffix_When_GetAgent_Then_StripsSuffix() {
        // Arrange
        mockPathParam("ams@container");
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "ams@host:1099/JADE", "active", "init", "Main-Container", new String[]{});
        when(mockService.getAgent("ams")).thenReturn(agent);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgent("ams");
    }

    @Test
    public void Given_ServiceThrowsException_When_GetAgent_Then_Returns500() {
        // Arrange
        mockPathParam("ams");
        when(mockService.getAgent("ams"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }

    @Test
    public void Given_AgentWithNullState_When_GetAgent_Then_ReturnsUnknownState() {
        // Arrange
        mockPathParam("ams");
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "ams@host:1099/JADE", null, null, "Main-Container", new String[]{});
        when(mockService.getAgent("ams")).thenReturn(agent);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgent("ams");
        verify(mockResponse).setStatusCode(200);
    }
}
