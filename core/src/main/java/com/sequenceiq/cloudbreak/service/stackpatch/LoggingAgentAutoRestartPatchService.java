package com.sequenceiq.cloudbreak.service.stackpatch;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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

    private final String affectedVersionFrom;

    private final String dateAfter;

    private final String dateBefore;

    private Version affectedVersion;

    private Long dateAfterTimestamp;

    private Long dateBeforeTimestamp;

    public LoggingAgentAutoRestartPatchService(
            @Value("${existingstackpatcher.activePatches.loggingAgentAutoRestart.affectedVersionFrom}") String affectedVersionFrom,
            @Value("${existingstackpatcher.activePatches.loggingAgentAutoRestart.dateAfter}") String dateAfter,
            @Value("${existingstackpatcher.activePatches.loggingAgentAutoRestart.dateBefore}") String dateBefore) {
        this.affectedVersionFrom = affectedVersionFrom;
        this.dateAfter = dateAfter;
        this.dateBefore = dateBefore;
    }

    @PostConstruct
    public void init() {
        affectedVersion = Version.parse(affectedVersionFrom);
        dateAfterTimestamp = dateStringToTimestampForImage(dateAfter);
        dateBeforeTimestamp = dateStringToTimestampForImage(dateBefore);
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            if (StackType.WORKLOAD.equals(stack.getType())) {
                Image image = getImageByStack(stack);
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
    void doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (isPrimaryGatewayReachable(stack)) {
            try {
                byte[] currentSaltState = getCurrentSaltStateStack(stack);
                List<String> saltStateDefinitions = Arrays.asList("salt-common", "salt");
                List<String> loggingAgentSaltStateDef = List.of("/salt/fluent");
                byte[] fluentSaltStateConfig = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, loggingAgentSaltStateDef);
                boolean loggingAgentContentMatches = compressUtil.compareCompressedContent(currentSaltState, fluentSaltStateConfig, loggingAgentSaltStateDef);
                if (!loggingAgentContentMatches) {
                    Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
                    List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                    ClusterDeletionBasedExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.nonCancellableModel();
                    Set<Node> availableNodes = getAvailableNodes(stack.getName(), instanceMetaDataSet, gatewayConfigs, exitModel);
                    getTelemetryOrchestrator().executeLoggingAgentDiagnostics(fluentSaltStateConfig, gatewayConfigs, availableNodes, exitModel);
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
            String message = "Salt partial update cannot run, because primary gateway is unreachable of stack: " + stack.getResourceCrn();
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
    }

    @Override
    public StackPatchType getStackFixType() {
        return StackPatchType.LOGGING_AGENT_AUTO_RESTART;
    }

    private boolean isAffectedByImageTimestamp(Stack stack, Image image, long dateAfterTimestamp, long dateBeforeTimestamp) {
        boolean affected = false;
        ImageCatalog imageCatalog = getImageCatalogFromStackAndImage(stack, image);
        if (!imageCatalogService.isCustomImageCatalog(imageCatalog)) {
            StatedImage statedImage = getStatedImage(stack, image, imageCatalog);
            if (statedImage == null || statedImage.getImage() == null || (dateAfterTimestamp <= statedImage.getImage().getCreated()
                    && statedImage.getImage().getCreated() < dateBeforeTimestamp)) {
                affected = true;
            }
        }
        return affected;
    }
}
