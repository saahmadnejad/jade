package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.donbee.jade.lang.acl.ACLMessage;

public class MessageTrafficServiceTest {

    private AID sender;
    private AID receiver;
    private Vertx vertx;

    @Before
    public void setUp() {
        sender = new AID("seller@platform", AID.ISGUID);
        receiver = new AID("warehouse@platform", AID.ISGUID);
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

    private ACLMessage message(int performative, String content) {
        ACLMessage msg = mock(ACLMessage.class);
        when(msg.getPerformative()).thenReturn(performative);
        when(msg.getContent()).thenReturn(content);
        when(msg.getProtocol()).thenReturn("fipa-request");
        when(msg.getOntology()).thenReturn("devteam-ontology");
        return msg;
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
        service.onMessage(sender, receiver, message(ACLMessage.REQUEST, "a"));
        service.onMessage(receiver, sender, message(ACLMessage.AGREE, "b"));
        service.onMessage(sender, new AID("restock@platform", AID.ISGUID), message(ACLMessage.INFORM, "c"));

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
    public void Given_VertxEventBus_When_MessagePublished_Then_CapturedByServiceAndStreamPublished() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);

        CountDownLatch streamLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> streamReceived = new AtomicReference<>();
        vertx.eventBus().<JsonObject>consumer(MessageTrafficMonitor.EVENT_BUS_STREAM_ADDRESS, msg -> {
            streamReceived.set(msg.body());
            streamLatch.countDown();
        });

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
        boolean received = streamLatch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        // Stream consumer must receive TRUNCATED content (per messages-api.md spec)
        JsonObject fromStream = streamReceived.get();
        assertThat(fromStream).isNotNull();
        assertThat(fromStream.getString("id")).isEqualTo("bus-1");
        assertThat(fromStream.getString("content")).hasSize(256 + 3).endsWith("...");

        // Buffer must retain UNTRUNCATED content (for GET /api/messages/:id)
        JsonObject full = service.getById("bus-1");
        assertThat(full).isNotNull();
        assertThat(full.getString("content")).isEqualTo(longContent);

        service.stop();
    }

    @Test
    public void Given_VertxEventBus_When_ServiceStopped_Then_NoLongerCaptures() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);
        service.stop();

        CountDownLatch streamLatch = new CountDownLatch(1);
        vertx.eventBus().<JsonObject>consumer(MessageTrafficMonitor.EVENT_BUS_STREAM_ADDRESS, msg -> streamLatch.countDown());

        JsonObject published = new JsonObject()
            .put("id", "bus-stopped")
            .put("sender", "agent-a")
            .put("receiver", "agent-b")
            .put("performative", "inform")
            .put("content", "should not be captured");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, published);

        // --- Assert ---
        boolean received = streamLatch.await(300, TimeUnit.MILLISECONDS);
        assertThat(received).isFalse();
        assertThat(service.recent(10, null, null)).isEmpty();
    }
}
