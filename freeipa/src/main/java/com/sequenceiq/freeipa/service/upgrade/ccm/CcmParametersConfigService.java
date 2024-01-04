package com.sequenceiq.freeipa.service.upgrade.ccm;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.userdata.CcmV2TlsTypeDecider;

@Service
public class CcmParametersConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmParametersConfigService.class);

    private static final String UPGRADE_CCM_KEY = "ccm_jumpgate";

    private static final String UPGRADE_CCM_SLS_PATH = "/upgradeccm/init.sls";

    @Value("${ccmRevertJob.activationInMinutes}")
    private long activationInMinutes;

    @Inject
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Inject
    private CachedEnvironmentClientService environmentService;

    public Map<String, SaltPillarProperties> createCcmParametersPillarConfig(String environmentCrn, CcmV2JumpgateParameters ccmParameters) {
        if (ccmParameters != null) {
            LOGGER.debug("Filling CCMv2 properties to Salt Pillar");
            DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
            boolean useOneWayTls = CcmV2TlsType.ONE_WAY_TLS == ccmV2TlsTypeDecider.decide(environment);
            LOGGER.debug("Using one-way TLS for CCMv2: {}", useOneWayTls);
            Map<String, Object> ccmConfig = new HashMap<>();
            ccmConfig.put("inverting_proxy_host", ccmParameters.getInvertingProxyHost());
            ccmConfig.put("inverting_proxy_certificate", ccmParameters.getInvertingProxyCertificate());
            ccmConfig.put("agent_certificate", ccmParameters.getAgentCertificate());
            ccmConfig.put("agent_enciphered_key", ccmParameters.getAgentEncipheredPrivateKey());
            ccmConfig.put("agent_key_id", ccmParameters.getAgentKeyId());
            ccmConfig.put("agent_backend_id_prefix", ccmParameters.getAgentBackendIdPrefix());
            ccmConfig.put("environment_crn", ccmParameters.getEnvironmentCrn());
            ccmConfig.put("agent_access_key_id", useOneWayTls ? ccmParameters.getAgentMachineUserAccessKey() : EMPTY);
            ccmConfig.put("agent_enciphered_access_key", useOneWayTls ? ccmParameters.getAgentMachineUserEncipheredAccessKey() : EMPTY);
            ccmConfig.put("agent_hmac_key", useOneWayTls ? ccmParameters.getHmacKey() : EMPTY);
            ccmConfig.put("initialisation_vector", useOneWayTls ? ccmParameters.getInitialisationVector() : EMPTY);
            ccmConfig.put("agent_hmac_for_private_key", useOneWayTls ? ccmParameters.getHmacForPrivateKey() : EMPTY);
            ccmConfig.put("activation_in_minutes", getActivationInMinutes());
            return Map.of(UPGRADE_CCM_KEY, new SaltPillarProperties(UPGRADE_CCM_SLS_PATH, singletonMap(UPGRADE_CCM_KEY, ccmConfig)));
        } else {
            LOGGER.debug("CCM properties are empty in the Salt Pillar");
            return Map.of();
        }
    }

    public long getActivationInMinutes() {
        return activationInMinutes;
    }

}

