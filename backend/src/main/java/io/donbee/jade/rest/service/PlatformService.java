package io.donbee.jade.rest.service;


import java.util.List;

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
}
