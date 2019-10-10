package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOption;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;

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
    private FlowEndpoint flowEndpoint;

    public UpgradeOption checkForUpgradeByName(String userCrn, String clusterName) {
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        return stackV4Endpoint.checkForUpgrade(0L, cluster.getClusterName());
    }

    public UpgradeOption checkForUpgradeByCrn(String userCrn, String clusterCrn) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return stackV4Endpoint.checkForUpgrade(0L, cluster.getClusterName());
    }

    public void triggerUpgradeByName(String userCrn, String clusterName) {
        UpgradeOption upgradeOption = checkForUpgradeByName(userCrn, clusterName);
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        MDCBuilder.buildMdcContext(cluster);
        sdxReactorFlowManager.triggerDatalakeUpgradeFlow(cluster.getId(), upgradeOption);
    }

    public void triggerUpgradeByCrn(String userCrn, String clusterCrn) {
        UpgradeOption upgradeOption = checkForUpgradeByCrn(userCrn, clusterCrn);
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(cluster);
        sdxReactorFlowManager.triggerDatalakeUpgradeFlow(cluster.getId(), upgradeOption);
    }

    public void changeImage(Long id, UpgradeOption upgradeOption) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
            stackImageChangeRequest.setImageId(upgradeOption.getImageId());
            stackImageChangeRequest.setImageCatalogName(upgradeOption.getImageCatalogName());
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS,
                    ResourceEvent.SDX_CHANGE_IMAGE_STARTED,
                    "Changing image",
                    cluster.get());
            stackV4Endpoint.changeImage(0L, cluster.get().getClusterName(), stackImageChangeRequest);
        } else {
            throw new NotFoundException("Not found cluster with id" + id);
        }
    }

    public String getImageId(Long id) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            StackV4Response stackV4Response = stackV4Endpoint.get(0L, cluster.get().getClusterName(), Set.of());
            return stackV4Response.getImage().getId();
        } else {
            throw new NotFoundException("Not found cluster with id" + id);
        }
    }

    public void upgrade(Long id) {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(id);
        if (cluster.isPresent()) {
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DatalakeStatusEnum.UPGRADE_IN_PROGRESS,
                    ResourceEvent.SDX_UPGRADE_STARTED,
                    "Upgrade started",
                    cluster.get());
            stackV4Endpoint.upgradeCluster(0L, cluster.get().getClusterName());
        } else {
            throw new NotFoundException("Not found cluster with id" + id);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> checkClusterStatusDuringUpgrade(sdxCluster, pollingMessage));
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    private AttemptResult<StackV4Response> checkClusterStatusDuringUpgrade(SdxCluster sdxCluster, String pollingMessage) throws JsonProcessingException {
        LOGGER.info("{} polling cloudbreak for stack status: '{}' in '{}' env", pollingMessage, sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in inmemory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else if (hasActiveFlow(sdxCluster)) {
                LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.justContinue();
            } else {
                return getStackResponseAttemptResult(sdxCluster, pollingMessage);
            }
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private boolean hasActiveFlow(SdxCluster sdxCluster) {
        try {
            List<FlowLogResponse> flowLogsByResourceNameAndChainId =
                    flowEndpoint.getFlowLogsByResourceNameAndChainId(sdxCluster.getClusterName(), sdxCluster.getRepairFlowChainId());
            return flowLogsByResourceNameAndChainId.stream()
                    .anyMatch(flowLog -> flowLog.getStateStatus().equals(StateStatus.PENDING) || !flowLog.getFinalized());
        } catch (Exception e) {
            LOGGER.error("Exception occured during getting flow logs from CB: {}", e.getMessage());
            return false;
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, String pollingMessage) throws JsonProcessingException {
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
                return AttemptResults.justContinue();
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
}
