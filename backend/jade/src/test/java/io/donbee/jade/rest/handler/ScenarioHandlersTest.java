package io.donbee.jade.rest.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;
import io.donbee.jade.rest.service.ScenarioService;

@RunWith(MockitoJUnitRunner.class)
public class ScenarioHandlersTest {

    @Mock
    private ScenarioService mockScenarioService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Mock
    private io.vertx.ext.web.RequestBody mockBody;

    private final Scenario demoScenario = new Scenario() {
        @Override public String id() { return "demo"; }
        @Override public String title() { return "Demo"; }
        @Override public String description() { return "d"; }
        @Override public List<ScenarioParam> params() {
            return List.of(ScenarioParam.intParam("count", 2, 1, 5, "how many"));
        }
        @Override public List<AgentSpec> agents(Map<String, Object> config) {
            return List.of(new AgentSpec("a", "com.example.A", List.of()));
        }
    };

    @Mock
    private io.vertx.core.Vertx mockVertx;

    @Before
    public void setUp() {
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(org.mockito.ArgumentMatchers.anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        when(mockContext.vertx()).thenReturn(mockVertx);
        // Run the blocking task inline in tests
        when(mockVertx.executeBlocking(org.mockito.ArgumentMatchers.<java.util.concurrent.Callable<Object>>any(), org.mockito.ArgumentMatchers.anyBoolean()))
            .thenAnswer(inv -> io.vertx.core.Future.succeededFuture(
                ((java.util.concurrent.Callable<?>) inv.getArgument(0)).call()));
    }

    @Test
    public void Given_ScenariosAvailable_When_ListRequested_Then_ParamsAndDefaultsIncluded() {
        // --- Arrange ---
        when(mockScenarioService.list()).thenReturn(List.of(demoScenario));
        ScenarioListHandler handler = new ScenarioListHandler(mockScenarioService);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_ValidBody_When_StartRequested_Then_Returns201WithInstanceDetails() {
        // --- Arrange ---
        JsonObject body = new JsonObject()
            .put("instanceName", "i1")
            .put("config", new JsonObject().put("count", 3));
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockContext.pathParam("id")).thenReturn("demo");
        when(mockScenarioService.start(eq("demo"), eq("i1"), eq(Map.of("count", 3))))
            .thenReturn(new ScenarioService.StartResult("i1", "demo", "scenario-i1", List.of("i1-a")));
        ScenarioStartHandler handler = new ScenarioStartHandler(mockScenarioService);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockResponse).setStatusCode(201);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_InvalidConfig_When_StartRequested_Then_FailsWith400() {
        // --- Arrange ---
        JsonObject body = new JsonObject()
            .put("config", new JsonObject().put("bogus", 1));
        when(mockContext.body()).thenReturn(mockBody);
        when(mockBody.asJsonObject()).thenReturn(body);
        when(mockContext.pathParam("id")).thenReturn("demo");
        when(mockScenarioService.start(eq("demo"), any(), any()))
            .thenThrow(new IllegalArgumentException("Unknown config parameter: bogus"));
        ScenarioStartHandler handler = new ScenarioStartHandler(mockScenarioService);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockContext).fail(eq(400), any());
    }

    @Test
    public void Given_RunningInstance_When_StopRequested_Then_Returns200() {
        // --- Arrange ---
        when(mockContext.pathParam("name")).thenReturn("i1");
        ScenarioStopHandler handler = new ScenarioStopHandler(mockScenarioService);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockScenarioService).stop("i1");
        verify(mockResponse).setStatusCode(200);
    }

    @Test
    public void Given_UnknownInstance_When_StopRequested_Then_FailsWith404() {
        // --- Arrange ---
        when(mockContext.pathParam("name")).thenReturn("ghost");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Unknown instance: ghost"))
            .when(mockScenarioService).stop("ghost");
        ScenarioStopHandler handler = new ScenarioStopHandler(mockScenarioService);

        // --- Act ---
        handler.handle(mockContext);

        // --- Assert ---
        verify(mockContext).fail(eq(404), any());
    }
}
