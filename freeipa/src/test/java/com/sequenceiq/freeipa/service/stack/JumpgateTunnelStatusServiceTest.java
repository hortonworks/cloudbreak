package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityProto.TunnelStatus;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.WorkloadConnectivityAdminClient;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class JumpgateTunnelStatusServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:envId";

    private static final String AGENT_CRN = "crn:altus:ccmv2:us-west-1:accountId:agent:agentId";

    private static final String OTHER_AGENT_CRN = "crn:altus:ccmv2:us-west-1:accountId:agent:otherAgentId";

    private static final String INSTANCE_ID = "i-1234";

    private static final String OTHER_INSTANCE_ID = "i-5678";

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @Mock
    private WorkloadConnectivityAdminClient workloadConnectivityAdminClient;

    @InjectMocks
    private JumpgateTunnelStatusService underTest;

    @BeforeEach
    void setUp() {
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(JumpgateTunnelStatusService.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(JumpgateTunnelStatusService.class)).detachAppender(logAppender);
    }

    @Test
    void testLogTunnelStatusSkippedWhenTunnelIsNull() {
        Stack stack = createStack(null, AGENT_CRN);

        underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID)));

        verifyNoInteractions(workloadConnectivityAdminClient);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "CCMV2_JUMPGATE", mode = Mode.EXCLUDE)
    void testLogTunnelStatusSkippedWhenTunnelIsNotJumpgate(Tunnel tunnel) {
        Stack stack = createStack(tunnel, AGENT_CRN);

        underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID)));

        verifyNoInteractions(workloadConnectivityAdminClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testLogTunnelStatusSkippedWhenAgentCrnIsBlank(String agentCrn) {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, agentCrn);

        underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID)));

        verifyNoInteractions(workloadConnectivityAdminClient);
    }

    @Test
    void testLogTunnelStatusWhenTunnelFoundByBackendId() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        TunnelStatus tunnelStatus = createTunnelStatus(AGENT_CRN + "-" + INSTANCE_ID, "");
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(createResponse(tunnelStatus));

        assertDoesNotThrow(() -> underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID))));

        verify(workloadConnectivityAdminClient).listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);
        assertEquals(List.of("Jumpgate reports 1 tunnel(s) for agent " + AGENT_CRN + " (1 in environment)",
                "Jumpgate tunnel status: instance i-1234 last seen 0s ago, agent jumpgate-agent/1.0, relay jumpgate-relay-0"),
                logMessages());
    }

    @Test
    void testLogTunnelStatusWhenTunnelFoundByInstanceId() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        TunnelStatus tunnelStatus = createTunnelStatus(AGENT_CRN, INSTANCE_ID);
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(createResponse(tunnelStatus));

        assertDoesNotThrow(() -> underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID))));

        verify(workloadConnectivityAdminClient).listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);
        assertEquals(List.of("Jumpgate reports 1 tunnel(s) for agent " + AGENT_CRN + " (1 in environment)",
                "Jumpgate tunnel status: instance i-1234 last seen 0s ago, agent jumpgate-agent/1.0, relay jumpgate-relay-0"),
                logMessages());
    }

    @Test
    void testLogTunnelStatusIgnoresTunnelOfOtherAgentWithSameInstanceId() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        TunnelStatus tunnelStatus = createTunnelStatus(OTHER_AGENT_CRN, INSTANCE_ID).toBuilder().setAgentCrn(OTHER_AGENT_CRN).build();
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(createResponse(tunnelStatus));

        underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID)));

        assertEquals(List.of("Jumpgate reports 0 tunnel(s) for agent " + AGENT_CRN + " (1 in environment)",
                "Jumpgate tunnel status: instance i-1234 has no tunnel registered"), logMessages());
    }

    @Test
    void testLogTunnelStatusWhenTimestampIsMissing() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        TunnelStatus tunnelStatus = createTunnelStatus(AGENT_CRN + "-" + INSTANCE_ID, INSTANCE_ID).toBuilder().setTimestamp(0L).build();
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(createResponse(tunnelStatus));

        underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID)));

        assertEquals(List.of("Jumpgate reports 1 tunnel(s) for agent " + AGENT_CRN + " (1 in environment)",
                "Jumpgate tunnel status: instance i-1234 last seen unknown, agent jumpgate-agent/1.0, relay jumpgate-relay-0"),
                logMessages());
    }

    @Test
    void testLogTunnelStatusWhenNoTunnelRegisteredForInstance() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        TunnelStatus tunnelStatus = createTunnelStatus(AGENT_CRN + "-" + OTHER_INSTANCE_ID, OTHER_INSTANCE_ID);
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(createResponse(tunnelStatus));

        assertDoesNotThrow(() -> underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID))));

        verify(workloadConnectivityAdminClient).listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);
        assertEquals(List.of("Jumpgate reports 1 tunnel(s) for agent " + AGENT_CRN + " (1 in environment)",
                "Jumpgate tunnel status: instance i-1234 has no tunnel registered"), logMessages());
    }

    @Test
    void testLogTunnelStatusWhenResponseIsEmpty() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenReturn(ListTunnelStatusResponse.getDefaultInstance());

        assertDoesNotThrow(() -> underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID), createInstance(OTHER_INSTANCE_ID))));

        verify(workloadConnectivityAdminClient).listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);
        assertEquals(Set.of("Jumpgate reports 0 tunnel(s) for agent " + AGENT_CRN + " (0 in environment)",
                "Jumpgate tunnel status: instance i-1234 has no tunnel registered",
                "Jumpgate tunnel status: instance i-5678 has no tunnel registered"), Set.copyOf(logMessages()));
    }

    @Test
    void testLogTunnelStatusDoesNotThrowWhenClientFails() {
        Stack stack = createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN);
        when(workloadConnectivityAdminClient.listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN)).thenThrow(new RuntimeException("UNAVAILABLE"));

        assertDoesNotThrow(() -> underTest.logTunnelStatus(stack, Set.of(createInstance(INSTANCE_ID))));

        verify(workloadConnectivityAdminClient).listTunnelStatus(ACCOUNT_ID, ENVIRONMENT_CRN);
        assertEquals(List.of("Could not get jumpgate tunnel status for stack crn:cdp:freeipa:us-west-1:accountId:freeipa:stackId"), logMessages());
    }

    @Test
    void testLogTunnelStatusSkippedWhenNoInstances() {
        underTest.logTunnelStatus(createStack(Tunnel.CCMV2_JUMPGATE, AGENT_CRN), Set.of());

        verifyNoInteractions(workloadConnectivityAdminClient);
    }

    private Stack createStack(Tunnel tunnel, String agentCrn) {
        Stack stack = new Stack();
        stack.setTunnel(tunnel);
        stack.setCcmV2AgentCrn(agentCrn);
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setResourceCrn("crn:cdp:freeipa:us-west-1:accountId:freeipa:stackId");
        return stack;
    }

    private InstanceMetaData createInstance(String instanceId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        return instanceMetaData;
    }

    private TunnelStatus createTunnelStatus(String id, String instanceId) {
        Instant now = Instant.now();
        return TunnelStatus.newBuilder()
                .setId(id)
                .setInstanceId(instanceId)
                .setAgentCrn(AGENT_CRN)
                .setUserAgent("jumpgate-agent/1.0")
                .setRelayAddress("jumpgate-relay-0")
                .setTimestamp(now.getEpochSecond() * 1_000_000_000L + now.getNano())
                .build();
    }

    private List<String> logMessages() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private ListTunnelStatusResponse createResponse(TunnelStatus... tunnelStatuses) {
        return ListTunnelStatusResponse.newBuilder()
                .addAllTunnels(List.of(tunnelStatuses))
                .build();
    }
}
