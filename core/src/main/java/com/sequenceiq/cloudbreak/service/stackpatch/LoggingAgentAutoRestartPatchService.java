package com.sequenceiq.cloudbreak.service.stackpatch;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class LoggingAgentAutoRestartPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAgentAutoRestartPatchService.class);

    private static final String AFFECTED_CDP_LOGGING_AGENT_VERSION = "0.2.13";

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Override
    public boolean isAffected(Stack stack) {
        Version affectedVersion = Version.parse(AFFECTED_CDP_LOGGING_AGENT_VERSION);
        try {
            boolean affected = false;
            if (StackType.WORKLOAD.equals(stack.getType())) {
                Image image = stackImageService.getCurrentImage(stack);
                Map<String, String> packageVersions = image.getPackageVersions();
                if (packageVersions.containsKey(ImagePackageVersion.CDP_LOGGING_AGENT.getKey())
                        && Version.parse(packageVersions.get(ImagePackageVersion.CDP_LOGGING_AGENT.getKey())).compareTo(affectedVersion) <= 0) {
                    affected = true;
                }
            }
            return affected;
        } catch (Exception e) {
            LOGGER.warn("Image not found for stack " + stack.getResourceCrn(), e);
            throw new CloudbreakRuntimeException("Image not found for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    void doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (isCmServerReachable(stack)) {
            try {
                byte[] currentSaltState = clusterComponentConfigProvider.getSaltStateComponent(stack.getCluster().getId());
                if (currentSaltState == null) {
                    String message = "Salt state is empty for stack " + stack.getResourceCrn();
                    LOGGER.info(message);
                    throw new ExistingStackPatchApplyException(message);
                }
                List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
                List<String> loggingAgentSaltStateDef = List.of("/salt/fluent");
                byte[] fluentSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, loggingAgentSaltStateDef);
                boolean loggingAgentContentMatches = compressUtil.compareCompressedContent(currentSaltState, fluentSaltStateConfig, loggingAgentSaltStateDef);
                if (!loggingAgentContentMatches) {
                    Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
                    List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                    ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
                    Set<Node> availableNodes = getAvailableNodes(stack.getName(), instanceMetaDataSet, gatewayConfigs, exitModel);
                    telemetryOrchestrator.executeLoggingAgentDiagnostics(fluentSaltStateConfig, gatewayConfigs, availableNodes, exitModel);
                    byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, loggingAgentSaltStateDef, currentSaltState);
                    clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
                    LOGGER.debug("Logging agent partial salt refresh and diagnostics successfully finished for stack {}", stack.getName());
                } else {
                    LOGGER.debug("Logging agent partial salt refresh and diagnostics is not required for stack {}", stack.getName());
                }
            } catch (ExistingStackPatchApplyException e) {
                throw e;
            } catch (Exception e) {
                throw new ExistingStackPatchApplyException(e.getMessage(), e);
            }
        } else {
            String message = "Salt partial update cannot run, because CM server is unreachable of stack: " + stack.getResourceCrn();
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
    }

    private Set<Node> getAvailableNodes(String stackName, Set<InstanceMetaData> instanceMetaDataSet, List<GatewayConfig> gatewayConfigs,
            ClusterDeletionBasedExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException, ExistingStackPatchApplyException {
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(gatewayConfigs, allNodes, exitModel);
        Set<String> unresponsiveHostnames = unresponsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        Set<Node> availableNodes = allNodes.stream()
                .filter(n -> !unresponsiveHostnames.contains(n.getHostname()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(availableNodes)) {
            String message = "Not found any available nodes for logging-agent auto restart patch, stack: " + stackName;
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
        return availableNodes;
    }

    @Override
    public StackPatchType getStackFixType() {
        return StackPatchType.LOGGING_AGENT_AUTO_RESTART;
    }

    private Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .collect(Collectors.toSet());
    }
}
