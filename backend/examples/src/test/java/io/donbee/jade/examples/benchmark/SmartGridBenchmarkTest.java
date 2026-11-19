package io.donbee.jade.examples.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.donbee.jade.core.Profile;
import io.donbee.jade.core.ProfileImpl;
import io.donbee.jade.core.Runtime;
import io.donbee.jade.examples.smartgrid.SmartGridScenario;
import io.donbee.jade.examples.smartgrid.SubstationAgent;
import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.wrapper.AgentController;
import io.donbee.jade.wrapper.ContainerController;

/**
 * Side-by-side performance benchmark and memory-leak verification test comparing
 * the virtual thread runtime (default JADE) against the Vert.x verticle runtime (ADR-0004)
 * under identical Smart Grid Microgrid agent workloads.
 */
public class SmartGridBenchmarkTest {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(SmartGridBenchmarkTest.class.getName());
    private static Runtime runtime;
    private static ContainerController mainContainer;

    public record RuntimeMetrics(
        String runtimeName,
        int agentCount,
        int auctionsHeld,
        int proposalsReceived,
        int acceptedBids,
        double totalKwhTraded,
        long startupDurationMs,
        long simulationDurationMs,
        long teardownDurationMs,
        int peakThreads,
        double initialHeapMb,
        double peakHeapMb,
        double postTeardownHeapMb,
        double memoryRetainedMb
    ) {}

    @BeforeClass
    public static void bootPlatform() throws Exception {
        runtime = Runtime.instance();
        ProfileImpl profile = new ProfileImpl(true);
        profile.setParameter(Profile.MAIN, "true");
        profile.setParameter(Profile.MAIN_PORT, "13999"); // isolated test port
        profile.setParameter(Profile.GUI, "false");
        mainContainer = runtime.createMainContainer(profile);
    }

