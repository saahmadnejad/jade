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
public class RemotePlatformAgentsHandlerTest {

    @Mock
    private PlatformService mockService;
    @Mock
    private RoutingContext mockContext;
    @Mock
    private HttpServerResponse mockResponse;

    private RemotePlatformAgentsHandler handler;

    @Before
    public void setUp() {
        handler = new RemotePlatformAgentsHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_ExistingPlatform_When_SearchAgents_Then_Returns200WithAgentArray() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of("name", "jade-main"));
        List<PlatformService.AgentInfo> agents = new ArrayList<>();
        agents.add(new PlatformService.AgentInfo("rma@jade-main", "ACTIVE", null, "Main-Container", new String[]{}));
        when(mockService.searchRemotePlatformAgents("jade-main")).thenReturn(agents);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).searchRemotePlatformAgents("jade-main");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_SearchAgents_Then_Returns403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
        verify(mockService, never()).searchRemotePlatformAgents(anyString());
    }

    @Test
    public void Given_MissingPlatformName_When_SearchAgents_Then_Returns400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(Map.of());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }
}
