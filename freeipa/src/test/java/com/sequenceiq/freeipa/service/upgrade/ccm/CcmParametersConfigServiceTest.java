package com.sequenceiq.freeipa.service.upgrade.ccm;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.userdata.CcmV2TlsTypeDecider;

@ExtendWith(MockitoExtension.class)
class CcmParametersConfigServiceTest {

    private static final String INVERTING_PROXY_HOST = "hostname";

    private static final String INVERTING_PROXY_CERTIFICATE = "proxycert";

    private static final String AGENT_CRN = "agent1";

    private static final String AGENT_KEY_ID = "key1";

    private static final String PRIVATE_KEY = "privatekey1";

    private static final String AGENT_CERT = "agent certificate";

    private static final String ENV_CRN = "environment1";

    private static final String MACHINE_USER_ACCESS_KEY = "accessKey1";

    private static final String MACHINE_USER_PRIVATE_KEY = "privateKey1";

    private static final String HMAC_KEY = "hmacKey";

    private static final String IV = "iv";

    private static final String HMAC_PRIVATE_KEY = "hmacForPrivateKey";

    @Mock
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Mock
    private CachedEnvironmentClientService environmentService;

    @InjectMocks
    private CcmParametersConfigService underTest;

    @Test
    void testEmptyParams() {
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ENV_CRN, null);
        verifyNoInteractions(environmentService, ccmV2TlsTypeDecider);
        assertThat(result).isEmpty();
    }

    @Test
    void testParamsWithOneWayTls() {
        when(ccmV2TlsTypeDecider.decide(any())).thenReturn(CcmV2TlsType.ONE_WAY_TLS);
        CcmV2JumpgateParameters params = createParams();
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ENV_CRN, params);
        verify(environmentService).getByCrn(ENV_CRN);
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
                Map.entry("agent_enciphered_access_key", MACHINE_USER_PRIVATE_KEY),
                Map.entry("agent_hmac_key", HMAC_KEY),
                Map.entry("initialisation_vector", IV),
                Map.entry("agent_hmac_for_private_key", HMAC_PRIVATE_KEY),
                Map.entry("activation_in_minutes", 0L)
        );
    }

    @Test
    void testParamsWithTwoWayTls() {
        when(ccmV2TlsTypeDecider.decide(any())).thenReturn(CcmV2TlsType.TWO_WAY_TLS);
        CcmV2JumpgateParameters params = createParams();
        Map<String, SaltPillarProperties> result = underTest.createCcmParametersPillarConfig(ENV_CRN, params);
        verify(environmentService).getByCrn(ENV_CRN);
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
                Map.entry("agent_enciphered_access_key", EMPTY),
                Map.entry("agent_hmac_key", EMPTY),
                Map.entry("initialisation_vector", EMPTY),
                Map.entry("agent_hmac_for_private_key", EMPTY),
                Map.entry("activation_in_minutes", 0L)
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
                MACHINE_USER_PRIVATE_KEY,
                HMAC_KEY,
                IV,
                HMAC_PRIVATE_KEY
        );
        return params;
    }
}