    @AfterClass
    public static void tearDownPlatform() throws Exception {
        SubstationAgent.clearMetrics();
        if (mainContainer != null) {
            try {
                mainContainer.kill();
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void Given_HighDensityMicrogrid_When_ExecutedUnderBothRuntimes_Then_CompareScalability() throws Exception {
        int prosumers = 50;
        int consumers = 50;
        int batteries = 5;
        int maxRounds = 4;
        int tickMs = 150;

        runComparison(prosumers, consumers, batteries, maxRounds, tickMs, "High Density Microgrid Workload (106 agents)");
    }

    @Test
    public void Given_1000IdleAgents_When_ComparedAcrossRuntimes_Then_CompareMemoryFootprint() throws Exception {
        int prosumers = 500;
        int consumers = 475;
        int batteries = 24;

        // 1. Benchmark Virtual Thread Runtime
        RuntimeMetrics threadMetrics = runIdleWorkload("thread", "idle-th-" + System.currentTimeMillis(),
            prosumers, consumers, batteries);

        // Allow GC stabilization between runs
        forceGc();
        Thread.sleep(1000);

        // 2. Benchmark Vert.x Verticle Runtime
        RuntimeMetrics verticleMetrics = runIdleWorkload("verticle", "idle-vt-" + System.currentTimeMillis(),
            prosumers, consumers, batteries);

        // Print comparative Markdown table
        printComparisonTable("1,000 Idle Agents Footprint (ADR-0004 & Issue #17)", threadMetrics, verticleMetrics);

        // Assertions
        assertThat(verticleMetrics.agentCount()).isGreaterThanOrEqualTo(1000);
        assertThat(threadMetrics.agentCount()).isGreaterThanOrEqualTo(1000);

        // Comparative footprint verification: Verticle runtime executes on bounded thread pool
        assertThat(verticleMetrics.peakThreads())
            .as("Verticle runtime should execute agents on bounded thread pool")
            .isLessThan(500);

        // Memory leak check: Retained heap after teardown and GC must be bounded (< 250 MB drift)
        assertThat(verticleMetrics.memoryRetainedMb())
            .as("Verticle runtime should not leak heap memory after container kill")
            .isLessThan(250.0);
    }

    private void runComparison(int prosumers, int consumers, int batteries,
                               int maxRounds, int tickMs, String workloadTitle) throws Exception {
        // 1. Benchmark Virtual Thread Runtime
        RuntimeMetrics threadMetrics = runWorkload("thread", "bench-th-" + System.currentTimeMillis(),
            prosumers, consumers, batteries, maxRounds, tickMs);

        // Allow GC stabilization between runs
        forceGc();
        Thread.sleep(1000);

        // 2. Benchmark Vert.x Verticle Runtime
        RuntimeMetrics verticleMetrics = runWorkload("verticle", "bench-vt-" + System.currentTimeMillis(),
            prosumers, consumers, batteries, maxRounds, tickMs);

        // Print comparative Markdown table
        printComparisonTable(workloadTitle, threadMetrics, verticleMetrics);

        // --- Assertions ---
        assertThat(verticleMetrics.auctionsHeld()).isGreaterThanOrEqualTo(1);

        if (verticleMetrics.acceptedBids() > 0) {
            assertThat(verticleMetrics.totalKwhTraded()).isGreaterThan(0.0);
        }

        if (threadMetrics.auctionsHeld() > 0 && threadMetrics.acceptedBids() > 0) {
            assertThat(threadMetrics.totalKwhTraded()).isGreaterThan(0.0);
        }

        // Comparative footprint verification: Verticle runtime dispatches cooperative steps on pooled threads,
        // so active carrier/OS threads remain bounded regardless of agent density
        assertThat(verticleMetrics.peakThreads())
            .as("Verticle runtime should execute agents on bounded thread pool")
            .isLessThan(300);

        // Memory leak check: Retained heap after teardown and GC must be bounded (< 250 MB drift)
        assertThat(verticleMetrics.memoryRetainedMb())
            .as("Verticle runtime should not leak heap memory after container kill")
            .isLessThan(250.0);
    }

    private RuntimeMetrics runWorkload(String runtimeMode, String containerName,
                                       int prosumers, int consumers, int batteries,
                                       int maxRounds, int tickIntervalMs) throws Exception {
        forceGc();
        double initialHeapMb = getUsedHeapMb();

        SmartGridScenario scenario = new SmartGridScenario();
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", prosumers);
        config.put("consumersCount", consumers);
        config.put("batteriesCount", batteries);
        config.put("stormProbability", "1.0"); // deterministic storms to exercise actions
        config.put("tickIntervalMs", tickIntervalMs);
        config.put("maxRounds", maxRounds);

        List<AgentSpec> specs = scenario.agents(config);
        String gridId = String.valueOf(specs.get(0).getArgs().get(0));

        ProfileImpl subProfile = new ProfileImpl(false);
        subProfile.setParameter(Profile.CONTAINER_NAME, containerName);
        subProfile.setParameter(Profile.MAIN_PORT, "13999");
        subProfile.setParameter(Profile.AGENT_RUNTIME, runtimeMode);
        ContainerController container = runtime.createAgentContainer(subProfile);

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int initialThreads = threadBean.getThreadCount();
        int maxObservedThreads = initialThreads;
        double peakHeapMb = initialHeapMb;

        // Startup timing
        long startupStart = System.currentTimeMillis();
        for (AgentSpec spec : specs) {
            Object[] args = spec.getArgs().toArray(new Object[0]);
            String agentName = containerName + "-" + spec.getNameSuffix();
            AgentController ac = container.createNewAgent(agentName, spec.getClassName(), args);
            ac.start();
        }
        long startupDurationMs = System.currentTimeMillis() - startupStart;

        // Simulation execution timing and resource sampling
        long simStart = System.currentTimeMillis();
        long simDeadline = simStart + Math.max(10000L, ((long) maxRounds * tickIntervalMs * 4L));
        while (System.currentTimeMillis() < simDeadline) {
            SubstationAgent.Metrics stats = SubstationAgent.getMetrics(gridId);
            if (stats != null && stats.getCompletedRounds() >= maxRounds) {
                Thread.sleep(300); // allow round delivery confirmations to settle
                break;
            }
            int currentThreads = threadBean.getThreadCount();
            if (currentThreads > maxObservedThreads) {
                maxObservedThreads = currentThreads;
            }
            double currentHeap = getUsedHeapMb();
            if (currentHeap > peakHeapMb) {
                peakHeapMb = currentHeap;
            }
            Thread.sleep(50);
        }
        long simDurationMs = System.currentTimeMillis() - simStart;

        // Collect simulation stats
        SubstationAgent.Metrics stats = SubstationAgent.getMetrics(gridId);
        int auctions = stats != null ? stats.getAuctionsHeld() : 0;
        int proposals = stats != null ? stats.getProposalsReceived() : 0;
        int bids = stats != null ? stats.getAcceptedBids() : 0;
        double tradedKwh = stats != null ? stats.getTotalKwhTraded() : 0.0;

        // Teardown timing
        long teardownStart = System.currentTimeMillis();
        container.kill();
        long teardownDurationMs = System.currentTimeMillis() - teardownStart;

        // Memory leak check after GC
        forceGc();
        Thread.sleep(500);
        double postTeardownHeapMb = getUsedHeapMb();
        double memoryRetainedMb = Math.max(0.0, postTeardownHeapMb - initialHeapMb);

        return new RuntimeMetrics(
            runtimeMode.toUpperCase(),
            specs.size(),
            auctions,
            proposals,
            bids,
            tradedKwh,
            startupDurationMs,
            simDurationMs,
            teardownDurationMs,
            maxObservedThreads,
            Math.round(initialHeapMb * 100.0) / 100.0,
            Math.round(peakHeapMb * 100.0) / 100.0,
            Math.round(postTeardownHeapMb * 100.0) / 100.0,
            Math.round(memoryRetainedMb * 100.0) / 100.0
        );
    }

    private RuntimeMetrics runIdleWorkload(String runtimeMode, String containerName,
                                           int prosumers, int consumers, int batteries) throws Exception {
        forceGc();
        double initialHeapMb = getUsedHeapMb();

        SmartGridScenario scenario = new SmartGridScenario();
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", prosumers);
        config.put("consumersCount", consumers);
        config.put("batteriesCount", batteries);
        config.put("stormProbability", 0.0);
        config.put("tickIntervalMs", 0); // zero interval = idle, no recurring tickers
        config.put("maxRounds", 0);
        config.put("registerWithDf", false);

        List<AgentSpec> specs = scenario.agents(config);

        ProfileImpl subProfile = new ProfileImpl(false);
        subProfile.setParameter(Profile.CONTAINER_NAME, containerName);
        subProfile.setParameter(Profile.MAIN_PORT, "13999");
        subProfile.setParameter(Profile.AGENT_RUNTIME, runtimeMode);
        ContainerController container = runtime.createAgentContainer(subProfile);

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int initialThreads = threadBean.getThreadCount();
        int maxObservedThreads = initialThreads;
        double peakHeapMb = initialHeapMb;

        List<AgentController> agentControllers = new ArrayList<>(specs.size());

        // Startup timing
        long startupStart = System.currentTimeMillis();
        for (AgentSpec spec : specs) {
            Object[] args = spec.getArgs().toArray(new Object[0]);
            String agentName = containerName + "-" + spec.getNameSuffix();
            AgentController ac = container.createNewAgent(agentName, spec.getClassName(), args);
            ac.start();
            agentControllers.add(ac);
        }
        long startupDurationMs = System.currentTimeMillis() - startupStart;

        // Sample idle footprint
        long sampleStart = System.currentTimeMillis();
        long sampleDeadline = sampleStart + 1000L;
        while (System.currentTimeMillis() < sampleDeadline) {
            int currentThreads = threadBean.getThreadCount();
            if (currentThreads > maxObservedThreads) {
                maxObservedThreads = currentThreads;
            }
            double currentHeap = getUsedHeapMb();
            if (currentHeap > peakHeapMb) {
                peakHeapMb = currentHeap;
            }
            Thread.sleep(50);
        }
        long sampleDurationMs = System.currentTimeMillis() - sampleStart;

        // Teardown timing
        long teardownStart = System.currentTimeMillis();
        if ("THREAD".equalsIgnoreCase(runtimeMode)) {
            // Request parallel termination on agents before container teardown to avoid sequential join timeouts
            agentControllers.parallelStream().forEach(ac -> {
                try {
                    ac.kill();
                } catch (Exception ignored) {}
            });
            Thread.sleep(1500);
        }
        try {
            container.kill();
        } catch (Exception ignored) {}
        long teardownDurationMs = System.currentTimeMillis() - teardownStart;

        // Memory leak check after GC
        forceGc();
        Thread.sleep(500);
        double postTeardownHeapMb = getUsedHeapMb();
        double memoryRetainedMb = Math.max(0.0, postTeardownHeapMb - initialHeapMb);

        return new RuntimeMetrics(
            runtimeMode.toUpperCase(),
            specs.size(),
            0,
            0,
            0,
            0.0,
            startupDurationMs,
            sampleDurationMs,
            teardownDurationMs,
            maxObservedThreads,
            Math.round(initialHeapMb * 100.0) / 100.0,
            Math.round(peakHeapMb * 100.0) / 100.0,
            Math.round(postTeardownHeapMb * 100.0) / 100.0,
            Math.round(memoryRetainedMb * 100.0) / 100.0
        );
    }

    private static double getUsedHeapMb() {
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);
    }

    private static void forceGc() {
        for (int i = 0; i < 3; i++) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
    }

    private static void printComparisonTable(String title, RuntimeMetrics thread, RuntimeMetrics verticle) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n========================================================================================\n");
        sb.append("         JADE RUNTIME BENCHMARK: ").append(title).append("\n");
        sb.append("========================================================================================\n");
        sb.append(String.format("| %-32s | %-18s | %-18s | %-12s |\n", "Benchmark Metric", "Virtual Thread", "Vert.x Verticle", "Advantage"));
        sb.append("|----------------------------------|--------------------|--------------------|--------------|\n");
        sb.append(String.format("| %-32s | %-18d | %-18d | %-12s |\n", "Agent Density (Count)", thread.agentCount(), verticle.agentCount(), "Identical"));
        sb.append(String.format("| %-32s | %-18d | %-18d | %-12s |\n", "Auctions Held", thread.auctionsHeld(), verticle.auctionsHeld(), "-"));
        sb.append(String.format("| %-32s | %-18d | %-18d | %-12s |\n", "Proposals Processed", thread.proposalsReceived(), verticle.proposalsReceived(), "-"));
        sb.append(String.format("| %-32s | %-18d | %-18d | %-12s |\n", "Winning Bids Awarded", thread.acceptedBids(), verticle.acceptedBids(), "-"));
        sb.append(String.format("| %-32s | %-18.4f | %-18.4f | %-12s |\n", "Total Energy Traded (kWh)", thread.totalKwhTraded(), verticle.totalKwhTraded(), "-"));
        sb.append(String.format("| %-32s | %-18s | %-18s | %-12s |\n", "Startup Latency (ms)", thread.startupDurationMs() + " ms", verticle.startupDurationMs() + " ms",
            verticle.startupDurationMs() <= thread.startupDurationMs() ? "Verticle" : "Thread"));
        sb.append(String.format("| %-32s | %-18s | %-18s | %-12s |\n", "Teardown Duration (ms)", thread.teardownDurationMs() + " ms", verticle.teardownDurationMs() + " ms",
            verticle.teardownDurationMs() <= thread.teardownDurationMs() ? "Verticle" : "Thread"));
        sb.append(String.format("| %-32s | %-18d | %-18d | %-12s |\n", "Peak Threads (Carrier/OS)", thread.peakThreads(), verticle.peakThreads(),
            verticle.peakThreads() <= thread.peakThreads() ? "Verticle" : "Thread"));
        sb.append(String.format("| %-32s | %-18.2f | %-18.2f | %-12s |\n", "Peak Heap Usage (MB)", thread.peakHeapMb(), verticle.peakHeapMb(),
            verticle.peakHeapMb() <= thread.peakHeapMb() ? "Verticle" : "Thread"));
        sb.append(String.format("| %-32s | %-18.2f | %-18.2f | %-12s |\n", "Retained Heap Post-GC (MB)", thread.memoryRetainedMb(), verticle.memoryRetainedMb(),
            verticle.memoryRetainedMb() <= thread.memoryRetainedMb() ? "Verticle" : "Thread"));
        sb.append("========================================================================================\n\n");

        LOG.info(sb.toString());
    }
}
