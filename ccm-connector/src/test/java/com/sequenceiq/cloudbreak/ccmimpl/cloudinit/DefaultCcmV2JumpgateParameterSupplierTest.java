package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2RetryingClient;

@ExtendWith(MockitoExtension.class)
class DefaultCcmV2JumpgateParameterSupplierTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    private static final String TEST_HMAC = "testHmac";

    @InjectMocks
    private DefaultCcmV2JumpgateParameterSupplier underTest;

    @Mock
    private CcmV2RetryingClient ccmV2Client;

    @ParameterizedTest(name = "singleWayTls = {0}, useHmac = {1}")
    @MethodSource("parameterScenarios")
    void testGetCcmV2JumpgateParameters(boolean singleWayTls, boolean useHmac) {
        String gatewayDomain = "test.gateway.domain";
        InvertingProxy mockInvertingProxy = InvertingProxy.newBuilder()
                .setHostname("invertingProxyHost")
                .setCertificate("invertingProxyCertificate")
                .build();
        InvertingProxyAgent mockInvertingProxyAgent = getInvertingProxyAgent(singleWayTls, useHmac);

        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString())).thenReturn(mockInvertingProxy);
        when(ccmV2Client.registerInvertingProxyAgent(anyString(), any(Optional.class), anyString(), anyString(), any(Optional.class)))
                .thenReturn(mockInvertingProxyAgent);

        CcmV2JumpgateParameters resultParameters = underTest.getCcmV2JumpgateParameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), gatewayDomain,
                Crn.fromString(TEST_CLUSTER_CRN).getResource(), useHmac ? Optional.of(TEST_HMAC) : Optional.empty());
        assertNotNull(resultParameters, "CCMV2 Jumpgate Parameters should not be null");

        assertEquals(TEST_ENVIRONMENT_CRN, resultParameters.getEnvironmentCrn(), "EnvironmentCRN should match");
        assertEquals("invertingProxyAgentCrn", resultParameters.getAgentCrn(), "AgentCRN should match");
        assertEquals("invertingProxyHost", resultParameters.getInvertingProxyHost(), "InvertingProxyHost should match");
        assertEquals("invertingProxyCertificate", resultParameters.getInvertingProxyCertificate(), "InvertingProxyCertificate should match");
        assertEquals(TEST_RESOURCE_ID, resultParameters.getAgentKeyId(), "AgentKeyId should match");
        assertAgentCertOrMachineUser(resultParameters, singleWayTls, useHmac);
    }

    public static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true)
        );
    }

    private void assertAgentCertOrMachineUser(CcmV2JumpgateParameters resultParameters, boolean singleWayTls, boolean useHmac) {
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
            if (useHmac) {
                assertEquals("iv",
                        resultParameters.getInitialisationVector(),
                        "AgentInitialisationVector should match");
                assertEquals("hmacForPrivateKey",
                        resultParameters.getHmacForPrivateKey(),
                        "AgentMachineUserHmacForPrivateKey should match");
            } else {
                assertThat(resultParameters.getInitialisationVector())
                        .withFailMessage("AgentInitialisationVector should be null when HMAC Key is not provided")
                        .isNullOrEmpty();
                assertThat(resultParameters.getHmacForPrivateKey())
                        .withFailMessage("AgentMachineUserHmacForPrivateKey should be null when HMAC Key is not provided")
                        .isNullOrEmpty();
            }
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
            assertThat(resultParameters.getInitialisationVector())
                    .withFailMessage("AgentInitialisationVector should be null for two-way TLS")
                    .isNullOrEmpty();
            assertThat(resultParameters.getHmacForPrivateKey())
                    .withFailMessage("AgentMachineUserHmacForPrivateKey should be null for two-way TLS")
                    .isNullOrEmpty();
        }
    }

    private InvertingProxyAgent getInvertingProxyAgent(boolean singleWayTls, boolean useHmac) {
        InvertingProxyAgent.Builder mockInvertingProxyAgentBuilder = InvertingProxyAgent.newBuilder()
                .setAgentCrn("invertingProxyAgentCrn")
                .setEnvironmentCrn(TEST_ENVIRONMENT_CRN);
        if (singleWayTls) {
            mockInvertingProxyAgentBuilder
                    .setAccessKeyId("invertingProxyAgentMachineUserAccessKey")
                    .setEncipheredAccessKey("invertingProxyAgentMachineUserEncipheredAccessKey");
            if (useHmac) {
                mockInvertingProxyAgentBuilder
                        .setHmacForPrivateKey("hmacForPrivateKey")
                        .setInitialisationVector("iv");
            }
        } else {
            mockInvertingProxyAgentBuilder
                    .setCertificate("invertingProxyAgentCertificate")
                    .setEncipheredPrivateKey("invertingProxyAgentEncipheredKey");

        }
        return mockInvertingProxyAgentBuilder.build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validationScenarios")
    void testInvertingProxyResponseValidation(String testName, String invertingProxyHost, String invertingProxyCertificate, String agentCrn,
            String agentCert, String privateKey, String accessKey, String secretKey, String iv, Optional<String> hmacKeyOpt, String hmac, String message) {
        String gatewayDomain = "test.gateway.domain";
        InvertingProxy mockInvertingProxy = InvertingProxy.newBuilder()
                .setHostname(invertingProxyHost)
                .setCertificate(invertingProxyCertificate)
                .build();
        InvertingProxyAgent mockInvertingProxyAgent = InvertingProxyAgent.newBuilder()
                .setAgentCrn(agentCrn)
                .setEnvironmentCrn(TEST_ENVIRONMENT_CRN)
                .setCertificate(agentCert)
                .setEncipheredPrivateKey(privateKey)
                .setAccessKeyId(accessKey)
                .setEncipheredAccessKey(secretKey)
                .setInitialisationVector(iv)
                .setHmacForPrivateKey(hmac)
                .build();

        when(ccmV2Client.awaitReadyInvertingProxyForAccount(anyString())).thenReturn(mockInvertingProxy);
        when(ccmV2Client.registerInvertingProxyAgent(anyString(), any(Optional.class), eq(gatewayDomain), anyString(), any(Optional.class)))
                .thenReturn(mockInvertingProxyAgent);

        assertThatThrownBy(() -> underTest.getCcmV2JumpgateParameters(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN), gatewayDomain,
                Crn.fromString(TEST_CLUSTER_CRN).getResource(), hmacKeyOpt))
                .hasMessage(message)
                .isInstanceOf(IllegalArgumentException.class);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] validationScenarios() {
        return new Object[][] {
            // testName                              host    hostcert     agentCrn    agentCert    agentPrivKey    accessKey    secretKey    iv     hmacKey                 hmac              errorMessage
            { "Empty InvertingProxy hostname",       EMPTY,  "proxyCert", "agentCrn", "agentCert", "agentPrivKey", "accessKey", "secretKey", "iv",  Optional.empty(),       EMPTY,            "InvertingProxy Hostname is not initialized." },
            { "Empty InvertingProxy cert",           "host", EMPTY,       "agentCrn", "agentCert", "agentPrivKey", "accessKey", "secretKey", "iv",  Optional.empty(),       EMPTY,            "InvertingProxy Certificate is not initialized." },
            { "Empty InvertingProxyAgent CRN",       "host", "proxyCert", EMPTY,      "agentCert", "agentPrivKey", "accessKey", "secretKey", "iv",  Optional.empty(),       EMPTY,            "InvertingProxyAgent CRN is not initialized." },
            { "Empty Agent Machine User Secret Key", "host", "proxyCert", "agentCrn", "agentCert", "agentPrivKey", "accessKey", EMPTY,       "iv",  Optional.empty(),       EMPTY,            "InvertingProxyAgent Access Key ID is present but Enciphered Access Key is not initialized." },
            { "Empty Secret Key Digest",             "host", "proxyCert", "agentCrn", "agentCert", "agentPrivKey", "accessKey", "secretKey", "iv",  Optional.of("hmackey"), EMPTY,            "InvertingProxyAgent Enciphered Access Key digest is not present but HMAC key was passed. Error in inverting proxy logic." },
            { "Empty IV",                            "host", "proxyCert", "agentCrn", "agentCert", "agentPrivKey", "accessKey", "secretKey", EMPTY, Optional.of("hmackey"), "hmac",           "InvertingProxyAgent IV is not present but HMAC key was passed. Error in inverting proxy logic." },
            { "Empty Agent Cert",                    "host", "proxyCert", "agentCrn", EMPTY,       "agentPrivKey", EMPTY,       EMPTY,       EMPTY, Optional.empty(),       EMPTY,            "InvertingProxyAgent Certificate is not initialized." },
            { "Empty Agent Cert Private Key",        "host", "proxyCert", "agentCrn", "agentCert", EMPTY,          EMPTY,       EMPTY,       EMPTY, Optional.empty(),       EMPTY,            "InvertingProxyAgent Enciphered Private Key is not initialized." },
            { "Agent Access Key is not excepted",    "host", "proxyCert", "agentCrn", "agentCert", "agentPrivKey", EMPTY,       "secretKey", EMPTY, Optional.empty(),       EMPTY,            "InvertingProxyAgent Access Key ID is not present but Enciphered Access Key is initialized. Error in inverting proxy logic." }
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
