package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for GET/PUT /api/platforms/:name/description — get or refresh a remote platform description.
 */
public class RemotePlatformDescriptionHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformDescriptionHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String platformName = ctx.pathParams().get("name");
        if (platformName == null || platformName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing platform name"));
            return;
        }
        try {
            PlatformService.RemotePlatformInfo platform =
                service.getRemotePlatformDescription(platformName);
            JsonObject json = new JsonObject()
                .put("name", platform.name)
                .put("ams", platform.ams)
                .put("addresses", new JsonArray(java.util.Arrays.asList(platform.addresses)))
                .put("services", new JsonArray(java.util.Arrays.asList(platform.services)));
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(json.toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
