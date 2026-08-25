package io.donbee.jade.rest.service;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ContainerID;
import io.donbee.jade.core.NameClashException;
import io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementOntology;
import io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementVocabulary;
import io.donbee.jade.domain.FIPAAgentManagement.Register;
import io.donbee.jade.domain.FIPAAgentManagement.Deregister;
import io.donbee.jade.domain.FIPAAgentManagement.Modify;
import io.donbee.jade.domain.FIPAAgentManagement.Search;
import io.donbee.jade.domain.FIPAAgentManagement.SearchConstraints;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;
import io.donbee.jade.domain.DFGUIManagement.DeregisterFrom;
import io.donbee.jade.domain.DFGUIManagement.Federate;
import io.donbee.jade.domain.DFGUIManagement.GetParents;
import io.donbee.jade.domain.DFGUIManagement.DFAppletOntology;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.domain.FIPAException;
import io.donbee.jade.content.onto.basic.Action;
import io.donbee.jade.content.onto.basic.Result;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;
import io.donbee.jade.rest.service.DFService.DFParentInfo;
import io.donbee.jade.rest.service.DFService.SearchConstraintsInfo;
import io.donbee.jade.rest.service.DFService.DFStatus;

import io.donbee.jade.mtp.MTPDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of PlatformService and DFService using the JADE backend.
 */
public class JadesPlatformService implements PlatformService, DFService {

    private final AgentContainer impl;
    private final AgentManager agentManager;

    public JadesPlatformService(AgentContainer impl, AgentManager agentManager) {
        this.impl = impl;
        this.agentManager = agentManager;
    }

    @Override
    public boolean isMainContainer() {
        return agentManager != null;
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new PlatformInfo(
            impl.getPlatformID(),
            impl.here().getName(),
            impl.getMain() != null,
            impl.getAMS().getName(),
            impl.getDefaultDF().getName()
        );
    }

