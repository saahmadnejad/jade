package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFParentInfo;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DFederationHandlerTest {

    @Mock
    private DFService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private RequestBody mockBody;

    @Before
    public void setUp() {
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockContext.body()).thenReturn(mockBody);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        doAnswer(inv -> mockResponse).when(mockResponse).end(any(Buffer.class));
    }

    private DFParentInfo sampleParent(String name) {
        List<String> addrs = new ArrayList<>();
        addrs.add("jades://10.0.0.1:1099/jade-df");
        return new DFParentInfo(name, addrs);
    }

    // ===== PARENTS =====

    @Test
    public void Given_ParentsExist_When_GetParents_Then_Returns200WithParents() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.PARENTS);
        List<DFParentInfo> parents = new ArrayList<>();
        parents.add(sampleParent("parent-df@parent-platform"));
        when(mockService.getDFParents()).thenReturn(parents);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFParents();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NoParents_When_GetParents_Then_Returns200WithEmptyList() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.PARENTS);
        when(mockService.getDFParents()).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFParents();
        verify(mockResponse).setStatusCode(200);
    }

    @Test
    public void Given_NonMainContainer_When_GetParents_Then_Returns403() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.PARENTS);
        when(mockService.getDFParents()).thenThrow(new IllegalStateException("Not a Main Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    // ===== CHILDREN =====

    @Test
    public void Given_ChildrenExist_When_GetChildren_Then_Returns200WithChildren() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.CHILDREN);
        List<DFParentInfo> children = new ArrayList<>();
        children.add(sampleParent("child-df@child-platform"));
        when(mockService.getDFChildren()).thenReturn(children);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFChildren();
        verify(mockResponse).setStatusCode(200);
    }

    @Test
    public void Given_NoChildren_When_GetChildren_Then_Returns200WithEmptyList() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.CHILDREN);
        when(mockService.getDFChildren()).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFChildren();
        verify(mockResponse).setStatusCode(200);
    }

    // ===== FEDERATE =====

    @Test
    public void Given_ValidParent_When_Federate_Then_Returns200WithMessage() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.FEDERATE);
        JsonObject body = new JsonObject().put("parentDF", "parent-df@parent-platform");
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.federateDF(eq("parent-df@parent-platform"), anyList()))
            .thenReturn(sampleParent("parent-df@parent-platform"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).federateDF(eq("parent-df@parent-platform"), anyList());
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingParentDF_When_Federate_Then_Returns400() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.FEDERATE);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).federateDF(anyString(), anyList());
    }

    @Test
    public void Given_NoBody_When_Federate_Then_Returns400() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.FEDERATE);
        when(mockBody.asJsonObject()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
    }

    @Test
    public void Given_SelfFederation_When_Federate_Then_Returns400() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.FEDERATE);
        JsonObject body = new JsonObject().put("parentDF", "df@Main-Container");
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.federateDF(anyString(), anyList()))
            .thenThrow(new IllegalArgumentException("Self-federation not allowed"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    // ===== DEREGISTER PARENT =====

    @Test
    public void Given_ValidParentName_When_DeregisterParent_Then_Returns200() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.DEREGISTER_PARENT);
        when(mockContext.pathParams()).thenReturn(Map.of("parentDFName", "parent-df@parent-platform"));
        doNothing().when(mockService).deregisterParentDF("parent-df@parent-platform");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deregisterParentDF("parent-df@parent-platform");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingParentName_When_DeregisterParent_Then_Returns400() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.DEREGISTER_PARENT);
        when(mockContext.pathParams()).thenReturn(null);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).deregisterParentDF(anyString());
    }

    // ===== DEREGISTER CHILD =====

    @Test
    public void Given_ValidChildName_When_DeregisterChild_Then_Returns200() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.DEREGISTER_CHILD);
        when(mockContext.pathParams()).thenReturn(Map.of("childDFName", "child-df@child-platform"));
        doNothing().when(mockService).deregisterChildDF("child-df@child-platform");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deregisterChildDF("child-df@child-platform");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonExistentChild_When_DeregisterChild_Then_Returns404() {
        // Arrange
        DFederationHandler handler = new DFederationHandler(mockService, DFederationHandler.Mode.DEREGISTER_CHILD);
        when(mockContext.pathParams()).thenReturn(Map.of("childDFName", "unknown@host"));
        doThrow(new IllegalArgumentException("Agent not registered with DF: unknown@host"))
            .when(mockService).deregisterChildDF("unknown@host");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }
}
