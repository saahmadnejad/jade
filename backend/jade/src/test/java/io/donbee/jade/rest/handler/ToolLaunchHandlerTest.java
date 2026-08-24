package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.rest.service.PlatformService.AgentInfo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ToolLaunchHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    private ToolLaunchHandler handler;

    @Before
    public void setUp() {
        handler = new ToolLaunchHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockService.isMainContainer()).thenReturn(true);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        doAnswer(inv -> mockResponse).when(mockResponse).end(any(Buffer.class));
    }

    private void mockPathParam(String toolName) {
        Map<String, String> params = new HashMap<>();
        params.put("tool", toolName);
        when(mockContext.pathParams()).thenReturn(params);
    }

    private void mockBody(String container) {
        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject();
        json.put("container", container);
        when(mockBody.asJsonObject()).thenReturn(json);
        when(mockContext.body()).thenReturn(mockBody);
    }

    private void mockBodyNoContainer() {
        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject();
        when(mockBody.asJsonObject()).thenReturn(json);
        when(mockContext.body()).thenReturn(mockBody);
    }

    private void mockNullBody() {
        when(mockContext.body()).thenReturn(null);
    }

    @Test
    public void Given_ValidToolName_When_LaunchSniffer_Then_DeploysAgentAndReturns201() {
        // Arrange
        mockPathParam("sniffer");
        mockBody("Main-Container");
        AgentInfo info = new AgentInfo("sniffer-agent", "active", "jade", "Main-Container", new String[]{});
        when(mockService.deployAgent("sniffer-agent", "io.donbee.jade.tools.sniffer.Sniffer", new Object[]{}))
            .thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("sniffer-agent", "io.donbee.jade.tools.sniffer.Sniffer", new Object[]{});
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ValidToolName_When_LaunchDummy_Then_DeploysAgentAndReturns201() {
        // Arrange
        mockPathParam("dummy");
        mockBodyNoContainer();
        AgentInfo info = new AgentInfo("dummy-agent", "active", "jade", "Main-Container", new String[]{});
        when(mockService.deployAgent("dummy-agent", "io.donbee.jade.tools.DummyAgent.DummyAgent", new Object[]{}))
            .thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("dummy-agent", "io.donbee.jade.tools.DummyAgent.DummyAgent", new Object[]{});
        verify(mockResponse).setStatusCode(201);
    }

    @Test
    public void Given_ValidToolName_When_LaunchLogger_Then_DeploysAgentAndReturns201() {
        // Arrange
        mockPathParam("logger");
        mockNullBody();
        AgentInfo info = new AgentInfo("logger-agent", "active", "jade", "Main-Container", new String[]{});
        when(mockService.deployAgent("logger-agent", "io.donbee.jade.tools.logging.LoggerAgent", new Object[]{}))
            .thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("logger-agent", "io.donbee.jade.tools.logging.LoggerAgent", new Object[]{});
        verify(mockResponse).setStatusCode(201);
    }

    @Test
    public void Given_ValidToolName_When_LaunchIntrospector_Then_DeploysAgentAndReturns201() {
        // Arrange
        mockPathParam("introspector");
        mockBodyNoContainer();
        AgentInfo info = new AgentInfo("introspector-agent", "active", "jade", "Main-Container", new String[]{});
        when(mockService.deployAgent("introspector-agent", "io.donbee.jade.tools.introspector.Introspector", new Object[]{}))
            .thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("introspector-agent", "io.donbee.jade.tools.introspector.Introspector", new Object[]{});
        verify(mockResponse).setStatusCode(201);
    }

    @Test
    public void Given_ValidToolName_When_LaunchDfGui_Then_DeploysAgentAndReturns201() {
        // Arrange
        mockPathParam("df-gui");
        mockBodyNoContainer();
        AgentInfo info = new AgentInfo("df-gui-agent", "active", "jade", "Main-Container", new String[]{});
        when(mockService.deployAgent("df-gui-agent", "io.donbee.jade.tools.df.DFTool", new Object[]{}))
            .thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deployAgent("df-gui-agent", "io.donbee.jade.tools.df.DFTool", new Object[]{});
        verify(mockResponse).setStatusCode(201);
    }

    @Test
    public void Given_UnknownToolName_When_LaunchTool_Then_FailsWith400() {
        // Arrange
        mockPathParam("unknown-tool");
        mockBody("Main-Container");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).deployAgent(anyString(), anyString(), any(Object[].class));
    }

    @Test
    public void Given_NullToolName_When_LaunchTool_Then_FailsWith400() {
        // Arrange
        when(mockContext.pathParams()).thenReturn(new HashMap<>());
        mockBody("Main-Container");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_LaunchTool_Then_FailsWith403() {
        // Arrange
        mockPathParam("sniffer");
        mockBody("Main-Container");
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
        verify(mockService, never()).deployAgent(anyString(), anyString(), any(Object[].class));
    }

    @Test
    public void Given_DeploymentFailsWithIllegalArgumentException_When_LaunchTool_Then_FailsWith404() {
        // Arrange
        mockPathParam("sniffer");
        mockBody("Main-Container");
        when(mockService.deployAgent(anyString(), anyString(), any(Object[].class)))
            .thenThrow(new IllegalArgumentException("Tool class not found"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_UnexpectedError_When_LaunchTool_Then_FailsWith500() {
        // Arrange
        mockPathParam("sniffer");
        mockBody("Main-Container");
        when(mockService.deployAgent(anyString(), anyString(), any(Object[].class)))
            .thenThrow(new RuntimeException("Connection refused"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
