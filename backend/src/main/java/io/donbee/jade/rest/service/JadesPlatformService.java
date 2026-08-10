package io.donbee.jade.rest.service;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ContainerID;
import io.donbee.jade.core.NameClashException;
import io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.content.onto.basic.Action;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.mtp.MTPDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of PlatformService using the JADE backend.
 */
public class JadesPlatformService implements PlatformService {

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
                    aid.getName(),
                    state,
                    ownership,
                    cid.getName(),
                    aid.getAllAddresses() != null ? toArray(aid.getAllAddresses()) : new String[0]
                ));
            } else {
                result.add(new AgentInfo(
                    aid.getName(),
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
                    aid.getName(),
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

    private void sendAMSAction(io.donbee.jade.content.Concept action, String actionLabel) {
        try {
            io.donbee.jade.content.ContentManager cm = new io.donbee.jade.content.ContentManager();
            cm.registerOntology(io.donbee.jade.domain.persistence.PersistenceOntology.getInstance());
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
            new String[0]
        );
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
}
