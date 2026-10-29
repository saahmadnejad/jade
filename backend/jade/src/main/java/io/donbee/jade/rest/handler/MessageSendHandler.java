package io.donbee.jade.rest.handler;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.rest.service.PlatformService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for the message-send endpoint ({@code POST /api/messages} and {@code POST /api/messages/send}).
 * Allows composing and injecting an ACL message into the JADE platform via REST.
 *
 * <p><b>Old GUI implementation:</b>
 * In the legacy Swing GUI, message composition was handled by
 * {@code io.donbee.jade.gui.AclGui} used inside
 * {@code io.donbee.jade.tools.DummyAgent.DummyAgent} and
 * {@code io.donbee.jade.tools.testagent.TestAgent} (via {@code ACLPanel}).
 * The user selected performatives, recipients, content, and protocol slots,
 * then clicked "Send" which called {@code Agent.send(ACLMessage)}.
 * This handler replaces those legacy Swing components with a REST API endpoint
 * backed by {@code PlatformService#sendMessage(ACLMessage)}.</p>
 */
public class MessageSendHandler implements Handler<RoutingContext> {

    private final PlatformService service;

    public MessageSendHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }

        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "Request body must be a valid JSON object").put("code", 400).toBuffer());
            return;
        }

        // Parse performative
        int perf = parsePerformative(body.getValue("performative"));
        if (perf < 0 || perf > 21) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "Invalid or missing performative").put("code", 400).toBuffer());
            return;
        }

        // Parse receivers
        JsonArray receivers = body.getJsonArray("receivers");
        if (receivers == null || receivers.isEmpty()) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "At least one receiver is required").put("code", 400).toBuffer());
            return;
        }

        ACLMessage msg = new ACLMessage(perf);

        for (int i = 0; i < receivers.size(); i++) {
            String r = receivers.getString(i);
            if (r != null && !r.trim().isEmpty()) {
                msg.addReceiver(new AID(r.trim(), AID.ISGUID));
            }
        }

        // Parse sender
        String senderStr = body.getString("sender");
        if (senderStr != null && !senderStr.trim().isEmpty()) {
            msg.setSender(new AID(senderStr.trim(), AID.ISGUID));
        }

        // Parse reply-to
        JsonArray replyTo = body.getJsonArray("replyTo");
        if (replyTo != null) {
            for (int i = 0; i < replyTo.size(); i++) {
                String rt = replyTo.getString(i);
                if (rt != null && !rt.trim().isEmpty()) {
                    msg.addReplyTo(new AID(rt.trim(), AID.ISGUID));
                }
            }
        }

        // Content
        if (body.containsKey("content") && body.getString("content") != null) {
            msg.setContent(body.getString("content"));
        }

        // Protocol slots
        if (body.containsKey("language") && body.getString("language") != null) {
            msg.setLanguage(body.getString("language"));
        }
        if (body.containsKey("ontology") && body.getString("ontology") != null) {
            msg.setOntology(body.getString("ontology"));
        }
        if (body.containsKey("protocol") && body.getString("protocol") != null) {
            msg.setProtocol(body.getString("protocol"));
        }
        if (body.containsKey("conversationId") && body.getString("conversationId") != null) {
            msg.setConversationId(body.getString("conversationId"));
        }
        if (body.containsKey("replyWith") && body.getString("replyWith") != null) {
            msg.setReplyWith(body.getString("replyWith"));
        }
        if (body.containsKey("inReplyTo") && body.getString("inReplyTo") != null) {
            msg.setInReplyTo(body.getString("inReplyTo"));
        }

        // User properties
        JsonObject userProps = body.getJsonObject("userProperties");
        if (userProps != null) {
            for (String key : userProps.fieldNames()) {
                String val = userProps.getString(key);
                if (val != null) {
                    msg.addUserDefinedParameter(key, val);
                }
            }
        }

        ctx.vertx().executeBlocking(() -> {
            service.sendMessage(msg);
            return null;
        }).onSuccess(v -> {
            JsonObject response = new JsonObject()
                .put("message", "Message sent")
                .put("performative", ACLMessage.getPerformative(perf))
                .put("sender", msg.getSender() != null ? msg.getSender().getName() : "")
                .put("receivers", receivers);

            if (msg.getConversationId() != null) {
                response.put("conversationId", msg.getConversationId());
            }

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(response.toBuffer());
        }).onFailure(err -> ctx.fail(500, err));
    }

    private int parsePerformative(Object val) {
        if (val == null) {
            return -1;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            String str = ((String) val).trim().toUpperCase();
            int perf = ACLMessage.getInteger(str);
            if (perf != -1) {
                return perf;
            }
            // Try with hyphen if underscore was used, or vice versa
            perf = ACLMessage.getInteger(str.replace('_', '-'));
            if (perf != -1) {
                return perf;
            }
            return ACLMessage.getInteger(str.replace('-', '_'));
        }
        return -1;
    }
}
