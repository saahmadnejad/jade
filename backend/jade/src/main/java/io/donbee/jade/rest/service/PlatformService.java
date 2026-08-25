package io.donbee.jade.rest.service;


import java.util.List;

import io.donbee.jade.mtp.MTPDescriptor;

/**
 * Service interface for platform data access.
 * Follows DIP — high-level handlers depend on this abstraction.
 *
 * <p><b>Old GUI implementation:</b> The concrete methods in
 * {@code JadesPlatformService} replace the callback methods in
 * {@code io.donbee.jade.tools.rma.rma} (lines 468–1062), which was the
 * central agent behind the old Swing RMA GUI. Each method here maps
 * to a corresponding {@code rma.xxx()} method that sent FIPA management
 * actions (via {@code AMSClientBehaviour}) to the AMS.</p>
 */
public interface PlatformService {

    boolean isMainContainer();

    PlatformInfo getPlatformInfo();

    List<AgentInfo> getAgents(boolean detail);

    AgentInfo getAgent(String agentName);

    List<ContainerInfo> getContainers();

    ContainerInfo getContainer(String name);

    void killAgent(String agentName);

    void suspendAgent(String agentName);

    void resumeAgent(String agentName);

    void shutdownPlatform();

    void killContainer(String containerName);

    void saveContainer(String containerName, String repository);

    void loadContainer(String containerName, String repository);

    MTPDescriptor installMTP(String containerName, String address, String className);

    void uninstallMTP(String containerName, String address);

    java.util.List<MTPInfo> getMTPs(String containerName);

    AgentInfo freezeAgent(String agentName, String bufferContainer, String repository);

    AgentInfo thawAgent(String agentName, String targetContainer, String repository);

    AgentInfo cloneAgent(String agentName, String newName, String targetContainer);

    void moveAgent(String agentName, String targetContainer);

    void saveAgent(String agentName, String repository);

    AgentInfo loadAgent(String agentName, String targetContainer, String repository);

    void changeAgentOwnership(String agentName, String newOwner);

    void registerRemoteAgent(String aidName, String[] addresses);

    AgentInfo deployAgent(String agentName, String className, Object[] args);

    /**
     * Deploy an agent into a specific container. Used by the scenarios API to
     * place scenario agents into their dedicated instance container.
     *
     * @param targetContainer container name; when null the Main Container is used
     */
    default AgentInfo deployAgent(String agentName, String className, Object[] args, String targetContainer) {
        if (targetContainer == null) {
            return deployAgent(agentName, className, args);
        }
        throw new UnsupportedOperationException("Container-targeted deployment not supported");
    }

    java.util.List<RemotePlatformInfo> getRemotePlatforms();

    RemotePlatformInfo addRemotePlatform(String amsName, String[] addresses);

    java.util.List<AgentInfo> searchRemotePlatformAgents(String platformName);

    RemotePlatformInfo getRemotePlatformDescription(String platformName);

    void removeRemotePlatform(String platformName);

    RemotePlatformInfo fetchRemotePlatform(String url);

    class PlatformInfo {
        public final String platformID;
        public final String containerName;
        public final boolean isMain;
        public final String ams;
        public final String defaultDF;

        public PlatformInfo(String platformID, String containerName, boolean isMain, String ams, String defaultDF) {
            this.platformID = platformID;
            this.containerName = containerName;
            this.isMain = isMain;
            this.ams = ams;
            this.defaultDF = defaultDF;
        }
    }

    class AgentInfo {
        public final String name;
        public final String state;
        public final String ownership;
        public final String container;
        public final String[] addresses;

        public AgentInfo(String name, String state, String ownership, String container, String[] addresses) {
            this.name = name;
            this.state = state;
            this.ownership = ownership;
            this.container = container;
            this.addresses = addresses;
        }
    }

    class ContainerInfo {
        public final String name;
        public final String address;
        public final String port;
        public final boolean isMain;

        public ContainerInfo(String name, String address, String port, boolean isMain) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.isMain = isMain;
        }
    }

    class MTPInfo {
        public final String address;
        public final String className;

        public MTPInfo(String address, String className) {
            this.address = address;
            this.className = className;
        }
    }

    class RemotePlatformInfo {
        public final String name;
        public final String ams;
        public final String[] addresses;
        public final String[] services;

        public RemotePlatformInfo(String name, String ams, String[] addresses, String[] services) {
            this.name = name;
            this.ams = ams;
            this.addresses = addresses;
            this.services = services;
        }
    }
}
