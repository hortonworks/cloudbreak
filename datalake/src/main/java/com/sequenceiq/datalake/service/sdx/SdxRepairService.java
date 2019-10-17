package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Service
public class SdxRepairService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void triggerRepairByCrn(String userCrn, String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(cluster);
        sdxReactorFlowManager.triggerSdxRepairFlow(cluster.getId(), clusterRepairRequest);
    }

    public void triggerRepairByName(String userCrn, String clusterName, SdxRepairRequest clusterRepairRequest) {
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        MDCBuilder.buildMdcContext(cluster);
        sdxReactorFlowManager.triggerSdxRepairFlow(cluster.getId(), clusterRepairRequest);
    }

    public void startSdxRepair(Long id, SdxRepairRequest repairRequest) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            startRepairInCb(sdxCluster, repairRequest);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    protected void startRepairInCb(SdxCluster sdxCluster, SdxRepairRequest repairRequest) {
        try {
            LOGGER.info("Triggering repair flow for cluster {} with hostgroups {}", sdxCluster.getClusterName(), repairRequest.getHostGroupName());
            stackV4Endpoint.repairCluster(0L, sdxCluster.getClusterName(), createRepairRequest(repairRequest));
            cloudbreakFlowService.setCloudbreakFlowChainId(sdxCluster);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REPAIR_IN_PROGRESS, ResourceEvent.SDX_REPAIR_STARTED,
                    "Datalake repair in progress", sdxCluster);
        } catch (NotFoundException e) {
            LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (ClientErrorException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not repair stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Can not repair stack, client error happened on Cloudbreak side: " + errorMessage);
        } catch (WebApplicationException e) {
            LOGGER.info("Can not repair stack {} from cloudbreak: {}", sdxCluster.getStackId(), e.getMessage(), e);
            throw new RuntimeException("Can not repair stack, client error happened on Cloudbreak side: " + e.getMessage());
        }
    }

    private ClusterRepairV4Request createRepairRequest(SdxRepairRequest sdxRepairRequest) {
        ClusterRepairV4Request repairRequest = new ClusterRepairV4Request();
        List<String> hostGroupNames;
        if (StringUtils.isNotBlank(sdxRepairRequest.getHostGroupName())) {
            hostGroupNames = List.of(sdxRepairRequest.getHostGroupName());
        } else {
            hostGroupNames = sdxRepairRequest.getHostGroupNames();
        }
        repairRequest.setHostGroups(hostGroupNames);
        return repairRequest;
    }

    public void waitCloudbreakClusterRepair(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> checkClusterStatusDuringRepair(sdxCluster));
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_REPAIR_FINISHED, "Datalake is running", sdxCluster);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    protected AttemptResult<StackV4Response> checkClusterStatusDuringRepair(SdxCluster sdxCluster) throws JsonProcessingException {
        LOGGER.info("Repair polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("Repair polling cancelled in inmemory store, id: " + sdxCluster.getId());
                return AttemptResults.breakFor("Repair polling cancelled in inmemory store, id: " + sdxCluster.getId());
            }
            if (cloudbreakFlowService.hasActiveFlow(sdxCluster)) {
                LOGGER.info("Repair polling will continue, cluster has an active flow in Cloudbreak, id: " + sdxCluster.getId());
                return AttemptResults.justContinue();
            } else {
                return getStackResponseAttemptResult(sdxCluster);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster) throws JsonProcessingException {
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (stackAndClusterAvailable(stackV4Response, cluster)) {
            return AttemptResults.finishWith(stackV4Response);
        } else {
            if (Status.UPDATE_FAILED.equals(stackV4Response.getStatus())) {
                LOGGER.info("Stack repair failed for Stack {} with status {}", stackV4Response.getName(), stackV4Response.getStatus());
                return sdxRepairFailed(sdxCluster, stackV4Response.getStatusReason());
            } else if (Status.UPDATE_FAILED.equals(stackV4Response.getCluster().getStatus())) {
                LOGGER.info("Cluster repair failed for Cluster {} status {}", stackV4Response.getCluster().getName(),
                        stackV4Response.getCluster().getStatus());
                return sdxRepairFailed(sdxCluster, stackV4Response.getCluster().getStatusReason());
            } else {
                return AttemptResults.justContinue();
            }
        }
    }

    private AttemptResult<StackV4Response> sdxRepairFailed(SdxCluster sdxCluster, String statusReason) {
        LOGGER.info("SDX repair failed, statusReason: " + statusReason);
        return AttemptResults.breakFor("SDX repair failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }
}
