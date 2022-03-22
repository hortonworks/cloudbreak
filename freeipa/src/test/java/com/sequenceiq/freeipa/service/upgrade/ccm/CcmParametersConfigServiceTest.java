package com.sequenceiq.freeipa.service.upgrade.ccm;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@ExtendWith(MockitoExtension.class)
class CcmParametersConfigServiceTest {

    private static final String ACCOUNT = "account";

    private static final String INVERTING_PROXY_HOST = "hostname";

    private static final String INVERTING_PROXY_CERTIFICATE = "proxycert";

    private static final String AGENT_CRN = "agent1";

    private static final String AGENT_KEY_ID = "key1";

    private static final String PRIVATE_KEY = "privatekey1";

    private static final String AGENT_CERT = "agent certificate";

    private static final String ENV_CRN = "environment1";

    private static final String MACHINE_USER_ACCESS_KEY = "accessKey1";

    private static final String MACHINE_USER_PRIVATE_KEY = "privateKey1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CcmParametersConfigService underTest;

    @Test
    void testEmptyParams() {
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ACCOUNT, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testParamsWithOneWayTls() {
        when(entitlementService.ccmV2UseOneWayTls(ACCOUNT)).thenReturn(true);
        CcmV2JumpgateParameters params = createParams();
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ACCOUNT, params);
        assertThat(result).containsKey("ccm_jumpgate");
        Map<String, Object> properties = result.get("ccm_jumpgate").getProperties();
        assertThat(properties).containsKey("ccm_jumpgate");
        Map<String, Object> ccmParameterMap = (Map<String, Object>) properties.get("ccm_jumpgate");

        assertThat(ccmParameterMap).containsOnly(
                Map.entry("inverting_proxy_certificate", INVERTING_PROXY_CERTIFICATE),
                Map.entry("inverting_proxy_host", INVERTING_PROXY_HOST),
                Map.entry("agent_certificate", AGENT_CERT),
                Map.entry("agent_enciphered_key", PRIVATE_KEY),
                Map.entry("agent_key_id", AGENT_KEY_ID),
                Map.entry("agent_backend_id_prefix", params.getAgentBackendIdPrefix()),
                Map.entry("environment_crn", ENV_CRN),
                Map.entry("agent_access_key_id", MACHINE_USER_ACCESS_KEY),
                Map.entry("agent_enciphered_access_key", MACHINE_USER_PRIVATE_KEY)
        );
    }

    @Test
    void testParamsWithTwoWayTls() {
        when(entitlementService.ccmV2UseOneWayTls(ACCOUNT)).thenReturn(false);
        CcmV2JumpgateParameters params = createParams();
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ACCOUNT, params);
        assertThat(result).containsKey("ccm_jumpgate");
        Map<String, Object> properties = result.get("ccm_jumpgate").getProperties();
        assertThat(properties).containsKey("ccm_jumpgate");
        Map<String, Object> ccmParameterMap = (Map<String, Object>) properties.get("ccm_jumpgate");

        assertThat(ccmParameterMap).containsOnly(
                Map.entry("inverting_proxy_certificate", INVERTING_PROXY_CERTIFICATE),
                Map.entry("inverting_proxy_host", INVERTING_PROXY_HOST),
                Map.entry("agent_certificate", AGENT_CERT),
                Map.entry("agent_enciphered_key", PRIVATE_KEY),
                Map.entry("agent_key_id", AGENT_KEY_ID),
                Map.entry("agent_backend_id_prefix", params.getAgentBackendIdPrefix()),
                Map.entry("environment_crn", ENV_CRN),
                Map.entry("agent_access_key_id", EMPTY),
                Map.entry("agent_enciphered_access_key", EMPTY)
        );
    }

    private CcmV2JumpgateParameters createParams() {
        CcmV2JumpgateParameters params = new DefaultCcmV2JumpgateParameters(
                INVERTING_PROXY_HOST,
                INVERTING_PROXY_CERTIFICATE,
                AGENT_CRN,
                AGENT_KEY_ID,
                PRIVATE_KEY,
                AGENT_CERT,
                ENV_CRN,
                MACHINE_USER_ACCESS_KEY,
                MACHINE_USER_PRIVATE_KEY
        );
        return params;
    }
}
