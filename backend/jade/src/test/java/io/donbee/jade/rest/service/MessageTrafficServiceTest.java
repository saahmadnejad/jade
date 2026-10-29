package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MessageTrafficServiceTest {

    private Vertx vertx;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws Exception {
        MessageTrafficMonitor.reset();
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(ar -> latch.countDown());
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void Given_DirectIngestion_When_MessageIngested_Then_BufferedWithJsonShape() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();

        JsonObject json = new JsonObject()
            .put("id", "1")
            .put("sender", "seller")
            .put("receiver", "warehouse")
            .put("performative", "request")
            .put("protocol", "fipa-request")
            .put("ontology", "devteam-ontology")
            .put("content", "(buy sku-1 1)")
            .put("timestamp", "2026-10-29T12:00:00Z");

        // --- Act ---
        service.onMessage(json);

        // --- Assert ---
        List<JsonObject> recent = service.recent(0, null, null);
        assertThat(recent).hasSize(1);
        JsonObject buffered = recent.get(0);
        assertThat(buffered.getString("sender")).isEqualTo("seller");
        assertThat(buffered.getString("receiver")).isEqualTo("warehouse");
        assertThat(buffered.getString("performative")).isEqualTo("request");
        assertThat(buffered.getString("protocol")).isEqualTo("fipa-request");
        assertThat(buffered.getString("ontology")).isEqualTo("devteam-ontology");
        assertThat(buffered.getString("content")).isEqualTo("(buy sku-1 1)");
        assertThat(buffered.getString("id")).isEqualTo("1");
    }

    @Test
    public void Given_CapacityExceeded_When_MessagesCaptured_Then_OldestEvictedAndDroppedCounted() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService(3);

        // --- Act ---
        for (int i = 0; i < 5; i++) {
            JsonObject json = new JsonObject()
                .put("id", Integer.toString(i))
                .put("content", "msg-" + i);
            service.onMessage(json);
        }

        // --- Assert ---
        List<JsonObject> recent = service.recent(10, null, null);
        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).getString("content")).isEqualTo("msg-2");
        assertThat(recent.get(2).getString("content")).isEqualTo("msg-4");
        assertThat(service.getDroppedCount()).isEqualTo(2);
    }

    @Test
    public void Given_FilterParams_When_RecentQueried_Then_OnlyMatchingMessagesReturned() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();
        service.onMessage(new JsonObject().put("id", "1").put("sender", "seller").put("receiver", "warehouse").put("content", "a"));
        service.onMessage(new JsonObject().put("id", "2").put("sender", "warehouse").put("receiver", "seller").put("content", "b"));
        service.onMessage(new JsonObject().put("id", "3").put("sender", "seller").put("receiver", "restock").put("content", "c"));

        // --- Act ---
        List<JsonObject> fromSeller = service.recent(0, "seller", null);
        List<JsonObject> toWarehouse = service.recent(0, null, "warehouse");

        // --- Assert ---
        assertThat(fromSeller).hasSize(2);
        assertThat(fromSeller).allMatch(m -> m.getString("sender").equals("seller"));
        assertThat(toWarehouse).hasSize(1);
        assertThat(toWarehouse.get(0).getString("content")).isEqualTo("a");
    }

    @Test
    public void Given_LongContent_When_Captured_Then_BufferKeepsFullAndRecentTruncates() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();
        String longContent = "x".repeat(1000);

        JsonObject json = new JsonObject()
            .put("id", "long-1")
            .put("sender", "sender")
            .put("receiver", "receiver")
            .put("content", longContent);

        // --- Act ---
        service.onMessage(json);
        List<JsonObject> recent = service.recent(10, null, null);
        JsonObject full = service.getById("long-1");

        // --- Assert ---
        assertThat(recent.get(0).getString("content")).hasSize(256 + 3).endsWith("...");
        assertThat(full).isNotNull();
        assertThat(full.getString("content")).isEqualTo(longContent); // detail view: untruncated
    }

    @Test
    public void Given_UnknownId_When_GetById_Then_Null() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();

        // --- Act / Assert ---
        assertThat(service.getById("999")).isNull();
    }

    @Test
    public void Given_VertxEventBus_When_MessagePublished_Then_CapturedByService() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> latch.countDown());

        String longContent = "y".repeat(300);
        JsonObject published = new JsonObject()
            .put("id", "bus-1")
            .put("sender", "agent-a")
            .put("receiver", "agent-b")
            .put("performative", "inform")
            .put("protocol", "fipa-request")
            .put("ontology", "test")
            .put("content", longContent)
            .put("timestamp", "2026-10-29T12:00:00Z");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, published);

        // --- Assert ---
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        // Buffer must retain UNTRUNCATED content (for GET /api/messages/:id)
        JsonObject full = service.getById("bus-1");
        assertThat(full).isNotNull();
        assertThat(full.getString("content")).isEqualTo(longContent);

        // Recent list must truncate (for GET /api/messages/recent)
        List<JsonObject> recent = service.recent(10, null, null);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getString("content")).hasSize(256 + 3).endsWith("...");

        service.stop();
    }

    @Test
    public void Given_VertxEventBus_When_ServiceStopped_Then_NoLongerCaptures() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);
        service.stop();

        CountDownLatch latch = new CountDownLatch(1);
        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> latch.countDown());

        JsonObject published = new JsonObject()
            .put("id", "bus-stopped")
            .put("sender", "agent-a")
            .put("receiver", "agent-b")
            .put("performative", "inform")
            .put("content", "should not be captured");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, published);

        // --- Assert ---
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(service.recent(10, null, null)).isEmpty();
    }
}
