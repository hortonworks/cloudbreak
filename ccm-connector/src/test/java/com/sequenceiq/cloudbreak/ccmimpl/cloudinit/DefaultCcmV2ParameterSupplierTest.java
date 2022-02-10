package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;

@ExtendWith(MockitoExtension.class)
class DefaultCcmV2ParameterSupplierTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    private static final String TEST_GATEWAY_DOMAIN = "test.gateway.domain";

    private static final String TEST_AGENT_CRN = String.format("crn:cdp:ccmv2:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:agent:%s", TEST_RESOURCE_ID);

    @InjectMocks
    private DefaultCcmV2ParameterSupplier underTest;

    @Mock
    private CcmV2ManagementClient ccmV2Client;

    @Test
    void testGetCcmV2Parameter() {
        setupRegisterInvertingProxyDetails();

        CcmV2Parameters resultParameters = underTest.getCcmV2Parameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), TEST_GATEWAY_DOMAIN,
                Crn.fromString(TEST_CLUSTER_CRN).getResource());
        assertResult(resultParameters);
        verify(ccmV2Client).listInvertingProxyAgents(anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)));
        verify(ccmV2Client).registerInvertingProxyAgent(
                anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)), eq(TEST_GATEWAY_DOMAIN), eq(TEST_RESOURCE_ID));
        verify(ccmV2Client, never()).deregisterInvertingProxyAgent(any(), any());
    }

    @Test
    void unregisterAgentIsCalledWhenExisted() {
        setupRegisterInvertingProxyDetails();
        when(ccmV2Client.listInvertingProxyAgents(any(), any(), any()))
                .thenReturn(List.of(InvertingProxyAgent.newBuilder().setAgentCrn(TEST_AGENT_CRN).build()));

        CcmV2Parameters resultParameters = underTest.getCcmV2Parameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), TEST_GATEWAY_DOMAIN,
                Crn.fromString(TEST_CLUSTER_CRN).getResource());

        assertResult(resultParameters);

        verify(ccmV2Client).listInvertingProxyAgents(anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)));
        verify(ccmV2Client).registerInvertingProxyAgent(
                anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)), eq(TEST_GATEWAY_DOMAIN), eq(TEST_RESOURCE_ID));
        verify(ccmV2Client).deregisterInvertingProxyAgent(any(), eq(TEST_AGENT_CRN));
    }

    @Test
    void unregisterAgentRethrows() {
        setupRegisterInvertingProxyDetails();
        when(ccmV2Client.listInvertingProxyAgents(any(), any(), any()))
                .thenThrow(new CcmV2Exception("internal error"));

        assertThatThrownBy(() -> underTest.getCcmV2Parameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), TEST_GATEWAY_DOMAIN,
                Crn.fromString(TEST_CLUSTER_CRN).getResource())).hasMessageNotContaining("internal error").isInstanceOf(CcmV2Exception.class);

        verify(ccmV2Client).listInvertingProxyAgents(anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)));
        verify(ccmV2Client, never()).registerInvertingProxyAgent(
                anyString(), eq(TEST_ACCOUNT_ID), eq(Optional.of(TEST_ENVIRONMENT_CRN)), eq(TEST_GATEWAY_DOMAIN), eq(TEST_RESOURCE_ID));
        verify(ccmV2Client, never()).deregisterInvertingProxyAgent(any(), eq(TEST_AGENT_CRN));
    }

    private void setupRegisterInvertingProxyDetails() {
        InvertingProxy mockInvertingProxy = InvertingProxy.newBuilder()
                .setHostname("invertingProxyHost")
                .setCertificate("invertingProxyCertificate")
                .build();
        InvertingProxyAgent mockInvertingProxyAgent = InvertingProxyAgent.newBuilder()
                .setAgentCrn("invertingProxyAgentCrn")
                .setEnvironmentCrn(TEST_ENVIRONMENT_CRN)
                .setCertificate("invertingProxyAgentCertificate")
                .setEncipheredPrivateKey("invertingProxyAgentEncipheredKey")
                .build();
        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString(), anyString())).thenReturn(mockInvertingProxy);
        lenient().when(ccmV2Client.registerInvertingProxyAgent(anyString(), anyString(), any(Optional.class), anyString(), anyString()))
                .thenReturn(mockInvertingProxyAgent);
    }

    private void assertResult(CcmV2Parameters resultParameters) {
        assertNotNull(resultParameters, "CCMV2 Parameters should not be null");

        assertEquals("invertingProxyAgentCrn", resultParameters.getAgentCrn(), "AgentCRN should match");
        assertEquals("invertingProxyHost", resultParameters.getInvertingProxyHost(), "InvertingProxyHost should match");
        assertEquals("invertingProxyCertificate", resultParameters.getInvertingProxyCertificate(), "InvertingProxyCertificate should match");
        assertEquals("invertingProxyAgentCertificate", resultParameters.getAgentCertificate(), "AgentCertificate should match");
        assertEquals("invertingProxyAgentEncipheredKey", resultParameters.getAgentEncipheredPrivateKey(), "AgentEncipheredPrivateKey should match");
        assertEquals(TEST_RESOURCE_ID, resultParameters.getAgentKeyId(), "AgentKeyId should match");
    }
}
