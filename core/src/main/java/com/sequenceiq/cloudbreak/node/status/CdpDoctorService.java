package com.sequenceiq.cloudbreak.node.status;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorMeteringStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.RetryType;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupName;

@Service
public class CdpDoctorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdpDoctorService.class);

    private static final String DOCTOR_NETWORK_COMMAND = "cdp-doctor network status --format json";

    private static final String DOCTOR_METERING_COMMAND = "cdp-doctor metering status --format json";

    private static final String DOCTOR_SERVICES_COMMAND = "cdp-doctor service status --format json";

    private static final String TELEMETRY_VERSION_COMMAND = "rpm -q --queryformat '%-{VERSION}' \"cdp-telemetry\"";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    public Map<String, CdpDoctorMeteringStatusResponse> getMeteringStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return saltOrchestrator.runCommandOnAllHostsWithFewRetry(primaryGatewayConfig, DOCTOR_METERING_COMMAND).entrySet().stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorMeteringStatusResponse.class)));
    }

    public Map<String, CdpDoctorNetworkStatusResponse> getNetworkStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Set<String> targetInstances = collectNewInstancesIfAny(stack);
        return saltOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfig), targetInstances, DOCTOR_NETWORK_COMMAND, RetryType.NO_RETRY)
                .entrySet()
                .stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorNetworkStatusResponse.class)));
    }

    private Set<String> collectNewInstancesIfAny(StackDtoDelegate stack) {
        Long creationFinished = Optional.ofNullable(stack.getCluster()).map(ClusterView::getCreationFinished).orElse(null);
        Predicate<InstanceMetadataView> newInstanceFilter = instanceMetaData -> {
            Long instanceStartDate = instanceMetaData.getStartDate();
            return Comparator.nullsFirst(Comparator.<Long>naturalOrder()).compare(instanceStartDate, creationFinished) > 0;
        };
        return stack.getAllAvailableInstances()
                .stream()
                .collect(Collectors.teeing(
                        Collectors.filtering(newInstanceFilter, Collectors.toSet()),
                        Collectors.toSet(),
                        (newInstances, allInstances) -> {
                            // Returns new instances only if there are any new instances.
                            LOGGER.debug("New instances: {}, all items: {}", newInstances, allInstances);
                            return newInstances.isEmpty() ? allInstances : newInstances;
                        }
                ))
                .stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());
    }

    public String getTelemetryVersion(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        OrchestratorRunParams runParams = getOrchestratorRunParams(primaryGatewayConfig);
        Map<String, String> result = saltOrchestrator.runShellCommandOnNodes(runParams);
        LOGGER.debug("Telemetry version: {}", result);
        String gatewayHostname = primaryGatewayConfig.getHostname();
        if (result.containsKey(gatewayHostname)) {
            return result.get(gatewayHostname);
        } else {
            throw new CloudbreakOrchestratorFailedException("Telemetry version is not available on host: " + gatewayHostname);
        }
    }

    private OrchestratorRunParams getOrchestratorRunParams(GatewayConfig primaryGatewayConfig) {
        Set<Node> nodes = Set.of(new Node(
                primaryGatewayConfig.getPrivateAddress(),
                primaryGatewayConfig.getPublicAddress(),
                primaryGatewayConfig.getInstanceId(),
                null,
                primaryGatewayConfig.getHostname(),
                InstanceGroupName.GATEWAY.name()));
        return new OrchestratorRunParams(nodes, List.of(primaryGatewayConfig),
                TELEMETRY_VERSION_COMMAND, "Failed to get telemetry version");
    }

    public Map<String, CdpDoctorServicesStatusResponse> getServicesStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return saltOrchestrator.runCommandOnAllHostsWithFewRetry(primaryGatewayConfig, DOCTOR_SERVICES_COMMAND).entrySet().stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorServicesStatusResponse.class)));
    }
}
