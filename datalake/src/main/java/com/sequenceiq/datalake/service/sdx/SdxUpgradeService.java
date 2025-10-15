package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_OS_UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ROLLING_OS_UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ROLLING_UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE_STARTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ROLLING_UPGRADE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.upgrade.OrderedOSUpgradeRequestProvider;
import com.sequenceiq.datalake.service.upgrade.SdxUpgradeValidationResultProvider;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SdxUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private SdxUpgradeValidationResultProvider cloudbreakFlowResultProvider;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private OrderedOSUpgradeRequestProvider orderedOSUpgradeRequestProvider;

    public void changeImage(Long id, UpgradeOptionV4Response upgradeOption) {
        SdxCluster cluster = sdxService.getById(id);
        String targetImageId = upgradeOption.getUpgrade().getImageId();
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(targetImageId);
        stackImageChangeRequest.setImageCatalogName(upgradeOption.getUpgrade().getImageCatalogName());
        sdxStatusService.setStatusForDatalakeAndNotify(CHANGE_IMAGE_IN_PROGRESS, "Changing image", cluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.changeImageInternal(0L, cluster.getClusterName(), stackImageChangeRequest, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack change image failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void upgradeRuntime(Long id, String imageId, boolean rollingUpgradeEnabled) {
        SdxCluster sdxCluster = sdxService.getById(id);
        sendRuntimeUpgradeNotification(rollingUpgradeEnabled, sdxCluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.upgradeClusterByNameInternal(0L, sdxCluster.getClusterName(), imageId, initiatorUserCrn, rollingUpgradeEnabled));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Upgrade failed on cluster: [%s]. Message: [%s]", sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private void sendRuntimeUpgradeNotification(boolean rollingUpgradeEnabled, SdxCluster sdxCluster) {
        if (rollingUpgradeEnabled) {
            sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_ROLLING_UPGRADE_IN_PROGRESS, DATALAKE_ROLLING_UPGRADE_STARTED, "Upgrading datalake runtime",
                    sdxCluster);
        } else {
            sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_IN_PROGRESS, DATALAKE_UPGRADE_STARTED, "Upgrading datalake runtime", sdxCluster);
        }
    }

    public String getImageId(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = retrieveStack(cluster);
            return stackV4Response.getImage().getId();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't get image id for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public Optional<String> updateRuntimeVersionFromCloudbreak(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        String clusterName = sdxCluster.getClusterName();
        LOGGER.info("Trying to update the runtime version from Cloudbreak for cluster: {}", clusterName);
        StackV4Response stack = retrieveStack(sdxCluster);
        return sdxService.updateRuntimeVersionFromStackResponse(sdxCluster, stack);
    }

    public void updateDbEngineVersionIfEmbeddedDbUpgradeHappened(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        if (!sdxCluster.getSdxDatabase().hasExternalDatabase()) {
            StackV4Response stackV4Response = retrieveStack(sdxCluster);
            updateDatabaseEngineVersion(stackV4Response, sdxCluster);
        }
    }

    private void updateDatabaseEngineVersion(StackV4Response stackV4Response, SdxCluster sdxCluster) {
        Optional.ofNullable(stackV4Response)
                .map(StackV4Response::getExternalDatabase)
                .map(DatabaseResponse::getDatabaseEngineVersion)
                .ifPresentOrElse(engineVersion -> {
                    if (!engineVersion.equals(sdxCluster.getDatabaseEngineVersion())) {
                        sdxService.updateDatabaseEngineVersion(sdxCluster.getCrn(), stackV4Response.getExternalDatabase().getDatabaseEngineVersion());
                        LOGGER.info("The embedded db engine version has been updated from Cloudbreak for cluster: {}", sdxCluster.getClusterName());
                    }
                }, () -> LOGGER.info("The embedded db version is missing from the stack response, sdx db version update will be skipped!"));
    }

    private StackV4Response retrieveStack(SdxCluster sdxCluster) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Set.of(), sdxCluster.getAccountId()));
    }

    public String getCurrentImageCatalogName(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = retrieveStack(cluster);
            return stackV4Response.getImage().getCatalogName();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't fetch image catalog for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void upgradeOs(Long id, String targetImageId, boolean rollingUpgradeEnabled, boolean keepVariant) {
        SdxCluster cluster = sdxService.getById(id);
        sendOsUpgradeNotification(rollingUpgradeEnabled, cluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> callOsUpgrade(cluster, initiatorUserCrn, targetImageId, rollingUpgradeEnabled, keepVariant));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack upgrade failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private FlowIdentifier callOsUpgrade(SdxCluster cluster, String initiatorUserCrn, String targetImageId, boolean rollingUpgradeEnabled, boolean keepVariant) {
        if (rollingUpgradeEnabled && cluster.getClusterShape().isHA()) {
            OrderedOSUpgradeSetRequest request = createOrderedOSUpgradeSetRequest(cluster, targetImageId);
            return stackV4Endpoint.upgradeOsByUpgradeSetsInternal(0L, cluster.getCrn(), request);
        } else {
            return stackV4Endpoint.upgradeOsInternal(0L, cluster.getClusterName(), initiatorUserCrn, keepVariant);
        }
    }

    private OrderedOSUpgradeSetRequest createOrderedOSUpgradeSetRequest(SdxCluster cluster, String targetImageId) {
        StackV4Response stackV4Response = retrieveStack(cluster);
        return orderedOSUpgradeRequestProvider.createDatalakeOrderedOSUpgradeSetRequest(stackV4Response, targetImageId, cluster.getClusterShape());
    }

    private void sendOsUpgradeNotification(boolean rollingUpgradeEnabled, SdxCluster cluster) {
        if (rollingUpgradeEnabled) {
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DATALAKE_ROLLING_UPGRADE_IN_PROGRESS, DATALAKE_ROLLING_OS_UPGRADE_STARTED, "Rolling OS upgrade started", cluster);
        } else {
            sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_IN_PROGRESS, DATALAKE_OS_UPGRADE_STARTED, "OS upgrade started", cluster);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxService.getById(id);
        cloudbreakPoller.pollUpdateUntilAvailable(pollingMessage, sdxCluster, pollingConfig);
        if (cloudbreakFlowResultProvider.isValidationFailed(sdxCluster)) {
            throw new UserBreakException(new UpgradeValidationFailedException("Upgrade validation failed."));
        }
    }
}
