package io.donbee.jade.core.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public void Given_LegacyListener_When_MessageNotified_Then_LegacyListenerReceives() throws Exception {
        // --- Arrange ---
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ACLMessage> receivedMsg = new AtomicReference<>();

        MessageTrafficListener listener = (s, r, m) -> {
            receivedMsg.set(m);
            latch.countDown();
        };
        MessageTrafficMonitor.addListener(listener);

        ACLMessage acl = createMockMessage(ACLMessage.INFORM, "status-ok", null, null);

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);

        // --- Assert ---
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedMsg.get()).isSameAs(acl);

        MessageTrafficMonitor.removeListener(listener);
    }

    @Test
    public void Given_DualWrite_When_MessageNotified_Then_BothEventBusAndLegacyListenerReceive() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        CountDownLatch busLatch = new CountDownLatch(1);
        CountDownLatch listenerLatch = new CountDownLatch(1);

        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> busLatch.countDown());

        MessageTrafficListener listener = (s, r, m) -> listenerLatch.countDown();
        MessageTrafficMonitor.addListener(listener);

        ACLMessage acl = createMockMessage(ACLMessage.QUERY_IF, "is-ready", null, null);

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);

        // --- Assert ---
        assertThat(busLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(listenerLatch.await(2, TimeUnit.SECONDS)).isTrue();

        MessageTrafficMonitor.removeListener(listener);
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
    public void Given_FaultyLegacyListener_When_MessageNotified_Then_FailureIsolatedAndOtherReceiversSucceed() throws Exception {
        // --- Arrange ---
        MessageTrafficMonitor.init(vertx);
        CountDownLatch busLatch = new CountDownLatch(1);
        CountDownLatch healthyListenerLatch = new CountDownLatch(1);

        vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> busLatch.countDown());

        MessageTrafficListener faultyListener = (s, r, m) -> {
            throw new RuntimeException("Simulated listener crash");
        };
        MessageTrafficListener healthyListener = (s, r, m) -> healthyListenerLatch.countDown();

        MessageTrafficMonitor.addListener(faultyListener);
        MessageTrafficMonitor.addListener(healthyListener);

        ACLMessage acl = createMockMessage(ACLMessage.AGREE, "agreed", null, null);

        // --- Act ---
        MessageTrafficMonitor.notifyMessage(sender, receiver, acl);

        // --- Assert ---
        assertThat(busLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(healthyListenerLatch.await(2, TimeUnit.SECONDS)).isTrue();

        MessageTrafficMonitor.removeListener(faultyListener);
        MessageTrafficMonitor.removeListener(healthyListener);
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
