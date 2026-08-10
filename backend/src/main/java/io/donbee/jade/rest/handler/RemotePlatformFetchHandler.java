package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for POST /api/platforms/fetch — add a remote platform by URL.
 */
public class RemotePlatformFetchHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformFetchHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        JsonObject body = ctx.body().asJsonObject();
        if (body == null || !body.containsKey("url")) {
            ctx.fail(400, new RuntimeException("Missing url in request body"));
            return;
        }
        String url = body.getString("url");
        try {
            PlatformService.RemotePlatformInfo platform = service.fetchRemotePlatform(url);
            JsonObject json = new JsonObject()
                .put("message", "Remote platform added")
                .put("name", platform.name)
                .put("ams", platform.ams)
                .put("addresses", new JsonArray(java.util.Arrays.asList(platform.addresses)));
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(json.toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
