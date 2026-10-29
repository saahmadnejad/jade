package io.donbee.jade.core;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * Verticle wrapper for a JADE Agent deployed in the Vert.x runtime mode.
 * Namespaced per container as {@code c/<container>/a/<agent>}.
 */
public class AgentVerticle extends AbstractVerticle {

    private final Agent agent;
    private final AID aid;
    private final String containerName;
    private final String agentAddress;
    private final AtomicBoolean undeploying = new AtomicBoolean(false);

    public AgentVerticle(Agent agent, AID aid, String containerName) {
        this.agent = agent;
        this.aid = aid;
        this.containerName = containerName;
        this.agentAddress = "c/" + containerName + "/a/" + aid.getLocalName();
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public String getContainerName() {
        return containerName;
    }

    public AID getAID() {
        return aid;
    }

    public Agent getAgent() {
        return agent;
    }

    public boolean isUndeploying() {
        return undeploying.get();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        agent.powerUpVerticle(aid, this);
        startPromise.complete();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (!undeploying.compareAndSet(false, true)) {
            stopPromise.complete();
            return;
        }
        if (agent.isAlive()) {
            agent.doDelete();
            agent.getTerminationFuture().whenComplete((v, t) -> stopPromise.complete());
        } else {
            stopPromise.complete();
        }
    }
}
