package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;

@ExtendWith(MockitoExtension.class)
class DefaultCcmV2JumpgateParameterSupplierTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    @InjectMocks
    private DefaultCcmV2JumpgateParameterSupplier underTest;

    @Mock
    private CcmV2ManagementClient ccmV2Client;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testGetCcmV2JumpgateParameters(boolean singleWayTls) {
        String gatewayDomain = "test.gateway.domain";
        InvertingProxy mockInvertingProxy = InvertingProxy.newBuilder()
                .setHostname("invertingProxyHost")
                .setCertificate("invertingProxyCertificate")
                .build();
        InvertingProxyAgent mockInvertingProxyAgent = getInvertingProxyAgent(singleWayTls);

        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString(), anyString())).thenReturn(mockInvertingProxy);
        when(ccmV2Client.registerInvertingProxyAgent(anyString(), anyString(), any(Optional.class), anyString(), anyString()))
                .thenReturn(mockInvertingProxyAgent);

        CcmV2JumpgateParameters resultParameters = underTest.getCcmV2JumpgateParameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), gatewayDomain,
                Crn.fromString(TEST_CLUSTER_CRN).getResource());
        assertNotNull(resultParameters, "CCMV2 Jumpgate Parameters should not be null");

        assertEquals(TEST_ENVIRONMENT_CRN, resultParameters.getEnvironmentCrn(), "EnvironmentCRN should match");
        assertEquals("invertingProxyAgentCrn", resultParameters.getAgentCrn(), "AgentCRN should match");
        assertEquals("invertingProxyHost", resultParameters.getInvertingProxyHost(), "InvertingProxyHost should match");
        assertEquals("invertingProxyCertificate", resultParameters.getInvertingProxyCertificate(), "InvertingProxyCertificate should match");
        assertEquals(TEST_RESOURCE_ID, resultParameters.getAgentKeyId(), "AgentKeyId should match");
        assertAgentCertOrMachineUser(resultParameters, singleWayTls);
    }

    private void assertAgentCertOrMachineUser(CcmV2JumpgateParameters resultParameters, boolean singleWayTls) {
        if (singleWayTls) {
            assertThat(resultParameters.getAgentCertificate())
                    .withFailMessage("AgentCertificate should be null for one-way TLS")
                    .isNullOrEmpty();
            assertThat(resultParameters.getAgentEncipheredPrivateKey())
                    .withFailMessage("AgentEncipheredPrivateKey should be null for one-way TLS")
                    .isNullOrEmpty();
            assertEquals("invertingProxyAgentMachineUserAccessKey",
                    resultParameters.getAgentMachineUserAccessKey(),
                    "AgentMachineUserAccessKey should match");
            assertEquals("invertingProxyAgentMachineUserEncipheredAccessKey",
                    resultParameters.getAgentMachineUserEncipheredAccessKey(),
                    "AgentMachineUserEncipheredAccessKey should match");
        } else {
            assertEquals("invertingProxyAgentCertificate", resultParameters.getAgentCertificate(),
                    "AgentCertificate should match");
            assertEquals("invertingProxyAgentEncipheredKey", resultParameters.getAgentEncipheredPrivateKey(),
                    "AgentEncipheredPrivateKey should match");
            assertThat(resultParameters.getAgentMachineUserAccessKey())
                    .withFailMessage("AgentMachineUserAccessKey should be null for two-way TLS")
                    .isNullOrEmpty();
            assertThat(resultParameters.getAgentMachineUserEncipheredAccessKey())
                    .withFailMessage("AgentMachineUserEncipheredAccessKey should be null for two-way TLS")
                    .isNullOrEmpty();
        }
    }

    private InvertingProxyAgent getInvertingProxyAgent(boolean singleWayTls) {
        InvertingProxyAgent.Builder mockInvertingProxyAgentBuilder = InvertingProxyAgent.newBuilder()
                .setAgentCrn("invertingProxyAgentCrn")
                .setEnvironmentCrn(TEST_ENVIRONMENT_CRN);
        if (singleWayTls) {
            mockInvertingProxyAgentBuilder
                    .setAccessKeyId("invertingProxyAgentMachineUserAccessKey")
                    .setEncipheredAccessKey("invertingProxyAgentMachineUserEncipheredAccessKey");
        } else {
            mockInvertingProxyAgentBuilder
                    .setCertificate("invertingProxyAgentCertificate")
                    .setEncipheredPrivateKey("invertingProxyAgentEncipheredKey");

        }
        return mockInvertingProxyAgentBuilder.build();
    }

    @Test
    void testInvertingProxyReturnsOnlyEncipheredAccessKey() {
        String gatewayDomain = "test.gateway.domain";
        InvertingProxy mockInvertingProxy = InvertingProxy.newBuilder()
                .setHostname("invertingProxyHost")
                .setCertificate("invertingProxyCertificate")
                .build();
        InvertingProxyAgent mockInvertingProxyAgent = InvertingProxyAgent.newBuilder()
                .setAgentCrn("invertingProxyAgentCrn")
                .setEnvironmentCrn(TEST_ENVIRONMENT_CRN)
                .setCertificate("invertingProxyAgentCertificate")
                .setEncipheredPrivateKey("invertingProxyAgentEncipheredKey")
                .setEncipheredAccessKey("invertingProxyAgentMachineUserEncipheredAccessKey")
                .build();

        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString(), anyString())).thenReturn(mockInvertingProxy);
        when(ccmV2Client.registerInvertingProxyAgent(anyString(), anyString(), any(Optional.class), anyString(), anyString()))
                .thenReturn(mockInvertingProxyAgent);

        assertThatThrownBy(() -> underTest.getCcmV2Parameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), gatewayDomain,
                Crn.fromString(TEST_CLUSTER_CRN).getResource()))
                .hasMessage("InvertingProxyAgent Access Key ID is not present but Enciphered Access Key is initialized. Error in inverting proxy logic.")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
