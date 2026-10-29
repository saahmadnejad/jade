package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    public void Given_RegisteredService_When_MessageDispatched_Then_BufferedWithJsonShape() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();
        service.start();

        // --- Act ---
        service.onMessage(sender, receiver, message(ACLMessage.REQUEST, "(buy sku-1 1)"));

        // --- Assert ---
        List<JsonObject> recent = service.recent(0, null, null);
        assertThat(recent).hasSize(1);
        JsonObject json = recent.get(0);
        assertThat(json.getString("sender")).isEqualTo("seller");
        assertThat(json.getString("receiver")).isEqualTo("warehouse");
        assertThat(json.getString("performative")).isEqualTo("request");
        assertThat(json.getString("protocol")).isEqualTo("fipa-request");
        assertThat(json.getString("ontology")).isEqualTo("devteam-ontology");
        assertThat(json.getString("content")).isEqualTo("(buy sku-1 1)");
        assertThat(json.getString("timestamp")).isNotBlank();
        assertThat(json.getString("id")).isEqualTo("1");

        service.stop();
    }

    @Test
    public void Given_CapacityExceeded_When_MessagesCaptured_Then_OldestEvictedAndDroppedCounted() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService(3);

        // --- Act ---
        for (int i = 0; i < 5; i++) {
            service.onMessage(sender, receiver, message(ACLMessage.INFORM, "msg-" + i));
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

        // --- Act ---
        service.onMessage(sender, receiver, message(ACLMessage.INFORM, longContent));
        List<JsonObject> recent = service.recent(10, null, null);
        JsonObject full = service.getById(recent.get(0).getString("id"));

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
    public void Given_Subscriber_When_MessageCaptured_Then_ReceivesLiveFrameAndUnsubscribeStopsDelivery() {
        // --- Arrange ---
        MessageTrafficService service = new MessageTrafficService();
        AtomicInteger received = new AtomicInteger();
        Consumer<JsonObject> subscriber = msg -> received.incrementAndGet();
        Runnable unsubscribe = service.subscribe(subscriber);

        // --- Act ---
        service.onMessage(sender, receiver, message(ACLMessage.REQUEST, "live"));
        unsubscribe.run();
        service.onMessage(sender, receiver, message(ACLMessage.REQUEST, "after"));

        // --- Assert ---
        assertThat(received.get()).isEqualTo(1);
    }

    @Test
    public void Given_VertxEventBus_When_MessagePublished_Then_CapturedByServiceAndNotifiesSubscribers() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);

        CountDownLatch subscriberLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> subscriberReceived = new AtomicReference<>();
        service.subscribe(msg -> {
            subscriberReceived.set(msg);
            subscriberLatch.countDown();
        });

        JsonObject published = new JsonObject()
            .put("id", "bus-1")
            .put("sender", "agent-a")
            .put("receiver", "agent-b")
            .put("performative", "inform")
            .put("protocol", "fipa-request")
            .put("ontology", "test")
            .put("content", "hello from eventbus")
            .put("timestamp", "2026-10-29T12:00:00Z");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, published);

        // --- Assert ---
        boolean received = subscriberLatch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        JsonObject fromSubscriber = subscriberReceived.get();
        assertThat(fromSubscriber).isNotNull();
        assertThat(fromSubscriber.getString("id")).isEqualTo("bus-1");
        assertThat(fromSubscriber.getString("content")).isEqualTo("hello from eventbus");

        // Verify buffer also holds the message
        List<JsonObject> recent = service.recent(10, null, null);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getString("id")).isEqualTo("bus-1");

        // Verify getById
        JsonObject full = service.getById("bus-1");
        assertThat(full).isNotNull();
        assertThat(full.getString("content")).isEqualTo("hello from eventbus");

        service.stop();
    }

    @Test
    public void Given_VertxEventBus_When_ServiceStopped_Then_NoLongerCaptures() throws Exception {
        // --- Arrange ---
        vertx = Vertx.vertx();
        MessageTrafficService service = new MessageTrafficService();
        service.start(vertx);
        service.stop();

        CountDownLatch subscriberLatch = new CountDownLatch(1);
        service.subscribe(msg -> subscriberLatch.countDown());

        JsonObject published = new JsonObject()
            .put("id", "bus-stopped")
            .put("sender", "agent-a")
            .put("receiver", "agent-b")
            .put("performative", "inform")
            .put("content", "should not be captured");

        // --- Act ---
        vertx.eventBus().publish(MessageTrafficMonitor.EVENT_BUS_ADDRESS, published);

        // --- Assert ---
        boolean received = subscriberLatch.await(300, TimeUnit.MILLISECONDS);
        assertThat(received).isFalse();
        assertThat(service.recent(10, null, null)).isEmpty();
    }
}
