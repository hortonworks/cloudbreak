package com.sequenceiq.freeipa.service.upgrade.ccm;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class CcmParametersConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmParametersConfigService.class);

    private static final String UPGRADE_CCM_KEY = "ccm_jumpgate";

    private static final String UPGRADE_CCM_SLS_PATH = "/upgradeccm/init.sls";

    @Inject
    private EntitlementService entitlementService;

    public Map<String, SaltPillarProperties> createCcmParametersPillarConfig(String accountId, CcmV2JumpgateParameters ccmParameters) {
        if (ccmParameters != null) {
            LOGGER.debug("Filling CCM properties to Salt Pillar");
            Map<String, Object> ccmConfig = new HashMap<>();
            ccmConfig.put("inverting_proxy_host", ccmParameters.getInvertingProxyHost());
            ccmConfig.put("inverting_proxy_certificate", ccmParameters.getInvertingProxyCertificate());
            ccmConfig.put("agent_certificate", ccmParameters.getAgentCertificate());
            ccmConfig.put("agent_enciphered_key", ccmParameters.getAgentEncipheredPrivateKey());
            ccmConfig.put("agent_key_id", ccmParameters.getAgentKeyId());
            ccmConfig.put("agent_backend_id_prefix", ccmParameters.getAgentBackendIdPrefix());
            ccmConfig.put("environment_crn", ccmParameters.getEnvironmentCrn());
            boolean useOneWayTls = entitlementService.ccmV2UseOneWayTls(accountId);
            ccmConfig.put("agent_access_key_id", useOneWayTls ? ccmParameters.getAgentMachineUserAccessKey() : EMPTY);
            ccmConfig.put("agent_enciphered_access_key", useOneWayTls ? ccmParameters.getAgentMachineUserEncipheredAccessKey() : EMPTY);
            return Map.of(UPGRADE_CCM_KEY, new SaltPillarProperties(UPGRADE_CCM_SLS_PATH, singletonMap(UPGRADE_CCM_KEY, ccmConfig)));
        } else {
            LOGGER.debug("CCM properties are empty in the Salt Pillar");
            return Map.of();
        }
    }

}

