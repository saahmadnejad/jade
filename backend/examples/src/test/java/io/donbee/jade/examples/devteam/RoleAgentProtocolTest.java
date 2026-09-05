package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;

import io.donbee.jade.domain.FIPANames;
import org.junit.Test;

/**
 * Conformance tests for the peer-to-peer message constants used by
 * {@code RoleAgent#notifyPeer}.
 *
 * <p>Guards against protocol-name regressions: FIPA protocol identifiers are
 * case-sensitive, so an uppercase {@code "FIPA_REQUEST"} literal is
 * non-conformant even though it looks correct to a human reader.</p>
 */
public class RoleAgentProtocolTest {

    @Test
    public void Given_PeerProtocolConstant_When_Resolved_Then_MatchesFipaRequestSpecValue() {
        assertThat(RoleAgent.PEER_PROTOCOL)
            .isEqualTo(FIPANames.InteractionProtocol.FIPA_REQUEST)
            .isEqualTo("fipa-request")
            .isNotEqualTo("FIPA_REQUEST")
            .isLowerCase();
    }

    @Test
    public void Given_ManagerAndPeerLegs_When_Compared_Then_UseSameProtocolValue() {
        // ManagerAgent sends tasks with the same spec constant; both legs of
        // the conversation must agree on the exact wire value.
        assertThat(RoleAgent.PEER_PROTOCOL).isEqualTo(ManagerAgent.TASK_PROTOCOL);
    }

    @Test
    public void Given_PeerConversationPrefix_When_Used_Then_ManagerCanRoutePeerTrafficAway() {
        // ManagerAgent accepts only "dt-" task conversations and drops
        // "dt-peer-" ones; the prefixes must stay distinct to keep peer
        // chatter out of the task FSM.
        assertThat(RoleAgent.PEER_CONVERSATION_PREFIX)
            .startsWith("dt-")
            .isEqualTo("dt-peer-");
    }
}
