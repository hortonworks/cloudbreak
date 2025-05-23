package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.settings.SdxRepairSettings;
import com.sequenceiq.flow.api.model.FlowIdentifier;
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

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    public FlowIdentifier triggerRepairByCrn(String userCrn, String clusterCrn, SdxRepairRequest clusterRepairRequest) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(cluster);
        verifyIfRepairIsPossible(cluster);
        return sdxReactorFlowManager.triggerSdxRepairFlow(cluster, clusterRepairRequest);
    }

    public FlowIdentifier triggerRepairByName(String userCrn, String clusterName, SdxRepairRequest clusterRepairRequest) {
        SdxCluster cluster = sdxService.getByNameInAccount(userCrn, clusterName);
        MDCBuilder.buildMdcContext(cluster);
        verifyIfRepairIsPossible(cluster);
        return sdxReactorFlowManager.triggerSdxRepairFlow(cluster, clusterRepairRequest);
    }

    public void startSdxRepair(Long id, SdxRepairSettings repairRequest) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            startRepairInCb(sdxCluster, repairRequest);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    protected void startRepairInCb(SdxCluster sdxCluster, SdxRepairSettings repairRequest) {
        try {
            LOGGER.info("Triggering repair flow for cluster {} with hostgroups {}", sdxCluster.getClusterName(), repairRequest.getHostGroupNames());
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.repairClusterInternal(0L, sdxCluster.getClusterName(), createRepairRequest(repairRequest), initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REPAIR_IN_PROGRESS, "Datalake repair in progress", sdxCluster);
        } catch (NotFoundException e) {
            LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (ClientErrorException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not repair stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Cannot repair cluster, error happened during operation: " + errorMessage);
        } catch (WebApplicationException e) {
            LOGGER.info("Can not repair stack {} from cloudbreak: {}", sdxCluster.getStackId(), e.getMessage(), e);
            throw new RuntimeException("Cannot repair cluster, error happened during the operation: " + e.getMessage());
        }
    }

    private ClusterRepairV4Request createRepairRequest(SdxRepairSettings sdxRepairSettings) {
        ClusterRepairV4Request repairRequest = new ClusterRepairV4Request();
        if (CollectionUtils.isNotEmpty(sdxRepairSettings.getHostGroupNames())) {
            repairRequest.setHostGroups(sdxRepairSettings.getHostGroupNames());
        } else {
            ClusterRepairNodesV4Request nodes = new ClusterRepairNodesV4Request();
            nodes.setDeleteVolumes(sdxRepairSettings.isDeleteVolumes());
            nodes.setIds(sdxRepairSettings.getNodeIds());
            repairRequest.setNodes(nodes);
        }
        repairRequest.setRestartServices(true);
        return repairRequest;
    }

    private void verifyIfRepairIsPossible(SdxCluster cluster) {
        if (cluster.getClusterShape().isHA() && !entitlementService.haRepairEnabled(cluster.getAccountId())) {
            LOGGER.error("Cluster {} is {} and is not allowed to be repaired", cluster.getClusterName(), cluster.getClusterShape().name());
            throw new BadRequestException("Cannot repair " + cluster.getClusterShape().name() + " cluster " + cluster.getClusterName() + " at this time");
        }
    }

    public void waitCloudbreakClusterRepair(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster ->
            cloudbreakPoller.pollUpdateUntilAvailable("Repair", sdxCluster, pollingConfig), () -> {
                throw notFound("SDX cluster", id).get();
        });
    }
}
