package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.AccountTelemetryService;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstallService.class);

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FluentConfigService fluentConfigService;

    @Inject
    private DatabusConfigService databusConfigService;

    @Inject
    private AccountTelemetryService accountTelemetryService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    public void installFreeIpa(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);
        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), "testGroup"))
                .collect(Collectors.toSet());

        if (allNodes.isEmpty()) {
            String errorMessage = "There are no nodes to install with FreeIPA.";
            LOGGER.error(errorMessage);
            throw new CloudbreakOrchestratorFailedException(errorMessage);
        }

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);

        SaltConfig saltConfig = new SaltConfig();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, allNodes);
        servicePillarConfig.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", Collections.singletonMap("freeipa", freeIpaConfigView.toMap())));
        decoratePillarsWithTelemetryConfigs(stack, servicePillarConfig);
        hostOrchestrator.initSaltConfig(gatewayConfigs, allNodes, saltConfig, new StackBasedExitCriteriaModel(stackId));
        hostOrchestrator.installFreeIPA(primaryGatewayConfig, gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
    }

    private void decoratePillarsWithTelemetryConfigs(Stack stack, Map<String, SaltPillarProperties> servicePillarConfig) {
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            boolean databusEnabled = telemetry.isClusterLogsCollectionEnabled();
            final TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                    .withOwner(stack.getOwner())
                    .withName(stack.getName())
                    .withType(FluentClusterType.FREEIPA.value())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVersion(version)
                    .build();
            List<AnonymizationRule> rules = accountTelemetryService.getAnonymizationRules();
            FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(clusterDetails,
                    databusEnabled, false, telemetry, rules);
            servicePillarConfig.put("fluent", new SaltPillarProperties("/fluent/init.sls", Collections.singletonMap("fluent", fluentConfigView.toMap())));
            if (databusEnabled) {
                Optional<AltusCredential> credential = altusMachineUserService.createMachineUserWithAccessKeys(stack, telemetry);
                String accessKey = credential.map(AltusCredential::getAccessKey).orElse(null);
                char[] privateKey = credential.map(AltusCredential::getPrivateKey).orElse(null);
                DatabusConfigView databusConfigView = databusConfigService.createDatabusConfigs(accessKey, privateKey,
                        null, telemetry.getDatabusEndpoint());
                servicePillarConfig.put("databus", new SaltPillarProperties("/databus/init.sls",
                        Collections.singletonMap("databus", databusConfigView.toMap())));
            }
        }
    }
}
