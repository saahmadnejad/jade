package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.donbee.jade.lang.acl.ACLMessage;

public class MessageTrafficServiceTest {

    private AID sender;
    private AID receiver;

    @Before
    public void setUp() {
        sender = new AID("shop@platform", AID.ISGUID);
        receiver = new AID("inventory@platform", AID.ISGUID);
    }

    private ACLMessage message(int performative, String content) {
        ACLMessage msg = mock(ACLMessage.class);
        when(msg.getPerformative()).thenReturn(performative);
        when(msg.getContent()).thenReturn(content);
        when(msg.getProtocol()).thenReturn("fipa-request");
        when(msg.getOntology()).thenReturn("shop-ontology");
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
        assertThat(json.getString("sender")).isEqualTo("shop");
        assertThat(json.getString("receiver")).isEqualTo("inventory");
        assertThat(json.getString("performative")).isEqualTo("request");
        assertThat(json.getString("protocol")).isEqualTo("fipa-request");
        assertThat(json.getString("ontology")).isEqualTo("shop-ontology");
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
        List<JsonObject> fromShop = service.recent(0, "shop", null);
        List<JsonObject> toInventory = service.recent(0, null, "inventory");

        // --- Assert ---
        assertThat(fromShop).hasSize(2);
        assertThat(fromShop).allMatch(m -> m.getString("sender").equals("shop"));
        assertThat(toInventory).hasSize(1);
        assertThat(toInventory.get(0).getString("content")).isEqualTo("a");
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
}
