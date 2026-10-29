package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.rest.service.PlatformService.AgentInfo;
import io.donbee.jade.rest.service.PlatformService.ContainerTreeNode;
import io.donbee.jade.rest.service.PlatformService.PlatformTree;
import io.donbee.jade.rest.service.PlatformService.RemotePlatformInfo;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlatformTreeHandlerTest {

    @Mock
    private PlatformService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private Vertx mockVertx;

    private PlatformTreeHandler handler;

    @Before
    public void setUp() {
        handler = new PlatformTreeHandler(mockService);
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        when(mockContext.vertx()).thenReturn(mockVertx);

        when(mockVertx.executeBlocking(any(Callable.class)))
            .thenAnswer(inv -> {
                Callable<?> callable = inv.getArgument(0);
                return Future.succeededFuture(callable.call());
            });
    }

    @Test
    public void Given_NotMainContainer_When_GetPlatformTree_Then_FailsWith403() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(false);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_PlatformTreeAvailable_When_GetPlatformTree_Then_Returns200WithTree() {
        // Arrange
        when(mockService.isMainContainer()).thenReturn(true);
        AgentInfo agent = new AgentInfo("ams", "ACTIVE", "init", "Main-Container", new String[]{"addr1"});
        ContainerTreeNode container = new ContainerTreeNode("Main-Container", "127.0.0.1", "1099", true, List.of(agent));
        RemotePlatformInfo remote = new RemotePlatformInfo("remote1", "ams@remote1", new String[]{"addr2"}, new String[]{"FIPA"});
        PlatformTree tree = new PlatformTree("jade-main", List.of(container), List.of(remote));
        when(mockService.getPlatformTree()).thenReturn(tree);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getPlatformTree();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }
}
