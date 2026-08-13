package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/containers/:name/save} — save a container
 * to a repository.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.SaveContainerAction} invoked
 * {@code rma.saveContainer(name, "JADE-DB")} ({@code rma.java:669}).
 * The old action hardcoded the repository as {@code "JADE-DB"}; this
 * handler allows the caller to specify an arbitrary repository. Both
 * send a {@link io.donbee.jade.domain.persistence.SaveContainer}
 * action via the {@code PersistenceOntology} to the AMS.</p>
 */
public class ContainerSaveHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerSaveHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String containerName = ctx.pathParams().get("name");
        if (containerName == null || containerName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container name"));
            return;
        }
        JsonObject body = ctx.body().asJsonObject();
        String repository = body != null ? body.getString("repository") : null;
        if (repository == null || repository.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing repository"));
            return;
        }
        try {
            service.saveContainer(containerName, repository);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Container '" + containerName + "' saved to " + repository).toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, new RuntimeException("Save failed: " + e.getMessage()));
        }
    }
}
