package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy.Status;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;

@ExtendWith(MockitoExtension.class)
class CcmV2ClientTest {

    private static final String TEST_REQUEST_ID = "requestId";

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:ausername";

    private static final String TEST_ENVIRONMENT_CRN = "environmentCrn";

    private static final String TEST_HMAC_KEY = "testHmacKey";

    private static final String TEST_AGENT_CRN = "testAgentCrn";

    @Mock
    private GrpcCcmV2Client grpcCcmV2Client;

    @InjectMocks
    private CcmV2Client underTest;

    @ParameterizedTest
    @EnumSource(value = Status.class, names = { "CREATING", "FAILED" }, mode = Mode.INCLUDE)
    void getOrCreateInvertingProxyFail(Status status) {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(status).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void getOrCreateInvertingProxySuccess() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(Status.READY).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        InvertingProxy result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID));
        assertEquals(Status.READY, result.getStatus(), "Inverting Proxy Status should be ready.");
    }

    @Test
    void registerAgent() {
        String domain = "test.domain";
        String keyId = "keyId";

        InvertingProxyAgent mockAgent = InvertingProxyAgent.newBuilder().setAgentCrn("testAgentCrn").build();
        when(grpcCcmV2Client.registerAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId,
                TEST_USER_CRN, Optional.of(TEST_HMAC_KEY))).thenReturn(mockAgent);
        InvertingProxyAgent registeredAgent = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.registerAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId, Optional.of(TEST_HMAC_KEY)));
        assertEquals(TEST_AGENT_CRN, registeredAgent.getAgentCrn(), "InvertingProxyAgent agentCrn should match.");
    }

    @Test
    void deregisterAgent() {
        UnregisterAgentResponse mockResponse = UnregisterAgentResponse.newBuilder().build();
        when(grpcCcmV2Client.unRegisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN, TEST_USER_CRN)).thenReturn(mockResponse);
        UnregisterAgentResponse unregisterAgentResponse = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.deregisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN));
        assertEquals(unregisterAgentResponse, mockResponse, "UnregisterAgentResponse should match.");
    }

    @Test
    void listAgents() {
        List<InvertingProxyAgent> mockResponse = List.of(InvertingProxyAgent.newBuilder().build());
        when(grpcCcmV2Client.listAgents(TEST_REQUEST_ID, TEST_USER_CRN, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN))).thenReturn(mockResponse);
        List<InvertingProxyAgent> listAgentsResponse = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.listAgents(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN)));
        assertEquals(listAgentsResponse, mockResponse, "List Agents Response should match.");
    }
}
