package io.donbee.jade.rest.service;

import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;

import java.util.List;

/**
 * Interface for Directory Facilitator (DF) data access.
 * Split from PlatformService to follow ISP — handlers that only need DF
 * operations don't depend on the full PlatformService interface.
 *
 * <p><b>Old GUI implementation:</b> The DF GUI was
 * {@code io.donbee.jade.tools.dfgui.DFGUI}, a Swing application with
 * three tabs (Registrations, Search Result, DF Federation). It
 * communicated with the DF agent through
 * {@code io.donbee.jade.domain.DFGUIAdapter}, posting
 * {@code GuiEvent}s that were translated into FIPA management
 * actions ({@code Register}, {@code Deregister}, {@code Modify},
 * {@code Search}, {@code GetParents}, {@code Federate}).
 * Each method here replicates one of those GUI-driven operations
 * directly, using the {@code DFRequestAgent} helper to send
 * synchronous ACL messages to the DF.</p>
 */
public interface DFService {

    /**
     * List all agent descriptions registered with the local DF.
     */
    List<DFRegistrationInfo> listDFRegistrations();

    /**
     * Register an agent with the local DF.
     */
    DFRegistrationInfo registerWithDF(String agentName, List<String> addresses,
                                      List<DFServiceInfo> services);

    /**
     * Deregister an agent from the local DF.
     */
    void deregisterFromDF(String agentName);

    /**
     * View a specific agent's DF registration.
     */
    DFRegistrationInfo getDFRegistration(String agentName);

    /**
     * Modify an existing DF registration.
     */
    DFRegistrationInfo modifyDFRegistration(String agentName, List<String> addresses,
                                             List<DFServiceInfo> services);

    /**
     * Search for agents in the DF matching the given criteria.
     */
    List<DFRegistrationInfo> searchDF(DFAgentDescription template, SearchConstraintsInfo constraints);

    /**
     * Get the description of this DF.
     */
    DFRegistrationInfo getDFDescription();

    /**
     * Get federation status and metadata.
     */
    DFStatus getDFStatus();

    /**
     * List the DFs that this DF is federated with (its parents).
     */
    List<DFParentInfo> getDFParents();

    /**
     * List the DFs federated with this DF (its children).
     */
    List<DFParentInfo> getDFChildren();

    /**
     * Federate this DF with another (parent) DF.
     */
    DFParentInfo federateDF(String parentDFName, List<String> parentDFAddresses);

    /**
     * Deregister this DF from a parent DF it was federated with.
     * The parent's contact addresses are resolved from the current
     * parent list (falling back to any provided addresses).
     */
    void deregisterParentDF(String parentDFName);

    /**
     * Deregister a child DF (one federated with this DF) from this DF.
     */
    void deregisterChildDF(String childDFName);

    class DFRegistrationInfo {
        public final String name;
        public final List<String> addresses;
        public final List<DFServiceInfo> services;
        public final String ownership;

        public DFRegistrationInfo(String name, List<String> addresses,
                                  List<DFServiceInfo> services, String ownership) {
            this.name = name;
            this.addresses = addresses;
            this.services = services;
            this.ownership = ownership;
        }

        public DFRegistrationInfo(DFAgentDescription dfd) {
            String dfdName = "";
            List<String> addrs = java.util.Collections.emptyList();
            if (dfd.getName() != null) {
                dfdName = dfd.getName().getName();
                if (dfd.getName().getAllAddresses() != null) {
                    addrs = toList(dfd.getName().getAllAddresses());
                }
            }
            this.name = dfdName;
            this.addresses = addrs;

            List<DFServiceInfo> svcList = new java.util.ArrayList<>();
            if (dfd.getAllServices() != null) {
                java.util.Iterator<?> it = dfd.getAllServices();
                while (it.hasNext()) {
                    io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription sd =
                        (io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription) it.next();
                    svcList.add(new DFServiceInfo(
                        sd.getType(), sd.getName(), sd.getOwnership()
                    ));
                }
            }
            this.services = svcList;
            this.ownership = "";
        }

        private static List<String> toList(java.util.Iterator<String> it) {
            List<String> list = new java.util.ArrayList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        }
    }

    class DFServiceInfo {
        public final String type;
        public final String name;
        public final String ownership;

        public DFServiceInfo(String type, String name, String ownership) {
            this.type = type;
            this.name = name;
            this.ownership = ownership;
        }
    }

    class SearchConstraintsInfo {
        public final Long maxDepth;
        public final Long maxResults;

        public SearchConstraintsInfo(Long maxDepth, Long maxResults) {
            this.maxDepth = maxDepth;
            this.maxResults = maxResults;
        }
    }

    class DFStatus {
        public final boolean running;
        public final String agent;
        public final String container;
        public final int registeredAgentCount;
        public final int parentCount;
        public final int childCount;

        public DFStatus(boolean running, String agent, String container,
                        int registeredAgentCount, int parentCount, int childCount) {
            this.running = running;
            this.agent = agent;
            this.container = container;
            this.registeredAgentCount = registeredAgentCount;
            this.parentCount = parentCount;
            this.childCount = childCount;
        }
    }

    class DFParentInfo {
        public final String name;
        public final List<String> addresses;

        public DFParentInfo(String name, List<String> addresses) {
            this.name = name;
            this.addresses = addresses;
        }
    }
}
