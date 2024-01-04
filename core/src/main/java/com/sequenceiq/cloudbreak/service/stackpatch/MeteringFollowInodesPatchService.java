package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.METERING_FOLLOW_INODES;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class MeteringFollowInodesPatchService extends AbstractTelemetryPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringFollowInodesPatchService.class);

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            if (stack.isStopped()) {
                LOGGER.warn("Stack {} is stopped, won't apply metering follow inodes fix yet. (will retry)", stack.getName());
                return false;
            }
            byte[] currentSaltState = getCurrentSaltStateStack(stack);
            List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
            List<String> loggingAgentSaltStateDef = List.of("/salt/fluent");
            byte[] fluentSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, loggingAgentSaltStateDef);
            boolean loggingAgentContentMatches = compressUtil.compareCompressedContent(currentSaltState, fluentSaltStateConfig, loggingAgentSaltStateDef);
            if (!loggingAgentContentMatches) {
                byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, loggingAgentSaltStateDef, currentSaltState);
                clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
                LOGGER.debug("Metering follow inodes config for logging agent updated for stack {}", stack.getName());
                uploadSaltConfigsAndRestartLoggingAgent(stack, fluentSaltStateConfig);
            } else {
                LOGGER.debug("No need for any logging agent config update (follow inodes) for stack {}", stack.getName());
            }
            return true;
        } catch (ExistingStackPatchApplyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExistingStackPatchApplyException(e.getMessage(), e);
        }
    }

    private void uploadSaltConfigsAndRestartLoggingAgent(Stack stack, byte[] fluentSaltStateConfig) {
        try {
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stack.getId());
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
            Set<Node> availableNodes = getAvailableNodes(instanceMetaDataSet, gatewayConfigs, exitModel);
            if (CollectionUtils.isEmpty(availableNodes)) {
                LOGGER.info("Not found any available nodes for patch, stack: {}. skipping salt state update and waiting for next start", stack.getName());
            } else {
                getTelemetryOrchestrator().updateAndRestartTelemetryService(fluentSaltStateConfig, "fluent", "fluent.init",
                        gatewayConfigs, availableNodes, exitModel);
            }
        } catch (Exception e) {
            LOGGER.warn("Error during logging agent config update and restart. (skipping salt state update and waiting for next start)", e);
        }
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            Crn stackCrn = Crn.fromString(stack.getResourceCrn());
            if (StackType.WORKLOAD.equals(stack.getType()) && stackCrn != null && entitlementService.isCdpSaasEnabled(stackCrn.getAccountId())) {
                Image image = stackImageService.getCurrentImage(stack.getId());
                Map<String, String> packageVersions = image.getPackageVersions();
                boolean hasCdpLoggingAgentPackage = packageVersions.containsKey(ImagePackageVersion.CDP_LOGGING_AGENT.getKey());
                if (hasCdpLoggingAgentPackage) {
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
    public StackPatchType getStackPatchType() {
        return METERING_FOLLOW_INODES;
    }
}
