package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcCcmV2Config;

@ExtendWith(MockitoExtension.class)
class CcmV2ManagementClientTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_REQUEST_ID = "requestId";

    private static final String TEST_ENVIRONMENT_CRN = "environmentCrn";

    private static final String TEST_USER_CRN = "testUserCrn";

    private static final String TEST_AGENT_CRN = "testAgentCrn";

    @InjectMocks
    private CcmV2ManagementClient underTest;

    @Mock
    private GrpcCcmV2Client grpcCcmV2Client;

    @Mock
    private GrpcCcmV2Config grpcCcmV2Config;

    @BeforeEach
    void setUp() {
        when(grpcCcmV2Config.getPollingIntervalMs()).thenReturn(10);
        when(grpcCcmV2Config.getTimeoutMs()).thenReturn(100);
    }

    @Test
    void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsReady() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.READY).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        InvertingProxy retrievedProxy = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID));
        assertEquals(InvertingProxy.Status.READY, retrievedProxy.getStatus(), "Inverting Proxy Status should be ready.");
    }

    @Test
    void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsCreating() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.CREATING).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsFailed() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.FAILED).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testRegisterInvertingProxyAgent() {
        String domain = "test.domain";
        String keyId = "keyId";

        InvertingProxyAgent mockAgent = InvertingProxyAgent.newBuilder().setAgentCrn("testAgentCrn").build();
        when(grpcCcmV2Client.registerAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId, TEST_USER_CRN))
                .thenReturn(mockAgent);
        InvertingProxyAgent registeredAgent =
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                        () -> underTest.registerInvertingProxyAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId));
        assertEquals(TEST_AGENT_CRN, registeredAgent.getAgentCrn(), "InvertingProxyAgent agentCrn  should match.");
    }

    @Test
    void testRegisterInvertingProxyAgentWhenException() {
        String domain = "test.domain";
        String keyId = "keyId";

        when(grpcCcmV2Client.registerAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId, TEST_USER_CRN))
                .thenThrow(new RuntimeException());
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.registerInvertingProxyAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testUnRegisterAgent() {
        UnregisterAgentResponse mockResponse = UnregisterAgentResponse.newBuilder().build();
        when(grpcCcmV2Client.unRegisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN, TEST_USER_CRN)).thenReturn(mockResponse);
        UnregisterAgentResponse unregisterAgentResponse = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.deregisterInvertingProxyAgent(TEST_REQUEST_ID, TEST_AGENT_CRN));
        assertEquals(unregisterAgentResponse, mockResponse, "UnregisterAgentResponse should match.");
    }

    @Test
    void testUnRegisterAgentWhenException() {
        when(grpcCcmV2Client.unRegisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN, TEST_USER_CRN)).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.deregisterInvertingProxyAgent(TEST_REQUEST_ID, TEST_AGENT_CRN)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testListAgents() {
        List<InvertingProxyAgent> mockResponse = List.of(InvertingProxyAgent.newBuilder().build());
        when(grpcCcmV2Client.listAgents(TEST_REQUEST_ID, TEST_USER_CRN, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN))).thenReturn(mockResponse);
        List<InvertingProxyAgent> listAgentsResponse = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.listInvertingProxyAgents(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN)));
        assertEquals(listAgentsResponse, mockResponse, "List Agents Response should match.");
    }

    @Test
    void testListAgentsWhenException() {
        when(grpcCcmV2Client.listAgents(TEST_REQUEST_ID, TEST_USER_CRN, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN))).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                        () ->  underTest.listInvertingProxyAgents(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN))))
                .isInstanceOf(CcmV2Exception.class);
    }
}
