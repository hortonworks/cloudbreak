package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCcmV2ParameterSupplierTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    @InjectMocks
    private DefaultCcmV2ParameterSupplier underTest;

    @Mock
    private CcmV2ManagementClient ccmV2Client;

    @Test
    public void testGetCcmV2Parameters() throws CcmException {
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
                .build();

        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString(), anyString())).thenReturn(mockInvertingProxy);
        when(ccmV2Client.registerInvertingProxyAgent(anyString(), anyString(), any(Optional.class), anyString(), anyString()))
                .thenReturn(mockInvertingProxyAgent);

        CcmV2Parameters resultParameters = underTest.getCcmV2Parameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), gatewayDomain,
                Crn.fromString(TEST_CLUSTER_CRN).getResource());
        assertNotNull(resultParameters, "CCMV2 Parameters should not be null");

        assertEquals("invertingProxyAgentCrn", resultParameters.getAgentCrn(), "AgentCRN should match");
        assertEquals("invertingProxyAgentCertificate", resultParameters.getAgentCertificate(), "AgentCertificate should match");
        assertEquals("invertingProxyAgentEncipheredKey", resultParameters.getAgentEncipheredPrivateKey(), "AgentEncipheredPrivateKey should match");
        assertEquals("invertingProxyHost", resultParameters.getInvertingProxyHost(), "InvertingProxyHost should match");
        assertEquals("invertingProxyCertificate", resultParameters.getInvertingProxyCertificate(), "InvertingProxyCertificate should match");
        assertEquals(TEST_RESOURCE_ID, resultParameters.getAgentKeyId(), "AgentKeyId should match");
    }
}
