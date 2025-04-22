package com.sequenceiq.cloudbreak.telemetry.upgrade;

import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView.DESIRED_CDP_LOGGING_AGENT_VERSION;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView.DESIRED_CDP_TELEMETRY_VERSION;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.DATABUS;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.FILECOLLECTOR;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.FLUENT;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.MONITORING;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.TELEMETRY;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class TelemetryUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryUpgradeService.class);

    private static final String EMPTY_VERSION = "";

    private static final Set<TelemetryOrchestratorModule> TELEMETRY_MODULES = Set.of(TELEMETRY, DATABUS, FILECOLLECTOR, MONITORING);

    private static final Set<TelemetryOrchestratorModule> LOGGING_AGENT_MODULES = Set.of(TELEMETRY, DATABUS, FLUENT);

    @Inject
    private TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    @Inject
    private TelemetryConfigProvider telemetryConfigProvider;

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private CompressUtil compressUtil;

    public void upgradeTelemetryComponent(Long stackId, TelemetryComponentType componentType, OrchestratorMetadataFilter filter)
            throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        Set<Node> nodes = filter != null ? filter.apply(metadata).getNodes() : metadata.getNodes();
        if (TelemetryComponentType.CDP_LOGGING_AGENT.equals(componentType)) {
            LOGGER.debug("Starting cdp-logging-agent upgrade");
            telemetryOrchestrator.updateTelemetryComponent(metadata.getGatewayConfigs(), nodes, metadata.getExitCriteriaModel(),
                    Map.of("telemetry", Map.of(DESIRED_CDP_LOGGING_AGENT_VERSION, telemetryUpgradeConfiguration.getCdpLoggingAgent().getDesiredVersion(),
                            DESIRED_CDP_TELEMETRY_VERSION, EMPTY_VERSION)));
        } else {
            LOGGER.debug("Starting cdp-telemetry upgrade");
            telemetryOrchestrator.updateTelemetryComponent(metadata.getGatewayConfigs(), nodes, metadata.getExitCriteriaModel(),
                    Map.of("telemetry", Map.of(DESIRED_CDP_LOGGING_AGENT_VERSION, EMPTY_VERSION,
                            DESIRED_CDP_TELEMETRY_VERSION, telemetryUpgradeConfiguration.getCdpTelemetry().getDesiredVersion())));
        }
    }

    public void upgradeTelemetrySaltPillars(Long stackId, Set<TelemetryComponentType> components) throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        Set<Node> availableNodes = collectAvailableNodes(metadata);
        if (CollectionUtils.isEmpty(availableNodes)) {
            String message = "Not found any available nodes for stack: " + stackId;
            LOGGER.info(message);
            throw new CloudbreakOrchestratorFailedException(message);
        } else {
            Map<String, SaltPillarProperties> pillarPropMap = telemetryConfigProvider.createTelemetryConfigs(stackId, components);
            SaltConfig saltConfig = new SaltConfig(pillarPropMap);
            hostOrchestrator.initSaltConfig(metadata.getStack(), metadata.getGatewayConfigs(), availableNodes, saltConfig, metadata.getExitCriteriaModel());
        }
    }

    private Set<Node> collectAvailableNodes(OrchestratorMetadata metadata) throws CloudbreakOrchestratorFailedException {
        Set<Node> allNodes = metadata.getNodes();
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(
                metadata.getGatewayConfigs(), allNodes, metadata.getExitCriteriaModel());
        Set<String> unresponsiveHostnames = unresponsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        return allNodes.stream()
                .filter(n -> !unresponsiveHostnames.contains(n.getHostname()))
                .collect(Collectors.toSet());
    }

    public void upgradeTelemetrySaltStates(Long stackId, Set<TelemetryComponentType> components) throws IOException, CloudbreakOrchestratorFailedException {
        List<String> saltStateDefinitions = orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders();
        Set<TelemetryOrchestratorModule> saltStateComponents = getSaltStateComponents(components);
        List<String> filteredSaltComponents = getSaltStateComponentPaths(saltStateComponents);
        LOGGER.debug("Start upgrading salt components: {}", filteredSaltComponents);
        byte[] currentSaltState = orchestratorMetadataProvider.getStoredStates(stackId);
        byte[] telemetrySaltStateConfigs = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, filteredSaltComponents);
        if (currentSaltState == null) {
            LOGGER.debug("Salt states are not stored, upgrading salt state components: {}", saltStateComponents);
            updateSaltStateForComponents(stackId, saltStateComponents, telemetrySaltStateConfigs);
        } else {
            boolean saltStateContentMatches = compressUtil.compareCompressedContent(currentSaltState, telemetrySaltStateConfigs, filteredSaltComponents);
            if (!saltStateContentMatches) {
                LOGGER.debug("Content is not matching for salt states, upgrading salt state components: {}", saltStateComponents);
                updateSaltStateForComponents(stackId, saltStateComponents, telemetrySaltStateConfigs);
                byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, filteredSaltComponents, currentSaltState);
                LOGGER.debug("Storing new salt states for stack with id {}.", stackId);
                orchestratorMetadataProvider.storeNewState(stackId, newFullSaltState);
            } else {
                LOGGER.debug("Skipping salt state upgrade as current content matches with the persisted one.");
            }
        }
    }

    private void updateSaltStateForComponents(Long stackId, Set<TelemetryOrchestratorModule> components, byte[] telemetrySaltStateConfigs)
            throws CloudbreakOrchestratorFailedException {
        List<String> componentNames = components.stream().map(TelemetryOrchestratorModule::getValue).collect(Collectors.toList());
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        telemetryOrchestrator.updatePartialSaltDefinition(telemetrySaltStateConfigs, componentNames, metadata.getGatewayConfigs(),
                metadata.getExitCriteriaModel());
    }

    private Set<TelemetryOrchestratorModule> getSaltStateComponents(Set<TelemetryComponentType> components) {
        return components.stream().map(this::mapTelemetryComponentType).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private Set<TelemetryOrchestratorModule> mapTelemetryComponentType(TelemetryComponentType component) {
        switch (component) {
            case CDP_TELEMETRY:
                return TELEMETRY_MODULES;
            case CDP_LOGGING_AGENT:
                return LOGGING_AGENT_MODULES;
            default:
                return Set.of();
        }
    }

    private List<String> getSaltStateComponentPaths(Set<TelemetryOrchestratorModule> modules) {
        return modules.stream().map(module -> String.format("/salt/%s", module.getValue())).collect(Collectors.toList());
    }
}
