package com.sequenceiq.datalake.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeRecoveryFlowEventChainFactory;
import com.sequenceiq.datalake.flow.create.SdxCreateFlowConfig;
import com.sequenceiq.datalake.flow.delete.SdxDeleteFlowConfig;
import com.sequenceiq.datalake.flow.detach.SdxDetachFlowConfig;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFlowConfig;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFlowConfig;
import com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSFlowConfig;
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowConfig;
import com.sequenceiq.datalake.flow.stop.SdxStopFlowConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitFlowConfig;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxResizeOperationResponse;

@Service
public class SdxResizeOperationService {

    public static final String FAIL_NAME = "foldikppkdl";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeOperationService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private FlowService flowService;

    public SdxResizeOperationResponse getResizeOperationByStatusFromFlowLog(String environmentCrn) {
        List<SdxCluster> clusters = sdxService.listAllSdxByEnvCrn(ThreadBasedUserCrnProvider.getUserCrn(), environmentCrn);
        Optional<SdxResizeOperationResponse> resizeFromOriginalCluster = getSmallerCluster(clusters)
                .flatMap(originalCluster -> getFirstByFlowType(originalCluster.getId(), FlowChainInitFlowConfig.class, this::isResizeOrResizeRollback))
                .map(resizeInitFlowLog -> getResizeStatus(environmentCrn, resizeInitFlowLog.getFlowChainId(), getResizeOrRollbackFlowType(resizeInitFlowLog)));
        if (resizeFromOriginalCluster.isPresent()) {
            return resizeFromOriginalCluster.get();
        }
        return getClusterByShape(clusters, SdxClusterShape.ENTERPRISE::equals)
                .flatMap(resizedCluster -> getFirstByFlowType(resizedCluster.getId(), SdxCreateFlowConfig.class, flowLog -> true))
                .flatMap(flowLog -> flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowLog.getFlowChainId()))
                .filter(flowChainLog -> DatalakeResizeFlowEventChainFactory.class.getSimpleName().equals(flowChainLog.getFlowChainType()))
                .map(flowChainLog -> getResizeStatus(environmentCrn, flowChainLog.getFlowChainId(), DatalakeResizeFlowEventChainFactory.class))
                .orElseGet(this::noResizeOperation);
    }

    private Class<?> getResizeOrRollbackFlowType(FlowLog flowLog) {
        if (Strings.CS.contains(flowLog.getPayloadJackson(), DatalakeResizeFlowEventChainFactory.class.getSimpleName())) {
            return DatalakeResizeFlowEventChainFactory.class;
        } else {
            return DatalakeResizeRecoveryFlowEventChainFactory.class;
        }
    }

    private boolean isResizeOrResizeRollback(FlowLog flowLog) {
        return Strings.CS.contains(flowLog.getPayloadJackson(), DatalakeResizeFlowEventChainFactory.class.getSimpleName())
                || Strings.CS.contains(flowLog.getPayloadJackson(), DatalakeResizeRecoveryFlowEventChainFactory.class.getSimpleName());
    }

    private Optional<FlowLog> getFirstByFlowType(Long resourceId, Class<?> flowType, Predicate<FlowLog> condition) {
        return flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(resourceId, List.of(ClassValue.of(flowType)))
                .stream()
                .filter(condition)
                .findFirst();
    }

    private Optional<SdxCluster> getSmallerCluster(List<SdxCluster> clusters) {
        Optional<SdxCluster> originalCluster = getClusterByShape(clusters, SdxClusterShape.LIGHT_DUTY::equals);
        if (originalCluster.isEmpty()) {
            originalCluster = getClusterByShape(clusters, SdxClusterShape.MEDIUM_DUTY_HA::equals);
        }
        return originalCluster;
    }

    private SdxResizeOperationResponse getResizeStatus(String envCrn, String flowChainId, Class<?> flowChainType) {
        FlowCheckResponse flowChainState = flowService.getFlowChainState(flowChainId);
        if (DatalakeResizeRecoveryFlowEventChainFactory.class.equals(flowChainType)) {
            String message = getResizeRecoveryMessage(flowChainState);
            if (Boolean.TRUE.equals(flowChainState.getHasActiveFlow())) {
                return newSdxResizeOperation(flowChainId, true, false, "Data Lake scale up rollback is in progress.", false, false);
            } else if (Boolean.TRUE.equals(flowChainState.getLatestFlowFinalizedAndFailed())) {
                return newSdxResizeOperation(flowChainId, false, true, "Data Lake scale up rollback failed.", false, true);
            } else {
                return noResizeOperation();
            }
        } else if (Boolean.TRUE.equals(flowChainState.getHasActiveFlow())) {
            return newSdxResizeOperation(flowChainId, true, false, "Data Lake scale up operation is running.", false, false);
        } else if (Boolean.TRUE.equals(flowChainState.getLatestFlowFinalizedAndFailed())) {
            return mapFailedFlowTypeToStatus(flowChainId, flowChainState);
        }
        return noResizeOperation();
    }

    private SdxResizeOperationResponse mapFailedFlowTypeToStatus(String flowChainId, FlowCheckResponse flowChainState) {
        String flowType = flowChainState.getFlowType();
        if (classEquals(DatahubRefreshFlowConfig.class, flowType)) {
            return failedWithRetryAllowed(flowChainId, "Data Hub refresh operation failed.");
        } else if (classEquals(UpdateLoadBalancerDNSFlowConfig.class, flowType)) {
            return failedWithRetryAllowed(flowChainId, "Load balancer update failed.");
        } else if (classEquals(SdxDeleteFlowConfig.class, flowType)) {
            return failedWithRetryAllowed(flowChainId, "Failed to delete old data lake.");
        } else if (classEquals(DatalakeRestoreFlowConfig.class, flowType)) {
            return failedWithRetryAndRollbackAllowed(flowChainId, "Failed to restore data from old Data Lake to new Data Lake.");
        } else if (classEquals(SdxCreateFlowConfig.class, flowType)) {
            return failedWithRetryAndRollbackAllowed(flowChainId, "New Data Lake creation failed.");
        } else if (classEquals(SdxDetachFlowConfig.class, flowType)) {
            return failedWithRetryAndRollbackAllowed(flowChainId, "Failed to detach old Data Lake from environment.");
        } else if (classEquals(SdxStopFlowConfig.class, flowType)) {
            return failedWithRetryAndRollbackAllowed(flowChainId, "Failed to stop old Data Lake cluster.");
        } else if (classEquals(DatalakeBackupFlowConfig.class, flowType)) {
            return failedWithRetryAndRollbackAllowed(flowChainId, "Failed to create backup from old Data Lake cluster.");
        } else {
            return noResizeOperation();
        }
    }

    private SdxResizeOperationResponse noResizeOperation() {
        return null;
    }

    private String getResizeRecoveryMessage(FlowCheckResponse flowChainState) {
        if (Boolean.TRUE.equals(flowChainState.getHasActiveFlow())) {
            return "Data Lake scale up rollback is in progress.";
        } else if (Boolean.TRUE.equals(flowChainState.getLatestFlowFinalizedAndFailed())) {
            return "Data Lake scale up rollback failed.";
        } else {
            return "Data Lake scale up rollback completed.";
        }
    }

    private boolean classEquals(Class<? extends FlowConfiguration> flowConfigClass, String flowType) {
        return flowConfigClass.getName().equals(flowType);
    }

    private SdxResizeOperationResponse failedWithRetryAllowed(String flowChainId, String statusReason) {
        return newSdxResizeOperation(flowChainId, false, true, statusReason, false, true);
    }

    private SdxResizeOperationResponse failedWithRollbackAllowed(String flowChainId, String statusReason) {
        return newSdxResizeOperation(flowChainId, false, true, statusReason, true, false);
    }

    private SdxResizeOperationResponse failedWithRetryAndRollbackAllowed(String flowChainId, String statusReason) {
        return newSdxResizeOperation(flowChainId, false, true, statusReason, true, true);
    }

    private SdxResizeOperationResponse newSdxResizeOperation(String flowChainId, boolean active, boolean failed,
            String statusReason, boolean rollbackAllowed, boolean retryAllowed) {
        SdxResizeOperationResponse resizeOperation = new SdxResizeOperationResponse();
        resizeOperation.setOperationId(flowChainId);
        resizeOperation.setActive(active);
        resizeOperation.setFailed(failed);
        resizeOperation.setStatusReason(statusReason);
        resizeOperation.setRollbackAllowed(rollbackAllowed);
        resizeOperation.setRetryAllowed(retryAllowed);
        return resizeOperation;
    }

    private Optional<SdxCluster> getClusterByShape(List<SdxCluster> clusters, Predicate<SdxClusterShape> predicate) {
        return clusters.stream().filter(cluster -> predicate.test(cluster.getClusterShape())).findFirst();
    }
}
