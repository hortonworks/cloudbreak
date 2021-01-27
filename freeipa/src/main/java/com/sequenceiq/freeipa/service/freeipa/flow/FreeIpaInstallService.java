package com.sequenceiq.freeipa.service.freeipa.flow;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstallService.class);

    private static final String PROXY_KEY = "proxy";

    private static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

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
    private TelemetryCommonConfigService telemetryCommonConfigService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

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
        ProxyConfig proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn()).orElse(null);

        SaltConfig saltConfig = new SaltConfig();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, allNodes, proxyConfig);
        servicePillarConfig.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", Collections.singletonMap("freeipa", freeIpaConfigView.toMap())));
        servicePillarConfig.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.getCloudPlatform())));
        servicePillarConfig.putAll(createTelemetryPillarConfig(stack));
        servicePillarConfig.putAll(createProxyPillarConfig(proxyConfig));
        servicePillarConfig.putAll(createTagsPillarConfig(stack));
        hostOrchestrator.initSaltConfig(gatewayConfigs, allNodes, saltConfig, new StackBasedExitCriteriaModel(stackId));
        hostOrchestrator.installFreeIpa(primaryGatewayConfig, gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
    }

    private Map<String, SaltPillarProperties> createProxyPillarConfig(ProxyConfig proxyConfig) {
        if (proxyConfig != null) {
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", proxyConfig.getServerHost());
            proxy.put("port", proxyConfig.getServerPort());
            proxy.put("protocol", proxyConfig.getProtocol());
            proxyConfig.getProxyAuthentication().ifPresent(auth -> {
                proxy.put("user", auth.getUserName());
                proxy.put("password", auth.getPassword());
            });
            return Map.of(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        } else {
            return Map.of();
        }
    }

    private Map<String, SaltPillarProperties> createTelemetryPillarConfig(Stack stack) throws CloudbreakOrchestratorFailedException {
        Map<String, SaltPillarProperties> servicePillarConfig = new HashMap<>();
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


            final TelemetryCommonConfigView telemetryCommonConfigs = telemetryCommonConfigService.createTelemetryCommonConfigs(
                    telemetry, vmLogsService.getVmLogs(), FluentClusterType.FREEIPA.value(), stack.getResourceCrn(),
                    stack.getName(), stack.getOwner(), stack.getCloudPlatform());
            servicePillarConfig.put("telemetry",
                    new SaltPillarProperties("/telemetry/init.sls", Collections.singletonMap("telemetry", telemetryCommonConfigs.toMap())));

            FluentConfigView fluentConfigView = fluentConfigService.createFluentConfigs(clusterDetails,
                    databusEnabled, false, telemetry);
            servicePillarConfig.put("fluent", new SaltPillarProperties("/fluent/init.sls", Collections.singletonMap("fluent", fluentConfigView.toMap())));
            if (databusEnabled) {
                LOGGER.debug("Apply DataBus related pillars.");
                try {
                    DataBusCredential dbusCredential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(stack);
                    String accessKey = dbusCredential.getAccessKey();
                    char[] privateKey = dbusCredential.getPrivateKey().toCharArray();
                    boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(stack.getAccountId());
                    String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
                    DatabusConfigView databusConfigView = databusConfigService.createDatabusConfigs(accessKey, privateKey,
                            null, databusEndpoint);
                    servicePillarConfig.put("databus", new SaltPillarProperties("/databus/init.sls",
                            Collections.singletonMap("databus", databusConfigView.toMap())));
                } catch (IOException e) {
                    throw new CloudbreakOrchestratorFailedException(e);
                }
            }
        }
        return servicePillarConfig;
    }

    private Map<String, SaltPillarProperties> createTagsPillarConfig(Stack stack) {
        if (stack.getTags() != null && isNotBlank(stack.getTags().getValue())) {
            try {
                StackTags stackTags = stack.getTags().get(StackTags.class);
                Map<String, Object> tags = new HashMap<>(stackTags.getDefaultTags());
                Map<String, Object> applicationTags = new HashMap<>(stackTags.getApplicationTags());
                tags.putAll(applicationTags);
                return Map.of("tags", new SaltPillarProperties("/tags/init.sls",
                        Collections.singletonMap("tags", tags)));
            } catch (Exception e) {
                LOGGER.debug("Exception during reading default tags.", e);
                return Map.of();
            }
        } else {
            return Map.of();
        }
    }
}
