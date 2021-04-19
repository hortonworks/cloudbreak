package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import com.sequenceiq.datalake.service.sdx.status.AvailabilityChecker;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
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
    private AvailabilityChecker availabilityChecker;

    @Inject
    private SdxUpgradeValidationResultProvider sdxUpgradeValidationResultProvider;

    public void changeImage(Long id, UpgradeOptionV4Response upgradeOption) {
        SdxCluster cluster = sdxService.getById(id);
        String targetImageId = upgradeOption.getUpgrade().getImageId();
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(targetImageId);
        stackImageChangeRequest.setImageCatalogName(upgradeOption.getUpgrade().getImageCatalogName());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS, "Changing image", cluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.changeImageInternal(0L, cluster.getClusterName(), stackImageChangeRequest, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack change image failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void upgradeRuntime(Long id, String imageId) {
        SdxCluster sdxCluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS, "Upgrading datalake stack", sdxCluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.upgradeClusterByNameInternal(0L, sdxCluster.getClusterName(), imageId, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack upgrade failed on cluster: [%s]. Message: [%s]", sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public String getImageId(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.get(0L, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
            return stackV4Response.getImage().getId();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't get image id for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void updateRuntimeVersionFromCloudbreak(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        String clusterName = sdxCluster.getClusterName();
        LOGGER.info("Trying to update the runtime version from Cloudbreak for cluster: {}", clusterName);
        StackV4Response stack = ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.get(0L, clusterName, Set.of(), sdxCluster.getAccountId()));
        sdxService.updateRuntimeVersionFromStackResponse(sdxCluster, stack);
    }

    public String getCurrentImageCatalogName(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.get(0L, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
            return stackV4Response.getImage().getCatalogName();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't fetch image catalog for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void upgradeOs(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS, "OS upgrade started", cluster);

        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.upgradeOsInternal(0L, cluster.getClusterName(), initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack upgrade failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxService.getById(id);
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkClusterStatusDuringUpgrade(sdxCluster, pollingMessage));
    }

    private AttemptResult<StackV4Response> checkClusterStatusDuringUpgrade(SdxCluster sdxCluster, String pollingMessage) throws JsonProcessingException {
        LOGGER.info("{} polling cloudbreak for stack status: '{}' in '{}' env", pollingMessage, sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in inmemory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(sdxCluster);
                if (RUNNING.equals(flowState)) {
                    LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", pollingMessage, sdxCluster.getId());
                    return AttemptResults.justContinue();
                } else {
                    return getStackResponseAttemptResult(sdxCluster, pollingMessage, flowState);
                }
            }
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, String pollingMessage, FlowState flowState)
            throws JsonProcessingException {
        StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (availabilityChecker.stackAndClusterAvailable(stackV4Response, cluster)) {
            if (sdxUpgradeValidationResultProvider.isValidationFailed(sdxCluster)) {
                return AttemptResults.breakFor(new UpgradeValidationFailedException("Upgrade validation failed."));
            } else {
                return AttemptResults.finishWith(stackV4Response);
            }
        } else {
            if (Status.UPDATE_FAILED.equals(stackV4Response.getStatus())) {
                LOGGER.info("{} failed for Stack {} with status {}", pollingMessage, stackV4Response.getName(), stackV4Response.getStatus());
                return sdxUpgradeFailed(sdxCluster, stackV4Response.getStatusReason(), pollingMessage);
            } else if (Status.UPDATE_FAILED.equals(stackV4Response.getCluster().getStatus())) {
                LOGGER.info("{} failed for Cluster {} status {}", pollingMessage, stackV4Response.getCluster().getName(),
                        stackV4Response.getCluster().getStatus());
                return sdxUpgradeFailed(sdxCluster, stackV4Response.getCluster().getStatusReason(), pollingMessage);
            } else {
                if (FINISHED.equals(flowState)) {
                    String message = sdxStatusService.getShortStatusMessage(stackV4Response);
                    LOGGER.info("Flow finished, but stack or cluster is  not available: {}", message);
                    return sdxUpgradeFailed(sdxCluster, message, pollingMessage);
                } else {
                    return AttemptResults.justContinue();
                }
            }
        }
    }

    private AttemptResult<StackV4Response> sdxUpgradeFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor("SDX " + pollingMessage + " failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }
}
