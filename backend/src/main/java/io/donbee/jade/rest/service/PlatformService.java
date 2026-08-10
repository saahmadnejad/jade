package io.donbee.jade.rest.service;


import java.util.List;

import io.donbee.jade.mtp.MTPDescriptor;

/**
 * Service interface for platform data access.
 * Follows DIP — high-level handlers depend on this abstraction.
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

    AgentInfo deployAgent(String agentName, String className, Object[] args);

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
}
