package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.METERING_AZURE_METADATA;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.MeteringAzureMetadataPatchConfig;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class MeteringAzureMetadataPatchService extends AbstractTelemetryPatchService  {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringAzureMetadataPatchService.class);

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
    private MeteringAzureMetadataPatchConfig meteringAzureMetadataPatchConfig;

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            if (StackType.WORKLOAD.equals(stack.getType())
                    && CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform())) {
                try {
                    final Long dateBeforeTimestamp = dateStringToTimestampForImage(meteringAzureMetadataPatchConfig.getDateBefore());
                    Optional<StatedImage> statedImageOpt = stackImageService.getStatedImageInternal(stack);
                    if (statedImageOpt.isEmpty() || statedImageOpt.get().getImage() == null
                            || statedImageOpt.get().getImage().getCreated() < dateBeforeTimestamp) {
                        affected = true;
                    }
                } catch (Exception e) {
                    String errorMessage = String.format("Cannot determine stack with crn %s is affected by azure metadata issue", stack.getResourceCrn());
                    LOGGER.debug(errorMessage, e);
                    throw new ExistingStackPatchApplyException(errorMessage, e);
                }
            }
            return affected;
        } catch (Exception e) {
            LOGGER.warn("Error during obtaining image / catalog for stack " + stack.getResourceCrn(), e);
            throw new CloudbreakRuntimeException("Error during obtaining image / catalog for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    void doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (isPrimaryGatewayReachable(stack)) {
            try {
                upgradeMeteringOnNodes(stack);
            } catch (ExistingStackPatchApplyException e) {
                throw e;
            } catch (Exception e) {
                throw new ExistingStackPatchApplyException(e.getMessage(), e);
            }
        } else {
            String message = "Salt partial update and metering upgrade cannot run, because primary gateway is unreachable of stack: " + stack.getResourceCrn();
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
    }

    private void upgradeMeteringOnNodes(Stack stack) throws ExistingStackPatchApplyException, IOException, CloudbreakOrchestratorFailedException {
        byte[] currentSaltState = getCurrentSaltStateStack(stack);
        List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
        List<String> meteringSaltStateDef = List.of("/salt/metering");
        byte[] meteringSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, meteringSaltStateDef);
        boolean meteringContentMatches = compressUtil.compareCompressedContent(currentSaltState, meteringSaltStateConfig, meteringSaltStateDef);
        if (!meteringContentMatches) {
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
            getTelemetryOrchestrator().updateMeteringSaltDefinition(meteringSaltStateConfig, gatewayConfigs, exitModel);
            Set<Node> availableNodes = getAvailableNodes(stack.getName(), instanceMetaDataSet, gatewayConfigs, exitModel);
            getTelemetryOrchestrator().upgradeMetering(gatewayConfigs, availableNodes, exitModel,
                    meteringAzureMetadataPatchConfig.getDateBefore(), meteringAzureMetadataPatchConfig.getCustomRpmUrl());
            byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, meteringSaltStateDef, currentSaltState);
            clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
            LOGGER.debug("Metering partial salt refresh successfully finished for stack {}", stack.getName());
        } else {
            LOGGER.debug("Metering partial salt refresh is not required for stack {}", stack.getName());
        }
    }

    @Override
    public StackPatchType getStackFixType() {
        return METERING_AZURE_METADATA;
    }
}
