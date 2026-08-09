package io.donbee.jade.rest;

import io.donbee.jade.core.Runtime;
import io.donbee.jade.core.Profile;
import io.donbee.jade.core.ProfileImpl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RestAPIIntegrationTest {

    private static Vertx vertx;
    private static WebClient client;
    private static Runtime runtime;
    private static final int REST_PORT = 19080;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN, "true");
        profile.setParameter(Profile.MAIN_PORT, "19099");
        profile.setParameter(Profile.GUI, "true");
        profile.setParameter(Profile.REST_PORT, String.valueOf(REST_PORT));

        runtime = Runtime.instance();
        io.donbee.jade.wrapper.AgentContainer container = runtime.createMainContainer(profile);

        vertx = Vertx.vertx();
        RestAPIVerticle verticle = new RestAPIVerticle(container, REST_PORT);
        vertx.deployVerticle(verticle).onComplete(context.asyncAssertSuccess(ar -> {}));

        client = WebClient.create(vertx);

        Thread.sleep(3000);
    }

    @AfterClass
    public static void tearDown(TestContext context) {
        if (vertx != null) {
            vertx.close().onComplete(context.asyncAssertSuccess(ar -> {}));
        }
        if (runtime != null) {
            runtime.shutDown();
        }
    }

    // ===== Health =====

    @Test
    public void Given_RestApiStarted_When_HealthEndpointCalled_Then_Returns200(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/health")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertEquals("ok", body.getString("status"));
                async.complete();
            }));
    }

    // ===== Version =====

    @Test
    public void Given_RestApiStarted_When_VersionEndpointCalled_Then_ReturnsVersionInfo(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/version")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertTrue(body.containsKey("version"));
                async.complete();
            }));
    }

    // ===== Platform Info =====

    @Test
    public void Given_MainContainerRunning_When_PlatformInfoRequested_Then_ReturnsPlatformData(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/platform")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertTrue(body.containsKey("platformID"));
                context.assertTrue(body.containsKey("ams"));
                context.assertTrue(body.containsKey("defaultDF"));
                async.complete();
            }));
    }

    // ===== Agents List (basic) =====

    @Test
    public void Given_MainContainerRunning_When_AgentsListRequestedWithoutDetail_Then_ReturnsAgentNamesOnly(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/agents")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertTrue(body.getJsonArray("agents").size() > 0);
                async.complete();
            }));
    }

    // ===== Agents List (with detail) =====

    @Test
    public void Given_MainContainerRunning_When_AgentsListRequestedWithDetail_Then_ReturnsAgentsWithState(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/agents?detail=true")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertTrue(body.getJsonArray("agents").size() > 0);
                async.complete();
            }));
    }

    // ===== Containers =====

    @Test
    public void Given_MainContainerRunning_When_ContainersListRequested_Then_ReturnsContainers(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.get(REST_PORT, "localhost", "/api/containers")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                context.assertTrue(body.getJsonArray("containers").size() > 0);
                async.complete();
            }));
    }

    // ===== Suspend + Resume Existing Agent =====

    @Test
    public void Given_AgentExists_When_SuspendThenResume_Then_AgentIsSuspendedAndResumed(TestContext context) {
        // Arrange
        Async suspendAsync = context.async();
        Async resumeAsync = context.async();

        // Act - Suspend (df is always present in main container)
        client.post(REST_PORT, "localhost", "/api/agents/df/suspend")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                context.assertTrue(response.bodyAsJsonObject().getString("message").contains("suspended"));
                suspendAsync.complete();
            }));

        // Act - Resume
        client.post(REST_PORT, "localhost", "/api/agents/df/resume")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(200, response.statusCode());
                context.assertTrue(response.bodyAsJsonObject().getString("message").contains("resumed"));
                resumeAsync.complete();
            }));
    }

    // ===== Error: Suspend Non-Existent Agent =====

    @Test
    public void Given_NonExistentAgent_When_SuspendAgent_Then_Returns404(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.post(REST_PORT, "localhost", "/api/agents/nonexistent-agent-abc123/suspend")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(404, response.statusCode());
                async.complete();
            }));
    }

    // ===== Error: Delete Non-Existent Agent =====

    @Test
    public void Given_NonExistentAgent_When_DeleteAgent_Then_Returns404(TestContext context) {
        // Arrange
        Async async = context.async();

        // Act
        client.delete(REST_PORT, "localhost", "/api/agents/nonexistent-agent-xyz789")
            .send()
            .onComplete(context.asyncAssertSuccess(response -> {
                // Assert
                context.assertEquals(404, response.statusCode());
                async.complete();
            }));
    }
}
