package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;

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
public class DFRegistrationHandlerTest {

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

    private void mockPathParam(String name) {
        when(mockContext.pathParams()).thenReturn(Map.of("agentName", name));
    }

    private DFRegistrationInfo sampleRegistration(String name) {
        List<DFServiceInfo> services = new ArrayList<>();
        services.add(new DFServiceInfo("weather-forecast", "weather-service", ""));
        return new DFRegistrationInfo(name, new ArrayList<>(), services, "");
    }

    // ===== LIST mode =====

    @Test
    public void Given_RegistrationsExist_When_ListRegistrations_Then_Returns200WithList() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.LIST);
        List<DFRegistrationInfo> regs = new ArrayList<>();
        regs.add(sampleRegistration("agent1@host"));
        when(mockService.listDFRegistrations()).thenReturn(regs);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).listDFRegistrations();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_EmptyDF_When_ListRegistrations_Then_Returns200WithEmptyList() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.LIST);
        when(mockService.listDFRegistrations()).thenReturn(new ArrayList<>());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).listDFRegistrations();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    // ===== REGISTER mode =====

    @Test
    public void Given_ValidRequest_When_RegisterAgent_Then_Returns201WithRegistration() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.REGISTER);
        JsonObject body = new JsonObject()
            .put("agentName", "test-agent@host");
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.registerWithDF(eq("test-agent@host"), anyList(), anyList()))
            .thenReturn(sampleRegistration("test-agent@host"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).registerWithDF(eq("test-agent@host"), anyList(), anyList());
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_MissingAgentName_When_RegisterAgent_Then_Returns400() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.REGISTER);
        when(mockBody.asJsonObject()).thenReturn(new JsonObject());

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(400), any(Throwable.class));
        verify(mockService, never()).registerWithDF(anyString(), anyList(), anyList());
    }

    // ===== VIEW mode =====

    @Test
    public void Given_ExistingRegistration_When_ViewRegistration_Then_Returns200() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.VIEW);
        mockPathParam("test-agent@host");
        when(mockService.getDFRegistration("test-agent@host"))
            .thenReturn(sampleRegistration("test-agent@host"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFRegistration("test-agent@host");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonExistentRegistration_When_ViewRegistration_Then_Returns404() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.VIEW);
        mockPathParam("nonexistent@host");
        when(mockService.getDFRegistration("nonexistent@host"))
            .thenThrow(new IllegalArgumentException("Agent not registered with DF: nonexistent@host"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    // ===== DEREGISTER mode =====

    @Test
    public void Given_ValidAgentName_When_Deregister_Then_Returns200() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.DEREGISTER);
        mockPathParam("test-agent@host");
        doNothing().when(mockService).deregisterFromDF("test-agent@host");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).deregisterFromDF("test-agent@host");
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonExistentAgent_When_Deregister_Then_Returns404() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.DEREGISTER);
        mockPathParam("nonexistent@host");
        doThrow(new IllegalArgumentException("Not registered"))
            .when(mockService).deregisterFromDF("nonexistent@host");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(404), any(Throwable.class));
    }

    @Test
    public void Given_NonMainContainer_When_Deregister_Then_Returns403() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.DEREGISTER);
        mockPathParam("test-agent@host");
        doThrow(new IllegalStateException("Not a Main Container"))
            .when(mockService).deregisterFromDF("test-agent@host");

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    // ===== MODIFY mode =====

    @Test
    public void Given_ValidRequest_When_ModifyRegistration_Then_Returns200WithRegistration() {
        // Arrange
        DFRegistrationHandler handler = new DFRegistrationHandler(mockService, DFRegistrationHandler.Mode.MODIFY);
        mockPathParam("test-agent@host");
        JsonObject body = new JsonObject();
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockService.modifyDFRegistration(eq("test-agent@host"), anyList(), anyList()))
            .thenReturn(sampleRegistration("test-agent@host"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).modifyDFRegistration(eq("test-agent@host"), anyList(), anyList());
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }
}
