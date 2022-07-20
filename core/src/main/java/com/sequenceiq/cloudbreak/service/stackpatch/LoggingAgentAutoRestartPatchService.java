package com.sequenceiq.cloudbreak.service.stackpatch;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.LoggingAgentAutoRestartPatchConfig;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class LoggingAgentAutoRestartPatchService extends AbstractTelemetryPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAgentAutoRestartPatchService.class);

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private LoggingAgentAutoRestartPatchConfig loggingAgentAutoRestartPatchConfig;

    private Version affectedVersion;

    private Long dateAfterTimestamp;

    private Long dateBeforeTimestamp;

    @PostConstruct
    public void init() {
        affectedVersion = Version.parse(loggingAgentAutoRestartPatchConfig.getAffectedVersionFrom());
        dateAfterTimestamp = dateStringToTimestampForImage(loggingAgentAutoRestartPatchConfig.getDateAfter());
        dateBeforeTimestamp = dateStringToTimestampForImage(loggingAgentAutoRestartPatchConfig.getDateBefore());
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            if (StackType.WORKLOAD.equals(stack.getType())) {
                Image image = stackImageService.getCurrentImage(stack.getId());
                Map<String, String> packageVersions = image.getPackageVersions();
                boolean hasCdpLoggingAgentPackageVersion = packageVersions.containsKey(ImagePackageVersion.CDP_LOGGING_AGENT.getKey());
                if (hasCdpLoggingAgentPackageVersion
                        && Version.parse(packageVersions.get(ImagePackageVersion.CDP_LOGGING_AGENT.getKey())).compareTo(affectedVersion) <= 0) {
                    affected = true;
                } else if (!hasCdpLoggingAgentPackageVersion) {
                    affected = isAffectedByImageTimestamp(stack, image, dateAfterTimestamp, dateBeforeTimestamp);
                }
            }
            return affected;
        } catch (Exception e) {
            LOGGER.warn("Image not found for stack " + stack.getResourceCrn(), e);
            throw new CloudbreakRuntimeException("Image not found for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (isPrimaryGatewayReachable(stack)) {
            try {
                byte[] currentSaltState = getCurrentSaltStateStack(stack);
                List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
                List<String> loggingAgentSaltStateDef = List.of("/salt/fluent");
                byte[] fluentSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, loggingAgentSaltStateDef);
                boolean loggingAgentContentMatches = compressUtil.compareCompressedContent(currentSaltState, fluentSaltStateConfig, loggingAgentSaltStateDef);
                if (!loggingAgentContentMatches) {
                    Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stack.getId());
                    List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                    ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
                    Set<Node> availableNodes = getAvailableNodes(instanceMetaDataSet, gatewayConfigs, exitModel);
                    if (CollectionUtils.isEmpty(availableNodes)) {
                        LOGGER.info("Not found any available nodes for patch, stack: " + stack.getName());
                        return false;
                    } else {
                        getTelemetryOrchestrator().executeLoggingAgentDiagnostics(fluentSaltStateConfig, gatewayConfigs, availableNodes, exitModel);
                        byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, loggingAgentSaltStateDef, currentSaltState);
                        clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
                        LOGGER.debug("Logging agent partial salt refresh and diagnostics successfully finished for stack {}", stack.getName());
                        return true;
                    }
                } else {
                    LOGGER.debug("Logging agent partial salt refresh and diagnostics is not required for stack {}", stack.getName());
                    return true;
                }
            } catch (ExistingStackPatchApplyException e) {
                throw e;
            } catch (Exception e) {
                throw new ExistingStackPatchApplyException(e.getMessage(), e);
            }
        } else {
            LOGGER.info("Salt partial update cannot run, because primary gateway is unreachable of stack: " + stack.getResourceCrn());
            return false;
        }
    }

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.LOGGING_AGENT_AUTO_RESTART_V2;
    }

    private boolean isAffectedByImageTimestamp(Stack stack, Image image, long dateAfterTimestamp, long dateBeforeTimestamp) {
        boolean affected = false;
        ImageCatalog imageCatalog = stackImageService.getImageCatalogFromStackAndImage(stack, image);
        if (!imageCatalogService.isCustomImageCatalog(imageCatalog)) {
            Optional<StatedImage> statedImageOpt = stackImageService.getStatedImageInternal(stack, image, imageCatalog);
            if (statedImageOpt.isEmpty() || statedImageOpt.get().getImage() == null || (dateAfterTimestamp <= statedImageOpt.get().getImage().getCreated()
                    && statedImageOpt.get().getImage().getCreated() < dateBeforeTimestamp)) {
                affected = true;
            }
        }
        return affected;
    }
}
