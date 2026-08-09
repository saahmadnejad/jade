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

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentListHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private AgentListHandler handler;

    @Before
    public void setUp() {
        handler = new AgentListHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    private void mockQueryParams(String detail) {
        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        if (detail != null) {
            params.add("detail", detail);
        }
        when(mockContext.queryParams()).thenReturn(params);
    }

    // ===== Health / basic list =====

    @Test
    public void Given_NoDetailParam_When_GetAgentList_Then_ReturnsAgentNamesOnly() {
        // Arrange
        mockQueryParams(null);
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "test-agent@host", "active", "owner", "Main-Container", new String[]{"addr1"});
        when(mockService.getAgents(false))
            .thenReturn(java.util.List.of(agent));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgents(false);
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    // ===== With detail =====

    @Test
    public void Given_DetailTrue_When_GetAgentList_Then_ReturnsAgentDetails() {
        // Arrange
        mockQueryParams("true");
        PlatformService.AgentInfo agent = new PlatformService.AgentInfo(
            "test-agent@host", "active", "owner", "Main-Container", new String[]{"addr1"});
        when(mockService.getAgents(true))
            .thenReturn(java.util.List.of(agent));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgents(true);
    }

    // ===== Empty list =====

    @Test
    public void Given_EmptyAgentList_When_GetAgents_Then_ReturnsEmptyArray() {
        // Arrange
        mockQueryParams(null);
        when(mockService.getAgents(false)).thenReturn(java.util.List.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getAgents(false);
        verify(mockResponse).setStatusCode(200);
    }

    // ===== Service error =====

    @Test
    public void Given_ServiceThrowsException_When_GetAgentList_Then_FailsWith500() {
        // Arrange
        mockQueryParams(null);
        when(mockService.getAgents(false))
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }

    // ===== Non-main container =====

    @Test
    public void Given_NonMainContainer_When_GetAgentList_Then_FailsWith403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);
        mockQueryParams(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }
}
