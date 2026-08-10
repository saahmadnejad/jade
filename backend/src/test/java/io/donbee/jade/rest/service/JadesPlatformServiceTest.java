package io.donbee.jade.rest.service;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ContainerID;
import io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JadesPlatformServiceTest {

    @Mock
    private AgentContainer mockImpl;

    @Mock
    private AgentManager mockAgentManager;

    @Mock
    private ContainerID mockContainerID;

    private JadesPlatformService service;

    @Before
    public void setUp() {
        service = new JadesPlatformService(mockImpl, mockAgentManager);
        when(mockImpl.getID()).thenReturn(mockContainerID);
        when(mockContainerID.getName()).thenReturn("Main-Container");
    }

    // ===== isMainContainer =====

    @Test
    public void Given_AgentManagerIsNull_When_IsMainContainer_Then_ReturnsFalse() {
        // Arrange
        JadesPlatformService nullService = new JadesPlatformService(mockImpl, null);

        // Act
        boolean result = nullService.isMainContainer();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void Given_AgentManagerIsNotNull_When_IsMainContainer_Then_ReturnsTrue() {
        // Arrange (mockAgentManager is not null)

        // Act
        boolean result = service.isMainContainer();

        // Assert
        assertThat(result).isTrue();
    }

    // ===== getAgents (non-detail) =====

    @Test
    public void Given_MainContainer_When_GetAgentsWithoutDetail_Then_ReturnsAgentNamesOnly() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getAllAddresses()).thenReturn(mock(io.donbee.jade.util.leap.Iterator.class));

        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act
        java.util.List<PlatformService.AgentInfo> agents = service.getAgents(false);

        // Assert
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).name).isEqualTo("ams@127.0.0.1:1099/JADE");
        assertThat(agents.get(0).state).isNull();
        assertThat(agents.get(0).ownership).isNull();
        assertThat(agents.get(0).container).isEqualTo("Main-Container");
    }

    @Test
    public void Given_NonMainContainer_When_GetAgents_Then_ReturnsEmptyList() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act
        java.util.List<PlatformService.AgentInfo> agents = nonMainService.getAgents(false);

        // Assert
        assertThat(agents).isEmpty();
    }

    // ===== getAgents (with detail) =====

    @Test
    public void Given_MainContainer_When_GetAgentsWithDetail_Then_ReturnsStateAndOwnership() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");
        when(aid1.getAllAddresses()).thenReturn(mock(io.donbee.jade.util.leap.Iterator.class));

        AMSAgentDescription amsDesc = mock(AMSAgentDescription.class);
        when(amsDesc.getState()).thenReturn("active");
        when(amsDesc.getOwnership()).thenReturn("init");
        when(mockAgentManager.getAMSDescription(aid1)).thenReturn(amsDesc);

        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act
        java.util.List<PlatformService.AgentInfo> agents = service.getAgents(true);

        // Assert
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).name).isEqualTo("ams@127.0.0.1:1099/JADE");
        assertThat(agents.get(0).state).isEqualTo("active");
        assertThat(agents.get(0).ownership).isEqualTo("init");
        assertThat(agents.get(0).container).isEqualTo("Main-Container");
        assertThat(agents.get(0).addresses).isEmpty();
    }

    @Test
    public void Given_MainContainer_When_GetAgentsWithDetailAndNoAMSDesc_Then_UsesUnknownState() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("test@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("test");
        when(aid1.getAllAddresses()).thenReturn(mock(io.donbee.jade.util.leap.Iterator.class));
        when(mockAgentManager.getAMSDescription(aid1)).thenThrow(new RuntimeException("not found"));

        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act
        java.util.List<PlatformService.AgentInfo> agents = service.getAgents(true);

        // Assert
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).state).isEqualTo("UNKNOWN");
        assertThat(agents.get(0).ownership).isEqualTo("");
    }

    // ===== getAgent =====

    @Test
    public void Given_AgentExists_When_GetAgentByName_Then_ReturnsAgentInfo() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");
        when(aid1.getAllAddresses()).thenReturn(mock(io.donbee.jade.util.leap.Iterator.class));

        AMSAgentDescription amsDesc = mock(AMSAgentDescription.class);
        when(amsDesc.getState()).thenReturn("active");
        when(amsDesc.getOwnership()).thenReturn("init");
        when(mockAgentManager.getAMSDescription(aid1)).thenReturn(amsDesc);

        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act
        PlatformService.AgentInfo result = service.getAgent("ams");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("ams@127.0.0.1:1099/JADE");
        assertThat(result.state).isEqualTo("active");
    }

    @Test
    public void Given_AgentDoesNotExist_When_GetAgentByName_Then_ReturnsNull() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");

        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(0);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act
        PlatformService.AgentInfo result = service.getAgent("nonexistent");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    public void Given_NonMainContainer_When_GetAgent_Then_ReturnsNull() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act
        PlatformService.AgentInfo result = nonMainService.getAgent("any");

        // Assert
        assertThat(result).isNull();
    }

    // ===== killAgent =====

    @Test
    public void Given_AgentExists_When_KillAgent_Then_AgentIsKilled() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("test-agent@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("test-agent");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);
        doNothing().when(mockAgentManager).kill(any(AID.class), any(), any());

        // Act
        service.killAgent("test-agent");

        // Assert
        verify(mockAgentManager).kill(eq(aid1), any(), any());
    }

    @Test
    public void Given_AgentDoesNotExist_When_KillAgent_Then_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act & Assert
        assertThatThrownBy(() -> service.killAgent("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Agent not found");
    }

    @Test
    public void Given_NonMainContainer_When_KillAgent_Then_ThrowsIllegalStateException() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act & Assert
        assertThatThrownBy(() -> nonMainService.killAgent("any"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not a Main Container");
    }

    // ===== suspendAgent =====

    @Test
    public void Given_AgentExists_When_SuspendAgent_Then_AgentIsSuspended() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("test-agent@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("test-agent");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);
        doNothing().when(mockAgentManager).suspend(any(AID.class));

        // Act
        service.suspendAgent("test-agent");

        // Assert
        verify(mockAgentManager).suspend(eq(aid1));
    }

    @Test
    public void Given_AgentDoesNotExist_When_SuspendAgent_Then_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act & Assert
        assertThatThrownBy(() -> service.suspendAgent("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Agent not found");
    }

    @Test
    public void Given_NonMainContainer_When_SuspendAgent_Then_ThrowsIllegalStateException() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act & Assert
        assertThatThrownBy(() -> nonMainService.suspendAgent("any"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not a Main Container");
    }

    // ===== resumeAgent =====

    @Test
    public void Given_AgentExists_When_ResumeAgent_Then_AgentIsResumed() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("test-agent@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("test-agent");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);
        doNothing().when(mockAgentManager).activate(any(AID.class));

        // Act
        service.resumeAgent("test-agent");

        // Assert
        verify(mockAgentManager).activate(eq(aid1));
    }

    @Test
    public void Given_AgentDoesNotExist_When_ResumeAgent_Then_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        AID aid1 = mock(AID.class);
        when(aid1.getName()).thenReturn("ams@127.0.0.1:1099/JADE");
        when(aid1.getLocalName()).thenReturn("ams");
        io.donbee.jade.util.leap.List mockAgentList = mock(io.donbee.jade.util.leap.List.class);
        when(mockAgentList.size()).thenReturn(1);
        when(mockAgentList.get(0)).thenReturn(aid1);
        when(mockAgentManager.containerAgents(mockContainerID)).thenReturn(mockAgentList);

        // Act & Assert
        assertThatThrownBy(() -> service.resumeAgent("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Agent not found");
    }

    // ===== getContainers =====

    @Test
    public void Given_MainContainer_When_GetContainers_Then_ReturnsContainerList() throws Exception {
        // Arrange
        ContainerID cid1 = mock(ContainerID.class);
        when(cid1.getName()).thenReturn("Main-Container");
        when(cid1.getAddress()).thenReturn("127.0.0.1");
        when(cid1.getPort()).thenReturn("1099");

        ContainerID cid2 = mock(ContainerID.class);
        when(cid2.getName()).thenReturn("Node1");
        when(cid2.getAddress()).thenReturn("127.0.0.1");
        when(cid2.getPort()).thenReturn("1098");

        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{cid1, cid2});
        when(mockImpl.here()).thenReturn(cid1);

        // Act
        java.util.List<PlatformService.ContainerInfo> containers = service.getContainers();

        // Assert
        assertThat(containers).hasSize(2);
        assertThat(containers.get(0).name).isEqualTo("Main-Container");
        assertThat(containers.get(0).isMain).isTrue();
        assertThat(containers.get(1).name).isEqualTo("Node1");
        assertThat(containers.get(1).isMain).isFalse();
    }

    @Test
    public void Given_NonMainContainer_When_GetContainers_Then_ReturnsEmptyList() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act
        java.util.List<PlatformService.ContainerInfo> containers = nonMainService.getContainers();

        // Assert
        assertThat(containers).isEmpty();
    }

    // ===== getContainer =====

    @Test
    public void Given_ContainerExists_When_GetContainer_Then_ReturnsContainerInfo() throws Exception {
        // Arrange
        ContainerID cid1 = mock(ContainerID.class);
        when(cid1.getName()).thenReturn("Main-Container");

        ContainerID cid2 = mock(ContainerID.class);
        when(cid2.getName()).thenReturn("Node1");
        when(cid2.getAddress()).thenReturn("127.0.0.1");
        when(cid2.getPort()).thenReturn("1098");

        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{cid1, cid2});
        when(mockImpl.here()).thenReturn(cid1);

        // Act
        PlatformService.ContainerInfo result = service.getContainer("Node1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Node1");
        assertThat(result.address).isEqualTo("127.0.0.1");
        assertThat(result.port).isEqualTo("1098");
        assertThat(result.isMain).isFalse();
    }

    @Test
    public void Given_ContainerDoesNotExist_When_GetContainer_Then_ReturnsNull() throws Exception {
        // Arrange
        ContainerID cid1 = mock(ContainerID.class);
        when(cid1.getName()).thenReturn("Main-Container");

        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{cid1});
        when(mockImpl.here()).thenReturn(cid1);

        // Act
        PlatformService.ContainerInfo result = service.getContainer("NonExistent");

        // Assert
        assertThat(result).isNull();
    }

    // ===== shutdownPlatform =====

    @Test
    public void Given_MainContainer_When_ShutdownPlatform_Then_CallsAgentManagerShutdown() throws Exception {
        // Arrange
        doNothing().when(mockAgentManager).shutdownPlatform(any(), any());

        // Act
        service.shutdownPlatform();

        // Assert
        verify(mockAgentManager).shutdownPlatform(any(), any());
    }

    @Test
    public void Given_NonMainContainer_When_ShutdownPlatform_Then_ThrowsIllegalStateException() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act & Assert
        assertThatThrownBy(() -> nonMainService.shutdownPlatform())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not a Main Container");
    }

    // ===== killContainer =====

    @Test
    public void Given_MainContainer_When_KillContainer_Then_CallsAgentManagerKillContainer() throws Exception {
        // Arrange
        ContainerID localCid = mock(ContainerID.class);
        ContainerID remoteCid = mock(ContainerID.class);
        when(localCid.getName()).thenReturn("Main-Container");
        when(remoteCid.getName()).thenReturn("Node1");
        when(mockImpl.here()).thenReturn(localCid);
        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{localCid, remoteCid});
        doNothing().when(mockAgentManager).killContainer(any(ContainerID.class), any(), any());

        // Act
        service.killContainer("Node1");

        // Assert
        verify(mockAgentManager).killContainer(eq(remoteCid), any(), any());
    }

    @Test
    public void Given_KillMainContainer_When_KillContainer_Then_ThrowsIllegalStateException() {
        // Arrange
        ContainerID localCid = mock(ContainerID.class);
        when(localCid.getName()).thenReturn("Main-Container");
        when(mockImpl.here()).thenReturn(localCid);
        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{localCid});

        // Act & Assert
        assertThatThrownBy(() -> service.killContainer("Main-Container"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot kill the Main Container");
    }

    @Test
    public void Given_UnknownContainer_When_KillContainer_Then_ThrowsIllegalArgumentException() {
        // Arrange
        ContainerID localCid = mock(ContainerID.class);
        when(localCid.getName()).thenReturn("Main-Container");
        when(mockImpl.here()).thenReturn(localCid);
        when(mockAgentManager.containerIDs()).thenReturn(new ContainerID[]{localCid});

        // Act & Assert
        assertThatThrownBy(() -> service.killContainer("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Container not found");
    }

    @Test
    public void Given_NonMainContainer_When_KillContainer_Then_ThrowsIllegalStateException() {
        // Arrange
        JadesPlatformService nonMainService = new JadesPlatformService(mockImpl, null);

        // Act & Assert
        assertThatThrownBy(() -> nonMainService.killContainer("Node1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not a Main Container");
    }
}
