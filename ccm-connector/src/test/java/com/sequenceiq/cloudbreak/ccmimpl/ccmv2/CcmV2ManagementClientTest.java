package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.UnregisterAgentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcCcmV2Config;

@RunWith(MockitoJUnitRunner.class)
public class CcmV2ManagementClientTest {

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

    @Before
    public void setUp() {
        when(grpcCcmV2Config.getPollingIntervalMs()).thenReturn(10);
        when(grpcCcmV2Config.getTimeoutMs()).thenReturn(100);
    }

    @Test
    public void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsReady() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.READY).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        InvertingProxy retrievedProxy = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID));
        assertEquals(InvertingProxy.Status.READY, retrievedProxy.getStatus(), "Inverting Proxy Status should be ready.");
    }

    @Test(expected = CcmV2Exception.class)
    public void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsCreating() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.CREATING).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID));
    }

    @Test(expected = CcmV2Exception.class)
    public void testAwaitReadyInvertingProxyForAccountWhenInvertingProxyIsFailed() {
        InvertingProxy invertingProxy = InvertingProxy.newBuilder().setStatus(InvertingProxy.Status.FAILED).build();
        when(grpcCcmV2Client.getOrCreateInvertingProxy(TEST_REQUEST_ID, TEST_ACCOUNT_ID, TEST_USER_CRN)).thenReturn(invertingProxy);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.awaitReadyInvertingProxyForAccount(TEST_REQUEST_ID, TEST_ACCOUNT_ID));
    }

    @Test
    public void testRegisterInvertingProxyAgent() {
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

    @Test(expected = CcmV2Exception.class)
    public void testRegisterInvertingProxyAgentWhenException() {
        String domain = "test.domain";
        String keyId = "keyId";

        when(grpcCcmV2Client.registerAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId, TEST_USER_CRN))
                .thenThrow(new RuntimeException());
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.registerInvertingProxyAgent(TEST_REQUEST_ID, TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), domain, keyId));
    }

    @Test
    public void testUnRegisterAgent() {
        UnregisterAgentResponse mockResponse = UnregisterAgentResponse.newBuilder().build();
        when(grpcCcmV2Client.unRegisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN, TEST_USER_CRN)).thenReturn(mockResponse);
        UnregisterAgentResponse unregisterAgentResponse = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.deregisterInvertingProxyAgent(TEST_REQUEST_ID, TEST_AGENT_CRN));
        assertEquals(unregisterAgentResponse, mockResponse, "UnregisterAgentResponse should match.");
    }

    @Test(expected = CcmV2Exception.class)
    public void testUnRegisterAgentWhenException() {
        when(grpcCcmV2Client.unRegisterAgent(TEST_REQUEST_ID, TEST_AGENT_CRN, TEST_USER_CRN)).thenThrow(new RuntimeException());
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.deregisterInvertingProxyAgent(TEST_REQUEST_ID, TEST_AGENT_CRN));
    }
}