    @Override
    public List<AgentInfo> getAgents(boolean detail) {
        if (agentManager == null) {
            return java.util.Collections.emptyList();
        }
        List<AgentInfo> result = new ArrayList<>();
        ContainerID cid = impl.getID();
        io.donbee.jade.util.leap.List agents;
        try {
            agents = agentManager.containerAgents(cid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list agents: " + e.getMessage(), e);
        }
        for (int i = 0; i < agents.size(); i++) {
            AID aid = (AID) agents.get(i);
            if (detail) {
                String state = "UNKNOWN";
                String ownership = "";
                String[] addresses = new String[0];
                try {
                    AMSAgentDescription amsDesc = agentManager.getAMSDescription(aid);
                    state = amsDesc.getState() != null ? amsDesc.getState() : "UNKNOWN";
                    ownership = amsDesc.getOwnership() != null ? amsDesc.getOwnership() : "";
                } catch (Exception ignored) {
                }
                result.add(new AgentInfo(
                    aid.getLocalName(),
                    state,
                    ownership,
                    cid.getName(),
                    aid.getAllAddresses() != null ? toArray(aid.getAllAddresses()) : new String[0]
                ));
            } else {
                result.add(new AgentInfo(
                    aid.getLocalName(),
                    null,
                    null,
                    cid.getName(),
                    new String[0]
                ));
            }
        }
        return result;
    }

    @Override
    public AgentInfo getAgent(String agentName) {
        if (agentManager == null) return null;
        ContainerID cid = impl.getID();
        io.donbee.jade.util.leap.List agents;
        try {
            agents = agentManager.containerAgents(cid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list agents: " + e.getMessage(), e);
        }
        for (int i = 0; i < agents.size(); i++) {
            AID aid = (AID) agents.get(i);
            if (aid.getName().equals(agentName) || aid.getLocalName().equals(agentName)) {
                String state = "UNKNOWN";
                String ownership = "";
                try {
                    AMSAgentDescription amsDesc = agentManager.getAMSDescription(aid);
                    state = amsDesc.getState() != null ? amsDesc.getState() : "UNKNOWN";
                    ownership = amsDesc.getOwnership() != null ? amsDesc.getOwnership() : "";
                } catch (Exception ignored) {
                }
                return new AgentInfo(
                    aid.getLocalName(),
                    state,
                    ownership,
                    cid.getName(),
                    aid.getAllAddresses() != null ? toArray(aid.getAllAddresses()) : new String[0]
                );
            }
        }
        return null;
    }

    @Override
    public List<ContainerInfo> getContainers() {
        if (agentManager == null) return java.util.Collections.emptyList();
        ContainerID[] cids = agentManager.containerIDs();
        java.util.List<ContainerInfo> result = new ArrayList<>();
        String mainContainerName = impl.here().getName();
        for (ContainerID cid : cids) {
            result.add(new ContainerInfo(
                cid.getName(),
                cid.getAddress(),
                cid.getPort(),
                cid.getName().equals(mainContainerName)
            ));
        }
        return result;
    }

    @Override
    public ContainerInfo getContainer(String name) {
        if (agentManager == null) return null;
        ContainerID[] cids = agentManager.containerIDs();
        for (ContainerID cid : cids) {
            if (cid.getName().equals(name)) {
                return new ContainerInfo(
                    cid.getName(),
                    cid.getAddress(),
                    cid.getPort(),
                    cid.getName().equals(impl.here().getName())
                );
            }
        }
        return null;
    }

    @Override
    public void killAgent(String agentName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        try {
            agentManager.kill(aid, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to kill agent: " + e.getMessage(), e);
        }
    }

    @Override
    public void suspendAgent(String agentName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        try {
            agentManager.suspend(aid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to suspend agent: " + e.getMessage(), e);
        }
    }

    @Override
    public void resumeAgent(String agentName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        try {
            agentManager.activate(aid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resume agent: " + e.getMessage(), e);
        }
    }

    @Override
    public void shutdownPlatform() {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            agentManager.shutdownPlatform(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to shutdown platform: " + e.getMessage(), e);
        }
    }

    @Override
    public void killContainer(String containerName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        ContainerID localCid = impl.getID();
        if (containerName.equals(localCid.getName())) {
            throw new IllegalStateException("Cannot kill the Main Container");
        }
        ContainerID targetCid = null;
        for (ContainerID cid : agentManager.containerIDs()) {
            if (cid.getName().equals(containerName)) {
                targetCid = cid;
                break;
            }
        }
        if (targetCid == null) {
            throw new IllegalArgumentException("Container not found: " + containerName);
        }
        try {
            agentManager.killContainer(targetCid, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to kill container: " + e.getMessage(), e);
        }
    }

    private ContainerID findContainerByName(String containerName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        ContainerID localCid = impl.getID();
        if (containerName.equals(localCid.getName())) {
            return localCid;
        }
        for (ContainerID cid : agentManager.containerIDs()) {
            if (cid.getName().equals(containerName)) {
                return cid;
            }
        }
        throw new IllegalArgumentException("Container not found: " + containerName);
    }

    private void sendAMSAction(io.donbee.jade.content.Concept action, String actionLabel, io.donbee.jade.content.onto.Ontology ontology) {
        try {
            io.donbee.jade.content.ContentManager cm = new io.donbee.jade.content.ContentManager();
            cm.registerOntology(ontology);
            io.donbee.jade.content.lang.sl.SLCodec codec = new io.donbee.jade.content.lang.sl.SLCodec();
            cm.registerLanguage(codec);

            Action a = new Action();
            a.setActor(impl.getAMS());
            a.setAction(action);

            ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.setSender(impl.getAMS());
            requestMsg.addReceiver(impl.getAMS());
            requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
            requestMsg.setOntology(codec.getName());
            requestMsg.setReplyByDate(new java.util.Date(System.currentTimeMillis() + 10000L));
            cm.fillContent(requestMsg, a);

            impl.postMessageToLocalAgent(requestMsg, impl.getAMS());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send " + actionLabel + " action: " + e.getMessage(), e);
        }
    }

    private void sendAMSAction(io.donbee.jade.content.Concept action, String actionLabel) {
        sendAMSAction(action, actionLabel, io.donbee.jade.domain.persistence.PersistenceOntology.getInstance());
    }

    @Override
    public void saveContainer(String containerName, String repository) {
        ContainerID cid = findContainerByName(containerName);
        io.donbee.jade.domain.persistence.SaveContainer saveAct = new io.donbee.jade.domain.persistence.SaveContainer();
        saveAct.setContainer(cid);
        saveAct.setRepository(repository);
        sendAMSAction(saveAct, "SaveContainer");
    }

    @Override
    public void loadContainer(String containerName, String repository) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        io.donbee.jade.domain.persistence.LoadContainer loadAct = new io.donbee.jade.domain.persistence.LoadContainer();
        loadAct.setContainer(new ContainerID(containerName, null));
        loadAct.setRepository(repository);
        sendAMSAction(loadAct, "LoadContainer");
    }

    @Override
    public MTPDescriptor installMTP(String containerName, String address, String className) {
        ContainerID cid = findContainerByName(containerName);
        try {
            return agentManager.installMTP(address, cid, className);
        } catch (Exception e) {
            throw new RuntimeException("Failed to install MTP: " + e.getMessage(), e);
        }
    }

    @Override
    public void uninstallMTP(String containerName, String address) {
        ContainerID cid = findContainerByName(containerName);
        try {
            agentManager.uninstallMTP(address, cid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to uninstall MTP: " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.List<MTPInfo> getMTPs(String containerName) {
        ContainerID cid = findContainerByName(containerName);
        try {
            java.util.List<MTPInfo> result = new java.util.ArrayList<>();
            io.donbee.jade.util.leap.List mtps = agentManager.containerMTPs(cid);
            for (int i = 0; i < mtps.size(); i++) {
                MTPDescriptor dsc = (MTPDescriptor) mtps.get(i);
                String address = dsc.getAddresses().length > 0 ? dsc.getAddresses()[0] : "";
                result.add(new MTPInfo(address, dsc.getName()));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list MTPs: " + e.getMessage(), e);
        }
    }

    @Override
    public AgentInfo deployAgent(String agentName, String className, Object[] args) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        ContainerID cid = impl.getID();
        try {
            agentManager.create(agentName, className, args, cid, null, null, null, null);
        } catch (NameClashException e) {
            throw new IllegalArgumentException("Agent already exists: " + agentName, e);
        } catch (Exception e) {
            if (containsCause(e, NameClashException.class)) {
                throw new IllegalArgumentException("Agent already exists: " + agentName, e);
            }
            throw new RuntimeException("Failed to deploy agent: " + e.getMessage(), e);
        }
        AID aid = new AID();
        aid.setName(agentName);
        return new AgentInfo(
            aid.getName(),
            null,
            null,
            cid.getName(),
            new String[]{});
    }

    @Override
    public AgentInfo freezeAgent(String agentName, String bufferContainer, String repository) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        ContainerID bufferCid = findContainerByName(bufferContainer);
        io.donbee.jade.domain.persistence.FreezeAgent freezeAct = new io.donbee.jade.domain.persistence.FreezeAgent();
        freezeAct.setAgent(aid);
        freezeAct.setRepository(repository);
        freezeAct.setBufferContainer(bufferCid);
        sendAMSAction(freezeAct, "FreezeAgent");
        return new AgentInfo(aid.getName(), "FROZEN", null, bufferCid.getName(), new String[]{});
    }

    @Override
    public AgentInfo thawAgent(String agentName, String targetContainer, String repository) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        ContainerID targetCid = findContainerByName(targetContainer);
        io.donbee.jade.domain.persistence.ThawAgent thawAct = new io.donbee.jade.domain.persistence.ThawAgent();
        thawAct.setAgent(aid);
        thawAct.setRepository(repository);
        thawAct.setNewContainer(targetCid);
        sendAMSAction(thawAct, "ThawAgent");
        return new AgentInfo(aid.getName(), "ACTIVE", null, targetCid.getName(), new String[]{});
    }

    @Override
    public AgentInfo cloneAgent(String agentName, String newName, String targetContainer) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        ContainerID targetCid = findContainerByName(targetContainer);
        try {
            agentManager.copy(aid, targetCid, newName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone agent: " + e.getMessage(), e);
        }
        AID clonedAid = new AID();
        clonedAid.setName(newName);
        return new AgentInfo(clonedAid.getName(), "ACTIVE", null, targetCid.getName(), new String[]{});
    }

    @Override
    public void moveAgent(String agentName, String targetContainer) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        ContainerID targetCid = findContainerByName(targetContainer);
        try {
            agentManager.move(aid, targetCid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to move agent: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAgent(String agentName, String repository) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        io.donbee.jade.domain.persistence.SaveAgent saveAct = new io.donbee.jade.domain.persistence.SaveAgent();
        saveAct.setAgent(aid);
        saveAct.setRepository(repository);
        sendAMSAction(saveAct, "SaveAgent");
    }

    @Override
    public AgentInfo loadAgent(String agentName, String targetContainer, String repository) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        ContainerID targetCid = findContainerByName(targetContainer);
        io.donbee.jade.domain.persistence.LoadAgent loadAct = new io.donbee.jade.domain.persistence.LoadAgent();
        loadAct.setAgent(new AID(agentName, AID.ISLOCALNAME));
        loadAct.setRepository(repository);
        loadAct.setWhere(targetCid);
        sendAMSAction(loadAct, "LoadAgent");
        AID aid = new AID();
        aid.setName(agentName);
        return new AgentInfo(aid.getName(), "ACTIVE", null, targetCid.getName(), new String[]{});
    }

    @Override
    public void changeAgentOwnership(String agentName, String newOwner) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID aid = findAgentByName(agentName);
        if (aid == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }
        io.donbee.jade.domain.FIPAAgentManagement.Modify modifyAct = new io.donbee.jade.domain.FIPAAgentManagement.Modify();
        AMSAgentDescription amsd = new AMSAgentDescription();
        amsd.setName(aid);
        amsd.setOwnership(newOwner);
        modifyAct.setDescription(amsd);
        sendAMSAction(modifyAct, "Modify");
    }

    @Override
    public void registerRemoteAgent(String aidName, String[] addresses) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            io.donbee.jade.domain.FIPAAgentManagement.Register registerAct =
                new io.donbee.jade.domain.FIPAAgentManagement.Register();
            io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription amsDesc =
                new io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription();
            AID aid = new AID();
            aid.setName(aidName);
            if (addresses != null) {
                for (String addr : addresses) {
                    aid.addAddresses(addr);
                }
            }
            amsDesc.setName(aid);
            registerAct.setDescription(amsDesc);
            sendAMSAction(registerAct, "Register",
                io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementOntology.getInstance());
        } catch (Exception e) {
            throw new RuntimeException("Failed to register remote agent: " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.List<RemotePlatformInfo> getRemotePlatforms() {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        java.util.List<RemotePlatformInfo> platforms = new java.util.ArrayList<>();
        try {
            ContainerID cid = impl.getID();
            String[] addresses = new String[]{cid.getAddress() + ":" + cid.getPort()};
            String[] services = {"FIPAAgentManagement", "Mobility", "Messaging"};
            platforms.add(new RemotePlatformInfo(
                impl.getPlatformID(),
                impl.getAMS().getName(),
                addresses != null ? addresses : new String[]{},
                services
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to list remote platforms: " + e.getMessage(), e);
        }
        return platforms;
    }

    @Override
    public RemotePlatformInfo addRemotePlatform(String amsName, String[] addresses) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            io.donbee.jade.domain.FIPAAgentManagement.GetDescription gd = new io.donbee.jade.domain.FIPAAgentManagement.GetDescription();
            AID remoteAid = new AID();
            remoteAid.setName(amsName);
            sendAMSAction(gd, "GetDescription", io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementOntology.getInstance());
            return new RemotePlatformInfo(amsName, amsName, addresses, new String[]{"FIPAAgentManagement"});
        } catch (Exception e) {
            throw new RuntimeException("Failed to add remote platform: " + e.getMessage(), e);
        }
    }

    @Override
    public RemotePlatformInfo getRemotePlatformDescription(String platformName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        java.util.List<RemotePlatformInfo> platforms = getRemotePlatforms();
        for (RemotePlatformInfo p : platforms) {
            if (p.name.equals(platformName)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Remote platform not found: " + platformName);
    }

    @Override
    public void removeRemotePlatform(String platformName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        throw new UnsupportedOperationException("Removing remote platforms is not supported by this JADE implementation");
    }

    @Override
    public RemotePlatformInfo fetchRemotePlatform(String url) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }
        try {
            String[] parts = url.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 1099;
            String addresses = host + ":" + port;
            return addRemotePlatform(host + ":" + port + "/FIPA", new String[]{addresses});
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }
    }

    @Override
    public java.util.List<AgentInfo> searchRemotePlatformAgents(String platformName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        java.util.List<AgentInfo> result = new java.util.ArrayList<>();
        return result;
    }

    private AID findAgentByName(String agentName) {        ContainerID cid = impl.getID();
        io.donbee.jade.util.leap.List agents;
        try {
            agents = agentManager.containerAgents(cid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list agents: " + e.getMessage(), e);
        }
        for (int i = 0; i < agents.size(); i++) {
            AID aid = (AID) agents.get(i);
            if (aid.getName().equals(agentName) || aid.getLocalName().equals(agentName)) {
                return aid;
            }
        }
        return null;
    }

    private String[] toArray(java.util.Iterator<String> it) {
        java.util.List<String> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray(new String[0]);
    }

    private boolean containsCause(Throwable t, Class<? extends Throwable> causeClass) {
        Throwable current = t;
        while (current != null) {
            if (causeClass.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    // ============================================================
    // DFService implementation
    // ============================================================

    private static final long DF_TIMEOUT_MS = 30000L;

    private AID getDefaultDF() {
        return impl.getDefaultDF();
    }

    @Override
    public List<DFRegistrationInfo> listDFRegistrations() {
        return searchDF(new DFAgentDescription(), new SearchConstraintsInfo(null, -1L));
    }

    @Override
    public DFRegistrationInfo registerWithDF(String agentName, List<String> addresses,
                                             List<DFServiceInfo> services) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            AID dfAID = getDefaultDF();
            if (dfAID == null) {
                throw new IllegalStateException("No default DF available on this container");
            }

            DFAgentDescription dfd = new DFAgentDescription();
            AID agentAID = new AID();
            agentAID.setName(agentName);
            dfd.setName(agentAID);

            if (addresses != null) {
                for (String addr : addresses) {
                    agentAID.addAddresses(addr);
                }
            }

            if (services != null) {
                for (DFServiceInfo svc : services) {
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(svc.type);
                    sd.setName(svc.name);
                    if (svc.ownership != null) {
                        sd.setOwnership(svc.ownership);
                    }
                    dfd.addServices(sd);
                }
            }

            Register register = new Register();
            register.setDescription(dfd);

            ACLMessage reply = DFRequestAgent.execute(
                agentManager, impl.getID(), dfAID, register,
                FIPAManagementOntology.getInstance().getName(),
                FIPAManagementVocabulary.REGISTER,
                DF_TIMEOUT_MS
            );

            if (reply == null) {
                throw new RuntimeException("No response from DF");
            }
            int perf = reply.getPerformative();
            if (perf == ACLMessage.INFORM) {
                DFAgentDescription registered = io.donbee.jade.domain.DFService.decodeDone(reply.getContent());
                return new DFRegistrationInfo(registered);
            } else {
                throw new RuntimeException("DF registration failed: " + decodeFailure(reply));
            }
        } catch (FIPAException e) {
            throw new RuntimeException("Failed to register with DF: " + e.getMessage(), e);
        }
    }

    @Override
    public void deregisterFromDF(String agentName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            AID dfAID = getDefaultDF();
            if (dfAID == null) {
                throw new IllegalStateException("No default DF available on this container");
            }

            DFAgentDescription dfd = new DFAgentDescription();
            AID agentAID = new AID();
            agentAID.setName(agentName);
            dfd.setName(agentAID);

            Deregister deregister = new Deregister();
            deregister.setDescription(dfd);

            ACLMessage reply = DFRequestAgent.execute(
                agentManager, impl.getID(), dfAID, deregister,
                FIPAManagementOntology.getInstance().getName(),
                FIPAManagementVocabulary.DEREGISTER,
                DF_TIMEOUT_MS
            );

            if (reply == null) {
                throw new RuntimeException("No response from DF");
            }
            int perf = reply.getPerformative();
            if (perf == ACLMessage.INFORM) {
                // Success
            } else {
                throw new RuntimeException("DF deregistration failed: " + decodeFailure(reply));
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public DFRegistrationInfo getDFRegistration(String agentName) {
        if (agentName == null || agentName.isEmpty()) {
            throw new IllegalArgumentException("Agent name is required");
        }
        DFAgentDescription template = new DFAgentDescription();
        AID agentAID = new AID();
        agentAID.setName(agentName);
        template.setName(agentAID);

        List<DFRegistrationInfo> results = searchDF(template, new SearchConstraintsInfo(0L, 1L));
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Agent not registered with DF: " + agentName);
        }
        return results.get(0);
    }

    @Override
    public DFRegistrationInfo modifyDFRegistration(String agentName, List<String> addresses,
                                                     List<DFServiceInfo> services) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            AID dfAID = getDefaultDF();
            if (dfAID == null) {
                throw new IllegalStateException("No default DF available on this container");
            }

            DFAgentDescription dfd = new DFAgentDescription();
            AID agentAID = new AID();
            agentAID.setName(agentName);
            dfd.setName(agentAID);

            if (addresses != null) {
                for (String addr : addresses) {
                    agentAID.addAddresses(addr);
                }
            }

            if (services != null) {
                for (DFServiceInfo svc : services) {
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(svc.type);
                    sd.setName(svc.name);
                    if (svc.ownership != null) {
                        sd.setOwnership(svc.ownership);
                    }
                    dfd.addServices(sd);
                }
            }

            Modify modify = new Modify();
            modify.setDescription(dfd);

            ACLMessage reply = DFRequestAgent.execute(
                agentManager, impl.getID(), dfAID, modify,
                FIPAManagementOntology.getInstance().getName(),
                FIPAManagementVocabulary.MODIFY,
                DF_TIMEOUT_MS
            );

            if (reply == null) {
                throw new RuntimeException("No response from DF");
            }
            int perf = reply.getPerformative();
            if (perf == ACLMessage.INFORM) {
                DFAgentDescription modified = io.donbee.jade.domain.DFService.decodeDone(reply.getContent());
                return new DFRegistrationInfo(modified);
            } else {
                throw new RuntimeException("DF modification failed: " + decodeFailure(reply));
            }
        } catch (FIPAException e) {
            throw new RuntimeException("Failed to modify DF registration: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DFRegistrationInfo> searchDF(DFAgentDescription template, SearchConstraintsInfo constraints) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            AID dfAID = getDefaultDF();
            if (dfAID == null) {
                throw new IllegalStateException("No default DF available on this container");
            }

            DFAgentDescription dfdTemplate = template;
            SearchConstraints sc = new SearchConstraints();
            if (constraints.maxResults != null) {
                sc.setMaxResults(new Long(constraints.maxResults));
            }
            if (constraints.maxDepth != null) {
                sc.setMaxDepth(new Long(constraints.maxDepth));
            }

            Search search = new Search();
            search.setDescription(dfdTemplate);
            search.setConstraints(sc);

            ACLMessage reply = DFRequestAgent.execute(
                agentManager, impl.getID(), dfAID, search,
                FIPAManagementOntology.getInstance().getName(),
                FIPAManagementVocabulary.SEARCH,
                DF_TIMEOUT_MS
            );

            if (reply == null) {
                throw new RuntimeException("No response from DF");
            }
            int perf = reply.getPerformative();
            if (perf == ACLMessage.INFORM) {
                DFAgentDescription[] results = io.donbee.jade.domain.DFService.decodeResult(reply.getContent());
                List<DFRegistrationInfo> infos = new ArrayList<>();
                for (DFAgentDescription r : results) {
                    infos.add(new DFRegistrationInfo(r));
                }
                return infos;
            } else {
                throw new RuntimeException("DF search failed: " + decodeFailure(reply));
            }
        } catch (FIPAException e) {
            throw new RuntimeException("Failed to search DF: " + e.getMessage(), e);
        }
    }

    @Override
    public DFRegistrationInfo getDFDescription() {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        try {
            AID dfAID = getDefaultDF();
            if (dfAID == null) {
                throw new IllegalStateException("No default DF available on this container");
            }

            DFAgentDescription template = new DFAgentDescription();
            template.setName(dfAID);

            // The Search action requires a description slot (and constraints),
            // otherwise the DF rejects it with an OntologyException.
            SearchConstraints constraints = new SearchConstraints();
            constraints.setMaxResults(new Long(1));
            Search search = new Search();
            search.setDescription(template);
            search.setConstraints(constraints);

            ACLMessage reply = DFRequestAgent.execute(
                agentManager, impl.getID(), dfAID,
                search,
                FIPAManagementOntology.getInstance().getName(),
                FIPAManagementVocabulary.SEARCH,
                DF_TIMEOUT_MS
            );

            if (reply == null) {
                throw new RuntimeException("No response from DF");
            }
            if (reply.getPerformative() == ACLMessage.INFORM) {
                DFAgentDescription[] results = io.donbee.jade.domain.DFService.decodeResult(reply.getContent());
                if (results.length > 0) {
                    return new DFRegistrationInfo(results[0]);
                }
                return new DFRegistrationInfo(dfAID.getName(), new ArrayList<>(), new ArrayList<>(), "");
            } else {
                throw new RuntimeException("DF search failed: " + decodeFailure(reply));
            }
        } catch (FIPAException e) {
            throw new RuntimeException("Failed to get DF description: " + e.getMessage(), e);
        }
    }

    @Override
    public DFStatus getDFStatus() {
        List<DFRegistrationInfo> registrations = listDFRegistrations();
        int parentCount = 0;
        int childCount = 0;
        try {
            parentCount = getDFParents().size();
            childCount = getDFChildren().size();
        } catch (RuntimeException e) {
            // Federation data may be unavailable (e.g. not a main container);
            // report zero counts rather than failing the status request.
        }
        String dfName = getDefaultDF() != null ? getDefaultDF().getName() : "unknown";
        String containerName = impl.here() != null ? impl.here().getName() : "unknown";
        return new DFStatus(
            true,
            dfName,
            containerName,
            registrations.size(),
            parentCount,
            childCount
        );
    }

    private String decodeFailure(ACLMessage reply) {
        if (reply == null) return "no response";
        String content = reply.getContent();
        if (content == null || content.isEmpty()) {
            return ACLMessage.getPerformative(reply.getPerformative());
        }
        return ACLMessage.getPerformative(reply.getPerformative()) + ": " + content;
    }

    // ============================================================
    // DFService federation implementation
    // ============================================================

    private AID aidFromName(String name) {
        AID aid = new AID();
        aid.setName(name);
        return aid;
    }

    @Override
    public List<DFParentInfo> getDFParents() {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID dfAID = getDefaultDF();
        if (dfAID == null) {
            throw new IllegalStateException("No default DF available on this container");
        }

        GetParents action = new GetParents();
        DFRequestAgent.DFResponse resp = DFRequestAgent.executeFull(
            agentManager, impl.getID(), dfAID, action,
            DFAppletOntology.getInstance().getName(),
            DF_TIMEOUT_MS
        );

        ACLMessage reply = resp.message;
        if (reply == null) {
            throw new RuntimeException("No response from DF");
        }
        if (reply.getPerformative() == ACLMessage.FAILURE) {
            throw new RuntimeException("DF get-parents failed: " + decodeFailure(reply));
        }
        if (reply.getPerformative() != ACLMessage.INFORM) {
            throw new RuntimeException("Unexpected reply from DF: " + ACLMessage.getPerformative(reply.getPerformative()));
        }

        List<DFParentInfo> parents = new ArrayList<>();
        if (resp.decoded instanceof Result) {
            Object value = ((Result) resp.decoded).getValue();
            if (value instanceof io.donbee.jade.util.leap.List) {
                io.donbee.jade.util.leap.Iterator it = ((io.donbee.jade.util.leap.List) value).iterator();
                while (it.hasNext()) {
                    Object elt = it.next();
                    if (elt instanceof AID) {
                        parents.add(toParentInfo((AID) elt));
                    } else if (elt instanceof String) {
                        parents.add(toParentInfo(aidFromName((String) elt)));
                    }
                }
            }
        }
        return parents;
    }

    @Override
    public List<DFParentInfo> getDFChildren() {
        // Children are DFs (registered with this DF) that expose the "fipa-df"
        // service type — mirroring the old DF GUI refresh logic.
        List<DFRegistrationInfo> all = listDFRegistrations();
        List<DFParentInfo> children = new ArrayList<>();
        for (DFRegistrationInfo reg : all) {
            if (isADF(reg)) {
                children.add(toParentInfo(reg));
            }
        }
        return children;
    }

    @Override
    public DFParentInfo federateDF(String parentDFName, List<String> parentDFAddresses) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID dfAID = getDefaultDF();
        if (dfAID == null) {
            throw new IllegalStateException("No default DF available on this container");
        }

        AID parentAID = aidFromName(parentDFName);
        if (parentAID.equals(dfAID)) {
            throw new IllegalArgumentException("Self-federation not allowed");
        }
        if (parentDFAddresses != null) {
            for (String addr : parentDFAddresses) {
                parentAID.addAddresses(addr);
            }
        }

        // Reuse this DF's own description for the federation registration
        // (mirrors the old GUI, which federates using getDescriptionOfThisDF()).
        DFAgentDescription dfd = toDFAgentDescription(getDFDescription());

        Federate action = new Federate();
        action.setDf(parentAID);
        action.setDescription(dfd);

        DFRequestAgent.DFResponse resp = DFRequestAgent.executeFull(
            agentManager, impl.getID(), dfAID, action,
            DFAppletOntology.getInstance().getName(),
            DF_TIMEOUT_MS
        );

        ACLMessage reply = resp.message;
        if (reply == null) {
            throw new RuntimeException("No response from DF");
        }
        int perf = reply.getPerformative();
        if (perf == ACLMessage.FAILURE || perf == ACLMessage.REFUSE) {
            throw new RuntimeException("DF federation failed: " + decodeFailure(reply));
        }
        if (perf != ACLMessage.INFORM) {
            throw new RuntimeException("Unexpected reply from DF: " + ACLMessage.getPerformative(perf));
        }
        return toParentInfo(parentAID);
    }

    @Override
    public void deregisterParentDF(String parentDFName) {
        if (agentManager == null) {
            throw new IllegalStateException("Not a Main Container");
        }
        AID dfAID = getDefaultDF();
        if (dfAID == null) {
            throw new IllegalStateException("No default DF available on this container");
        }

        // Resolve the parent's contact addresses from the known parents so the
        // DF can reach it. This mirrors how the DF stores parents when federating.
        List<String> resolved = new ArrayList<>();
        for (DFParentInfo p : getDFParents()) {
            if (p.name != null && p.name.equals(parentDFName)) {
                resolved = p.addresses;
                break;
            }
        }

        AID parentAID = aidFromName(parentDFName);
        if (resolved != null) {
            for (String addr : resolved) {
                parentAID.addAddresses(addr);
            }
        }

        // The description used to identify this DF on the parent = this DF's description.
        DFAgentDescription dfd = toDFAgentDescription(getDFDescription());

        DeregisterFrom action = new DeregisterFrom();
        action.setDf(parentAID);
        action.setDescription(dfd);

        DFRequestAgent.DFResponse resp = DFRequestAgent.executeFull(
            agentManager, impl.getID(), dfAID, action,
            DFAppletOntology.getInstance().getName(),
            DF_TIMEOUT_MS
        );

        ACLMessage reply = resp.message;
        if (reply == null) {
            throw new RuntimeException("No response from DF");
        }
        int perf = reply.getPerformative();
        if (perf == ACLMessage.FAILURE || perf == ACLMessage.REFUSE) {
            throw new RuntimeException("DF deregister-from-parent failed: " + decodeFailure(reply));
        }
        if (perf != ACLMessage.INFORM) {
            throw new RuntimeException("Unexpected reply from DF: " + ACLMessage.getPerformative(perf));
        }
    }

    @Override
    public void deregisterChildDF(String childDFName) {
        // A child DF is simply an agent registered with this DF; removing it
        // is the standard Deregister operation (the DF removes it from its
        // children list as part of DFDeregister when it is itself a DF).
        deregisterFromDF(childDFName);
    }

    private static boolean isADF(DFRegistrationInfo reg) {
        if (reg.services == null) {
            return false;
        }
        for (DFServiceInfo svc : reg.services) {
            if (svc.type != null && svc.type.equalsIgnoreCase("fipa-df")) {
                return true;
            }
        }
        return false;
    }

    private static DFParentInfo toParentInfo(AID aid) {
        List<String> addrs = new ArrayList<>();
        if (aid.getAllAddresses() != null) {
            io.donbee.jade.util.leap.Iterator it = aid.getAllAddresses();
            while (it.hasNext()) {
                addrs.add((String) it.next());
            }
        }
        return new DFParentInfo(aid.getName(), addrs);
    }

    private static DFParentInfo toParentInfo(DFRegistrationInfo reg) {
        List<String> addrs = reg.addresses != null ? new ArrayList<>(reg.addresses) : new ArrayList<>();
        return new DFParentInfo(reg.name, addrs);
    }

    private static DFAgentDescription toDFAgentDescription(DFRegistrationInfo reg) {
        DFAgentDescription dfd = new DFAgentDescription();
        AID aid = new AID();
        aid.setName(reg.name);
        if (reg.addresses != null) {
            for (String addr : reg.addresses) {
                aid.addAddresses(addr);
            }
        }
        dfd.setName(aid);
        if (reg.services != null) {
            for (DFServiceInfo svc : reg.services) {
                ServiceDescription sd = new ServiceDescription();
                if (svc.type != null) {
                    sd.setType(svc.type);
                }
                if (svc.name != null) {
                    sd.setName(svc.name);
                }
                if (svc.ownership != null) {
                    sd.setOwnership(svc.ownership);
                }
                dfd.addServices(sd);
            }
        }
        return dfd;
    }
}
