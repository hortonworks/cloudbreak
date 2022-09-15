package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_FOR_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_KEY;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterConstants.CCMV2_AGENT_IV;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_BACKEND_ID_PREFIX;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CERTIFICATE;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_ENCIPHERED_KEY;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_KEY_ID;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_CERTIFICATE;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_HOST;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DefaultCcmV2JumpgateParametersTest {

    private DefaultCcmV2JumpgateParameters underTest;

    @Test
    void testAddToTemplateModelFull() {
        underTest = new DefaultCcmV2JumpgateParameters("invertingProxyHost", "invertingProxyCertificate", "agentCrn",
                "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate", "environmentCrn",
                "agentMachineUserAccessKey", "agentMachineUserEncipheredAccessKey", "hmacKey", "initialisationVector", "hmacForPrivateKey");

        Map<String, Object> expectedParams = new HashMap<>();
        underTest.addToTemplateModel(expectedParams);

        assertEquals(12, expectedParams.size(), "CcmV2Jumpgate Paramters size should match");
        assertEquals("invertingProxyHost", expectedParams.get(CCMV2_INVERTING_PROXY_HOST), "CcmV2 InvertingProxy Host should match");
        assertEquals("agentCrn", expectedParams.get(CCMV2_AGENT_CRN), "CcmV2 AgentCrn should match");
        assertEquals("agentCrn-", expectedParams.get(CCMV2_AGENT_BACKEND_ID_PREFIX), "CcmV2 Agent BackendId Prefix should match");
        assertEquals("agentKeyId", expectedParams.get(CCMV2_AGENT_KEY_ID), "CcmV2 AgentKeyId should match");

        assertEquals("invertingProxyCertificate",
                expectedParams.get(CCMV2_INVERTING_PROXY_CERTIFICATE), "CcmV2 InvertingProxy Cert should match");

        assertEquals("agentEncipheredPrivateKey",
                expectedParams.get(CCMV2_AGENT_ENCIPHERED_KEY), "CcmV2 Agent Enciphered Private Key should match");

        assertEquals("agentCertificate", expectedParams.get(CCMV2_AGENT_CERTIFICATE), "CcmV2 Agent Certificate should match");
        assertEquals("agentMachineUserAccessKey",
                expectedParams.get(CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID), "CcmV2 Agent Machine User Access Key ID should match");
        assertEquals("agentMachineUserEncipheredAccessKey",
                expectedParams.get(CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY), "CcmV2 Agent Machine User Enciphered Access Key should match");
        assertEquals("hmacKey", expectedParams.get(CCMV2_AGENT_HMAC_KEY), "CcmV2 Agent HMAC Key should match");
        assertEquals("initialisationVector", expectedParams.get(CCMV2_AGENT_IV), "CcmV2 Agent Initialisation Vector should match");
        assertEquals("hmacForPrivateKey", expectedParams.get(CCMV2_AGENT_HMAC_FOR_PRIVATE_KEY), "CcmV2 Agent HMAC for Private Key should match");
    }

    @Test
    void testAddToTemplateModelDefault() {
        underTest = new DefaultCcmV2JumpgateParameters();

        Map<String, Object> expectedParams = new HashMap<>();
        underTest.addToTemplateModel(expectedParams);

        assertEquals(12, expectedParams.size(), "CcmV2Jumpgate Paramters size should match");
        assertEquals(EMPTY, expectedParams.get(CCMV2_INVERTING_PROXY_HOST), "CcmV2 InvertingProxy Host should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_CRN), "CcmV2 AgentCrn should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_BACKEND_ID_PREFIX), "CcmV2 Agent BackendId Prefix should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_KEY_ID), "CcmV2 AgentKeyId should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_INVERTING_PROXY_CERTIFICATE), "CcmV2 InvertingProxy Cert should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_ENCIPHERED_KEY), "CcmV2 Agent Enciphered Private Key should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_CERTIFICATE), "CcmV2 Agent Certificate should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID), "CcmV2 Agent Machine User Access Key ID should be empty");
        assertEquals(EMPTY,
                expectedParams.get(CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY), "CcmV2 Agent Machine User Enciphered Access Key should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_HMAC_KEY), "CcmV2 Agent HMAC Key should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_IV), "CcmV2 Agent Initialisation Vector should be empty");
        assertEquals(EMPTY, expectedParams.get(CCMV2_AGENT_HMAC_FOR_PRIVATE_KEY), "CcmV2 Agent HMAC for Private Key should be empty");
    }

}
