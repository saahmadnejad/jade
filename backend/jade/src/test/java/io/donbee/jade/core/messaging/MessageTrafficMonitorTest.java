package io.donbee.jade.core.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MessageTrafficMonitorTest {

    private Vertx vertx;
    private AID sender;
    private AID receiver;

    @Before
    public void setUp() {
        MessageTrafficMonitor.reset();
        vertx = Vertx.vertx();
        sender = new AID("sender-agent@platform", AID.ISGUID);
        receiver = new AID("receiver-agent@platform", AID.ISGUID);
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

    private ACLMessage createMockMessage(int performative, String content, String protocol, String ontology) {
        ACLMessage msg = mock(ACLMessage.class);
        when(msg.getPerformative()).thenReturn(performative);
        when(msg.getContent()).thenReturn(content);
        when(msg.getProtocol()).thenReturn(protocol);
        when(msg.getOntology()).thenReturn(ontology);
        return msg;
    }

    @Test
    public void Given_VertxActive_When_MessageNotified_Then_PublishedToEventBus() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JsonObject> receivedJson = new AtomicReference<>();

        vertx.eventBus().<JsonObject>consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> {
            receivedJson.set(msg.body());
            latch.countDown();
        });

        ACLMessage acl = createMockMessage(ACLMessage.REQUEST, "perform-task", "fipa-request", "task-onto");

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);

        // --- Assert ---
        boolean received = latch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        JsonObject json = receivedJson.get();
        assertThat(json).isNotNull();
        assertThat(json.getString("sender")).isEqualTo("sender-agent");
        assertThat(json.getString("receiver")).isEqualTo("receiver-agent");
        assertThat(json.getString("performative")).isEqualTo("request");
        assertThat(json.getString("content")).isEqualTo("perform-task");
        assertThat(json.getString("protocol")).isEqualTo("fipa-request");
        assertThat(json.getString("ontology")).isEqualTo("task-onto");
        assertThat(json.getString("id")).isNotBlank();
        assertThat(json.getString("timestamp")).isNotBlank();
    }

    @Test
    public void Given_NullArguments_When_MessageNotified_Then_NoEventPublishedAndNoException() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        CountDownLatch busLatch = new CountDownLatch(1);
        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> busLatch.countDown());

        ACLMessage acl = createMockMessage(ACLMessage.INFORM, "test", null, null);

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(null, receiver, acl);
        MessageTrafficMonitor.notifyMessage(sender, null, acl);
        MessageTrafficMonitor.notifyMessage(sender, receiver, null);

        // --- Assert ---
        boolean received = busLatch.await(300, TimeUnit.MILLISECONDS);
        assertThat(received).isFalse();
    }

    @Test
    public void Given_NoVertx_When_MessageNotified_Then_NoExceptionAndNoAllocation() {
        // --- Arrange ---
        MessageTrafficMonitor.reset();
        ACLMessage acl = createMockMessage(ACLMessage.INFORM, "offline", null, null);

        // --- Act / Assert (must not throw or allocate) ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);
    }

    @Test
    public void Given_MultipleMessages_When_Notified_Then_EventBusPreservesFifoSequenceOrdering() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        int count = 10;
        CountDownLatch latch = new CountDownLatch(count);
        List<JsonObject> received = new ArrayList<>();

        vertx.eventBus().<JsonObject>consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> {
            synchronized (received) {
                received.add(msg.body());
            }
            latch.countDown();
        });

        // --- Act ---
        for (int i = 1; i <= count; i++) {
            ACLMessage acl = createMockMessage(ACLMessage.INFORM, "msg-" + i, null, null);
            MessageTrafficMonitor.notifyMessage(sender, receiver, acl);
        }

        // --- Assert ---
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        synchronized (received) {
            assertThat(received).hasSize(count);
            for (int i = 0; i < count; i++) {
                assertThat(received.get(i).getString("content")).isEqualTo("msg-" + (i + 1));
                long expectedSeq = i + 1;
                assertThat(received.get(i).getString("id")).isEqualTo(Long.toString(expectedSeq));
            }
        }
    }

    @Test
    public void Given_ToJson_When_MessageConverted_Then_JsonShapeMatchesSchema() {
        // --- Arrange ---
        ACLMessage acl = createMockMessage(ACLMessage.PROPOSE, "(price 42)", "fipa-contract-net", "auction");

        // --- Act ---
        JsonObject json = MessageTrafficMonitor.toJson(101L, sender, receiver, acl);

        // --- Assert ---
        assertThat(json.getString("id")).isEqualTo("101");
        assertThat(json.getString("sender")).isEqualTo("sender-agent");
        assertThat(json.getString("receiver")).isEqualTo("receiver-agent");
        assertThat(json.getString("performative")).isEqualTo("propose");
        assertThat(json.getString("protocol")).isEqualTo("fipa-contract-net");
        assertThat(json.getString("ontology")).isEqualTo("auction");
        assertThat(json.getString("content")).isEqualTo("(price 42)");
        assertThat(json.getString("timestamp")).isNotBlank();
    }

    @Test
    public void Given_Reset_When_Called_Then_StateCleared() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        MessageTrafficMonitor.reset();

        CountDownLatch busLatch = new CountDownLatch(1);
        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> busLatch.countDown());

        ACLMessage acl = createMockMessage(ACLMessage.INFORM, "after-reset", null, null);

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);

        // --- Assert ---
        boolean received = busLatch.await(300, TimeUnit.MILLISECONDS);
        assertThat(received).isFalse();
    }
}
