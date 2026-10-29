package io.donbee.jade.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.donbee.jade.core.behaviours.Behaviour;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.OneShotBehaviour;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;
import io.donbee.jade.wrapper.AgentContainer;
import io.donbee.jade.wrapper.AgentController;
import io.donbee.jade.wrapper.ContainerController;

public class AgentVerticleTest {

    private static final AtomicInteger portCounter = new AtomicInteger(13100);
    private Runtime runtime;
    private AgentContainer mainContainer;

    @Before
    public void setUp() {
        runtime = Runtime.instance();
        ProfileImpl p = new ProfileImpl(true);
        p.setParameter(Profile.MAIN, "true");
        p.setParameter(Profile.MAIN_PORT, String.valueOf(portCounter.getAndIncrement()));
        p.setParameter(Profile.NO_MTP, "true");
        p.setParameter(Profile.AGENT_RUNTIME, Profile.AGENT_RUNTIME_VERTICLE);
        mainContainer = runtime.createMainContainer(p);
    }

    @After
    public void tearDown() {
        if (mainContainer != null) {
            try {
                mainContainer.kill();
            } catch (Exception ignored) {}
        }
        if (runtime != null) {
            runtime.shutDown();
        }
    }

    public static class LifecycleTestAgent extends Agent {
        static final AtomicBoolean setupRan = new AtomicBoolean(false);
        static final AtomicBoolean takeDownRan = new AtomicBoolean(false);

        @Override
        protected void setup() {
            setupRan.set(true);
        }

        @Override
        protected void takeDown() {
            takeDownRan.set(true);
        }
    }

