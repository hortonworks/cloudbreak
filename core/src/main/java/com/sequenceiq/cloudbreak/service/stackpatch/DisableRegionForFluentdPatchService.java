package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.auth.crn.Crn.Region.AP_1;
import static com.sequenceiq.cloudbreak.auth.crn.Crn.Region.EU_1;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.DISABLE_REGION_FOR_FLUENTD;

import java.lang.module.ModuleDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.service.stackpatch.config.DisableRegionForFluentdPatchConfig;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class DisableRegionForFluentdPatchService extends AbstractTelemetryPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableRegionForFluentdPatchService.class);

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
    private DisableRegionForFluentdPatchConfig disableRegionForFluentdPatchConfig;

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            byte[] currentSaltState = getCurrentSaltStateStack(stack);
            List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
            List<String> loggingAgentSaltStateDef = List.of("/salt/fluent");
            byte[] fluentSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, loggingAgentSaltStateDef);
            boolean loggingAgentContentMatches = compressUtil.compareCompressedContent(currentSaltState, fluentSaltStateConfig, loggingAgentSaltStateDef);
            if (!loggingAgentContentMatches) {
                Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stack.getId());
                List<GatewayConfig> gatewayConfigs = List.of(gatewayConfigService.getPrimaryGatewayConfig(stack));
                ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
                Set<Node> availableNodes = getAvailableNodes(instanceMetaDataSet, gatewayConfigs, exitModel);
                if (CollectionUtils.isEmpty(availableNodes)) {
                    LOGGER.info("Not found any available nodes for patch, stack: " + stack.getName());
                    return false;
                } else {
                    getTelemetryOrchestrator().updateAndRestartTelemetryService(fluentSaltStateConfig, "fluent", "fluent.init",
                            gatewayConfigs, availableNodes, exitModel);
                    byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, loggingAgentSaltStateDef, currentSaltState);
                    clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
                    LOGGER.debug("Disable region for fluentd has been successfully finished for stack {}", stack.getName());
                    return true;
                }
            } else {
                LOGGER.debug("Disable region for fluentd is not required for stack {}", stack.getName());
                return true;
            }
        } catch (ExistingStackPatchApplyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExistingStackPatchApplyException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            if (isDatahubAndEUorAPRegion(stack)) {
                Image image = stackImageService.getCurrentImage(stack.getId());
                Map<String, String> packageVersions = image.getPackageVersions();
                boolean hasCdpLoggingAgentPackageVersion = packageVersions.containsKey(ImagePackageVersion.CDP_LOGGING_AGENT.getKey());
                if (hasCdpLoggingAgentPackageVersion
                        && ModuleDescriptor.Version.parse(packageVersions.get(ImagePackageVersion.CDP_LOGGING_AGENT.getKey()))
                        .compareTo(disableRegionForFluentdPatchConfig.getVersionModelFromAffectedVersion()) <= 0) {
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
        return DISABLE_REGION_FOR_FLUENTD;
    }

    private boolean isDatahubAndEUorAPRegion(Stack stack) {
        if (StringUtils.isBlank(stack.getResourceCrn())) {
            return false;
        }
        Crn crn = Crn.safeFromString(stack.getResourceCrn());
        Crn.Region region = crn.getRegion();
        return Crn.Service.DATAHUB.equals(crn.getService()) && (EU_1.equals(region) || AP_1.equals(region));
    }
}
