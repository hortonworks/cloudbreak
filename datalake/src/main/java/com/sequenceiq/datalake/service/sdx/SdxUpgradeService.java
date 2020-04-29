package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;

import java.util.Collections;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Service
public class SdxUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public UpgradeOptionV4Response checkForUpgradeByName(String userCrn, String clusterName) {
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        return stackV4Endpoint.checkForOsUpgrade(0L, cluster.getClusterName());
    }

    public UpgradeOptionV4Response checkForUpgradeByCrn(String userCrn, String clusterCrn) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return stackV4Endpoint.checkForOsUpgrade(0L, cluster.getClusterName());
    }

    public SdxUpgradeResponse triggerUpgradeByName(String userCrn, String clusterName) {
        UpgradeOptionV4Response upgradeOption = checkForUpgradeByName(userCrn, clusterName);
        validateOsUpgradeOption(upgradeOption);
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        return triggerUpgrade(upgradeOption, cluster);
    }

    public SdxUpgradeResponse triggerUpgradeByCrn(String userCrn, String clusterCrn) {
        UpgradeOptionV4Response upgradeOption = checkForUpgradeByCrn(userCrn, clusterCrn);
        validateOsUpgradeOption(upgradeOption);
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return triggerUpgrade(upgradeOption, cluster);
    }

    private SdxUpgradeResponse triggerUpgrade(UpgradeOptionV4Response upgradeOption, SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeOsUpgradeFlow(cluster.getId(), upgradeOption);
        String imageId = upgradeOption.getUpgrade().getImageId();
        String message = getMessage(imageId);
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    public void changeImage(Long id, UpgradeOptionV4Response upgradeOption) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            String targetImageId = upgradeOption.getUpgrade().getImageId();
            StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
            stackImageChangeRequest.setImageId(targetImageId);
            stackImageChangeRequest.setImageCatalogName(upgradeOption.getUpgrade().getImageCatalogName());
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS,
                    "Changing image",
                    cluster.get());
            FlowIdentifier flowIdentifier = stackV4Endpoint.changeImage(0L, cluster.get().getClusterName(), stackImageChangeRequest);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster.get(), flowIdentifier);
        } else {
            throw new NotFoundException("Not found cluster with id" + id);
        }
    }

    public void upgradeCluster(Long id, String imageId) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(() -> new NotFoundException("Not found the cluster with id: " + id));
        sdxStatusService.setStatusForDatalakeAndNotify(
                DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS,
                "Upgrading datalake stack",
                sdxCluster);
        try {
            FlowIdentifier flowIdentifier = stackV4Endpoint.upgradeClusterByName(0L, sdxCluster.getClusterName(), imageId);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Stack upgrade failed on cluster: [%d]. Message: [%s]", id, e.getMessage());
            throw new CloudbreakApiException(message);
        }
    }

    public String getImageId(Long id) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            StackV4Response stackV4Response = stackV4Endpoint.get(0L, cluster.get().getClusterName(), Set.of());
            return stackV4Response.getImage().getId();
        } else {
            throw new NotFoundException("Cluster not found with id" + id);
        }
    }

    public String getCurrentImageCatalogName(Long id) {
        SdxCluster cluster = sdxClusterRepository.findById(id).orElseThrow(notFound("Cluster", id));
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, cluster.getClusterName(), Set.of());
        return stackV4Response.getImage().getCatalogName();
    }

    public void upgradeOs(Long id) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DatalakeStatusEnum.UPGRADE_IN_PROGRESS,
                    "OS upgrade started",
                    cluster.get());
            FlowIdentifier flowIdentifier = stackV4Endpoint.upgradeOs(0L, cluster.get().getClusterName());
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster.get(), flowIdentifier);
        } else {
            throw new NotFoundException("Cluster not found with id" + id);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkClusterStatusDuringUpgrade(sdxCluster, pollingMessage));
    }

    private void validateOsUpgradeOption(UpgradeOptionV4Response upgradeOptionV4Response) {
        if (upgradeOptionV4Response.getUpgrade() == null) {
            throw new BadRequestException("There is no image containing the same Runtime eligible to upgrade the stack");
        } else if (upgradeOptionV4Response.getReason() != null) {
            throw new BadRequestException(String.format("There is an error preventing the upgrade of the stack: %s.",
                    upgradeOptionV4Response.getReason()));
        }
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
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (stackAndClusterAvailable(stackV4Response, cluster)) {
            return AttemptResults.finishWith(stackV4Response);
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
                    LOGGER.info("Flow finished, but stack hasn't been upgraded: {}", sdxCluster.getClusterName());
                    return sdxUpgradeFailed(sdxCluster, "stack is in improper state", pollingMessage);
                } else {
                    return AttemptResults.justContinue();
                }
            }
        }
    }

    private AttemptResult<StackV4Response> sdxUpgradeFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor("SDX " + pollingMessage + " failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }

    private String getMessage(String imageId) {
        return messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), Collections.singletonList(imageId));
    }
}
