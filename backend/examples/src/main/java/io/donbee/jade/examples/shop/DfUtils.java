package io.donbee.jade.examples.shop;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPAException;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Small helpers around the Directory Facilitator (DF).
 */
final class DfUtils {

    private DfUtils() {
    }

    /** Register {@code agent} in the DF as offering a service of the given type. */
    static void registerService(Agent agent, String serviceType, String serviceName) throws FIPAException {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(serviceName);
        dfd.addServices(sd);
        DFService.register(agent, dfd);
    }

    /**
     * Find one agent offering the given service type via DF search,
     * or {@code null} when nobody provides it.
     */
    static AID findServiceProvider(Agent agent, String serviceType) throws FIPAException {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);
        DFAgentDescription[] results = DFService.search(agent, template);
        return results.length > 0 ? results[0].getName() : null;
    }
}