    @Test
    public void Given_VerticleRuntime_When_AgentDeployedAndKilled_Then_LifecycleExecutesCleanly() throws Exception {
        LifecycleTestAgent.setupRan.set(false);
        LifecycleTestAgent.takeDownRan.set(false);

        AgentController ac = mainContainer.createNewAgent(
                "lifecycle-agent", LifecycleTestAgent.class.getName(), new Object[0]);
        ac.start();

        long deadline = System.currentTimeMillis() + 5000;
        while (!LifecycleTestAgent.setupRan.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(LifecycleTestAgent.setupRan.get()).isTrue();

        ac.kill();

        deadline = System.currentTimeMillis() + 5000;
        while (!LifecycleTestAgent.takeDownRan.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(LifecycleTestAgent.takeDownRan.get()).isTrue();
    }

    public static class FifoReceiverAgent extends Agent {
        static final List<Integer> receivedSeqs = Collections.synchronizedList(new ArrayList<>());
        static final CountDownLatch allReceivedLatch = new CountDownLatch(10);

        @Override
        protected void setup() {
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        try {
                            int seq = Integer.parseInt(msg.getContent());
                            receivedSeqs.add(seq);
                            allReceivedLatch.countDown();
                        } catch (NumberFormatException ignored) {}
                    } else {
                        block();
                    }
                }
            });
        }
    }

    public static class SenderAgent extends Agent {
        @Override
        protected void setup() {
            Object[] args = getArguments();
            if (args != null && args.length >= 2) {
                String target = (String) args[0];
                int count = (Integer) args[1];
                addBehaviour(new OneShotBehaviour() {
                    @Override
                    public void action() {
                        for (int i = 1; i <= count; i++) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(new AID(target, AID.ISLOCALNAME));
                            msg.setContent(String.valueOf(i));
                            send(msg);
                        }
                    }
                });
            }
        }
    }

    @Test
    public void Given_VerticleRuntime_When_MessagesDispatched_Then_FifoOrderingPreserved() throws Exception {
        FifoReceiverAgent.receivedSeqs.clear();

        AgentController receiver = mainContainer.createNewAgent(
                "fifo-receiver", FifoReceiverAgent.class.getName(), new Object[0]);
        receiver.start();
        Thread.sleep(300);

        AgentController sender = mainContainer.createNewAgent(
                "fifo-sender", SenderAgent.class.getName(), new Object[]{"fifo-receiver", 10});
        sender.start();

        boolean done = FifoReceiverAgent.allReceivedLatch.await(5, TimeUnit.SECONDS);
        assertThat(done).as("All 10 messages received within 5s").isTrue();
        assertThat(FifoReceiverAgent.receivedSeqs)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public static class FairnessTestAgent extends Agent {
        static final AtomicInteger countA = new AtomicInteger(0);
        static final AtomicInteger countB = new AtomicInteger(0);
        static final CountDownLatch bothAdvancedLatch = new CountDownLatch(2);

        @Override
        protected void setup() {
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    if (countA.incrementAndGet() == 5) {
                        bothAdvancedLatch.countDown();
                    }
                }
            });
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    if (countB.incrementAndGet() == 5) {
                        bothAdvancedLatch.countDown();
                    }
                }
            });
        }
    }

    @Test
    public void Given_MultipleBehaviours_When_Draining_Then_FairnessIsMaintained() throws Exception {
        FairnessTestAgent.countA.set(0);
        FairnessTestAgent.countB.set(0);

        AgentController ac = mainContainer.createNewAgent(
                "fairness-agent", FairnessTestAgent.class.getName(), new Object[0]);
        ac.start();

        boolean done = FairnessTestAgent.bothAdvancedLatch.await(5, TimeUnit.SECONDS);
        assertThat(done).as("Both cyclic behaviours advance in drain task").isTrue();
        assertThat(FairnessTestAgent.countA.get()).isGreaterThanOrEqualTo(5);
        assertThat(FairnessTestAgent.countB.get()).isGreaterThanOrEqualTo(5);
        ac.kill();
    }

    public static class BlockingReceiveAgent extends Agent {
        static final CountDownLatch receivedLatch = new CountDownLatch(1);
        static volatile String receivedContent = null;

        @Override
        protected void setup() {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 5000);
                    if (msg != null) {
                        receivedContent = msg.getContent();
                        receivedLatch.countDown();
                    }
                }
            });
        }
    }

    public static class WakerSenderAgent extends Agent {
        @Override
        protected void setup() {
            Object[] args = getArguments();
            if (args != null && args.length >= 2) {
                String target = (String) args[0];
                String content = (String) args[1];
                addBehaviour(new OneShotBehaviour() {
                    @Override
                    public void action() {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(new AID(target, AID.ISLOCALNAME));
                        msg.setContent(content);
                        send(msg);
                    }
                });
            }
        }
    }

    @Test
    public void Given_VerticleRuntime_When_BlockingReceiveUsed_Then_ParksAndWakesCleanly() throws Exception {
        BlockingReceiveAgent.receivedContent = null;

        AgentController ac = mainContainer.createNewAgent(
                "blocking-agent", BlockingReceiveAgent.class.getName(), new Object[0]);
        ac.start();
        Thread.sleep(300);

        AgentController waker = mainContainer.createNewAgent(
                "waker-agent", WakerSenderAgent.class.getName(), new Object[]{"blocking-agent", "wake-up-payload"});
        waker.start();

        boolean received = BlockingReceiveAgent.receivedLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(BlockingReceiveAgent.receivedContent).isEqualTo("wake-up-payload");
        ac.kill();
        waker.kill();
    }

    public static class AbortTestAgent extends Agent {
        static final AtomicBoolean takeDownRan = new AtomicBoolean(false);

        @Override
        protected void setup() {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 30_000);
                }
            });
        }

        @Override
        protected void takeDown() {
            takeDownRan.set(true);
        }
    }

    @Test
    public void Given_BlockedAgent_When_Killed_Then_AbortsPromptlyAndTakesDown() throws Exception {
        AbortTestAgent.takeDownRan.set(false);

        AgentController ac = mainContainer.createNewAgent(
                "abort-agent", AbortTestAgent.class.getName(), new Object[0]);
        ac.start();
        Thread.sleep(300);

        long start = System.currentTimeMillis();
        ac.kill();

        long deadline = System.currentTimeMillis() + 5000;
        while (!AbortTestAgent.takeDownRan.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        long duration = System.currentTimeMillis() - start;

        assertThat(AbortTestAgent.takeDownRan.get()).as("takeDown executed").isTrue();
        assertThat(duration).as("Aborted well before the 30s timeout").isLessThan(5000);
    }

    public static class IdleTestAgent extends Agent {
        static final AtomicBoolean takeDownRan = new AtomicBoolean(false);

        @Override
        protected void setup() {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    // completes immediately -> becomes idle
                }
            });
        }

        @Override
        protected void takeDown() {
            takeDownRan.set(true);
        }
    }

    @Test
    public void Given_IdleAgent_When_Deleted_Then_TakeDownExecutesPromptly() throws Exception {
        IdleTestAgent.takeDownRan.set(false);

        AgentController ac = mainContainer.createNewAgent(
                "idle-agent", IdleTestAgent.class.getName(), new Object[0]);
        ac.start();
        Thread.sleep(300);

        ac.kill();

        long deadline = System.currentTimeMillis() + 5000;
        while (!IdleTestAgent.takeDownRan.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(IdleTestAgent.takeDownRan.get()).as("takeDown executes on idle agent").isTrue();
    }

    public static class PendingInterruptAgent extends Agent {
        static final AtomicBoolean interruptedObserved = new AtomicBoolean(false);
        static final CountDownLatch readyForInterruptLatch = new CountDownLatch(1);
        static final CountDownLatch step2FinishedLatch = new CountDownLatch(1);

        @Override
        protected void setup() {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    readyForInterruptLatch.countDown();
                }
            });
        }

        public void scheduleSecondStep() {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    interruptedObserved.set(Thread.currentThread().isInterrupted());
                    step2FinishedLatch.countDown();
                }
            });
        }
    }

    @Test
    public void Given_PendingInterrupt_When_DeliveredBetweenSteps_Then_InterruptPreservedOnNextStep() throws Exception {
        PendingInterruptAgent.interruptedObserved.set(false);

        AgentController ac = mainContainer.createNewAgent(
                "pending-interrupt-agent", PendingInterruptAgent.class.getName(), new Object[0]);
        ac.start();

        boolean ready = PendingInterruptAgent.readyForInterruptLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();
        Thread.sleep(100);

        Field f = ContainerController.class.getDeclaredField("myImpl");
        f.setAccessible(true);
        AgentContainerImpl impl = (AgentContainerImpl) f.get(mainContainer);
        PendingInterruptAgent agentInstance = (PendingInterruptAgent) impl.acquireLocalAgent(new AID("pending-interrupt-agent", AID.ISLOCALNAME));
        impl.releaseLocalAgent(agentInstance.getAID());

        // Interrupt when agent is idle (myThread == null) -> sets pendingInterrupt flag
        agentInstance.interruptThread();

        // Schedule next step -> pendingInterrupt must be applied
        agentInstance.scheduleSecondStep();

        boolean done = PendingInterruptAgent.step2FinishedLatch.await(5, TimeUnit.SECONDS);
        assertThat(done).isTrue();
        assertThat(PendingInterruptAgent.interruptedObserved.get()).as("Interrupt was preserved and applied to step 2").isTrue();
        ac.kill();
    }

    public static class ChurnWorkerAgent extends Agent {
        static final AtomicInteger totalSetup = new AtomicInteger(0);
        static final AtomicInteger totalTakeDown = new AtomicInteger(0);

        @Override
        protected void setup() {
            totalSetup.incrementAndGet();
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    // Small work
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(getAID());
                    msg.setContent("self-ping");
                    send(msg);
                }
            });
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        // Received self-ping, finished work
                        block();
                    } else {
                        block();
                    }
                }
            });
        }

        @Override
        protected void takeDown() {
            totalTakeDown.incrementAndGet();
        }
    }

    @Test
    public void Given_HighThroughputAgentChurn_When_ConcurrentUpsAndDowns_Then_ZeroLeaksAndCleanExecution() throws Exception {
        ChurnWorkerAgent.totalSetup.set(0);
        ChurnWorkerAgent.totalTakeDown.set(0);

        Field f = ContainerController.class.getDeclaredField("myImpl");
        f.setAccessible(true);
        AgentContainerImpl impl = (AgentContainerImpl) f.get(mainContainer);

        int waves = 5;
        int agentsPerWave = 10;
        int totalAgents = waves * agentsPerWave;

        for (int w = 0; w < waves; w++) {
            List<AgentController> waveControllers = new ArrayList<>();
            // Rapidly spin up wave
            for (int i = 0; i < agentsPerWave; i++) {
                String name = "churn-w" + w + "-a" + i;
                AgentController ac = mainContainer.createNewAgent(
                        name, ChurnWorkerAgent.class.getName(), new Object[0]);
                ac.start();
                waveControllers.add(ac);
            }

            // Short execution window
            Thread.sleep(150);

            // Rapidly tear down wave
            for (AgentController ac : waveControllers) {
                ac.kill();
            }
        }

        // Wait for all takeDown calls to complete
        long deadline = System.currentTimeMillis() + 10_000;
        while (ChurnWorkerAgent.totalTakeDown.get() < totalAgents && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertThat(ChurnWorkerAgent.totalSetup.get())
                .as("All agents executed setup()")
                .isEqualTo(totalAgents);
        assertThat(ChurnWorkerAgent.totalTakeDown.get())
                .as("All agents executed takeDown()")
                .isEqualTo(totalAgents);

        // Verify zero leaked churn verticle deployments in memory (only the 2 system agents: ams and df remain)
        Thread.sleep(300);
        assertThat(impl.getVerticleDeploymentCount())
                .as("All 50 churn verticle deployments cleaned up; only AMS and DF remain")
                .isEqualTo(2);
    }
}
