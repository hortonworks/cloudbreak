package com.sequenceiq.freeipa.service.freeipa.flow;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.freeipa.config.LdapAgentConfigProvider;
import com.sequenceiq.freeipa.service.paywall.PaywallConfigService;
import com.sequenceiq.freeipa.service.proxy.ProxyConfigService;
import com.sequenceiq.freeipa.service.tag.TagConfigService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;
import com.sequenceiq.freeipa.service.upgrade.ccm.CcmParametersConfigService;

@Service
public class SaltConfigProvider {

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private TagConfigService tagConfigService;

    @Inject
    private TelemetryConfigService telemetryConfigService;

    @Inject
    private CcmParametersConfigService ccmParametersConfigService;

    @Inject
    private LdapAgentConfigProvider ldapAgentConfigProvider;

    @Inject
    private PaywallConfigService paywallConfigService;

    @Inject
    private CachedEnvironmentClientService environmentClientService;

    public SaltConfig getSaltConfig(Stack stack, Set<Node> hosts) {
        SaltConfig saltConfig = new SaltConfig();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, hosts);
        servicePillarConfig.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", singletonMap("freeipa", freeIpaConfigView.toMap())));
        servicePillarConfig.put("discovery", new SaltPillarProperties("/discovery/init.sls", getPlatformMetadata(stack)));
        servicePillarConfig.putAll(telemetryConfigService.createTelemetryPillarConfig(stack));
        servicePillarConfig.putAll(proxyConfigService.createProxyPillarConfig(stack));
        servicePillarConfig.putAll(tagConfigService.createTagsPillarConfig(stack));
        servicePillarConfig.putAll(getCcmPillarProperties(stack));
        servicePillarConfig.putAll(ldapAgentConfigProvider.generateConfig(stack, freeIpaConfigView.getDomain()));
        servicePillarConfig.putAll(paywallConfigService.createPaywallPillarConfig(stack));
        if (freeIpaConfigView.isSecretEncryptionEnabled()) {
            servicePillarConfig.put("cdpluksvolumebackup", new SaltPillarProperties("/cdpluksvolumebackup/init.sls",
                    singletonMap("cdpluksvolumebackup", getCdpLuksVolumeBackUpProperties(stack))));
        }
        return saltConfig;
    }

    private Map<String, Object> getPlatformMetadata(Stack stack) {
        boolean govCloud = Boolean.FALSE;
        if (AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value().equals(stack.getPlatformvariant())) {
            govCloud = Boolean.TRUE;
        }
        String environmentType = environmentClientService.getByCrn(stack.getEnvironmentCrn()).getEnvironmentType();
        return Map.of(
                "platform", stack.getCloudPlatform(),
                "gov_cloud", govCloud,
                "environmentType", environmentType);
    }

    private static Map<String, Object> getCdpLuksVolumeBackUpProperties(Stack stack) {
        return Map.of(
                "backup_location", stack.getBackup() != null ? stack.getBackup().getStorageLocation() : "",
                "aws_region", stack.getRegion());
    }

    public Map<String, SaltPillarProperties> getCcmPillarProperties(Stack stack) {
        CcmConnectivityParameters ccmParameters = stack.getCcmParameters();
        if (ccmParameters != null && ccmParameters.getCcmV2JumpgateParameters() != null) {
            return ccmParametersConfigService.createCcmParametersPillarConfig(stack.getEnvironmentCrn(), ccmParameters.getCcmV2JumpgateParameters());
        }
        return Map.of();
    }
}
