package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
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
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxResizeOperationResponse;

@ExtendWith(MockitoExtension.class)
class SdxResizeOperationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_CRN = "envCrn";

    private static final String FLOW_ID = "flow1";

    private static final String FLOW_CHAIN_ID = "flowChain1";

    @Mock
    private SdxService sdxService;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private FlowService flowService;

    @InjectMocks
    private SdxResizeOperationService underTest;

    private AtomicLong clusterIdSeq = new AtomicLong(1);

    static Object[][] resizeFailureProvider() {
        return new Object[][]{
                {DatalakeBackupFlowConfig.class, failedOperation("Failed to create backup from old Data Lake cluster.", true, true), true},
                {SdxStopFlowConfig.class, failedOperation("Failed to stop old Data Lake cluster.", true, true), true},
                {SdxDetachFlowConfig.class, failedOperation("Failed to detach old Data Lake from environment.", true, true), true},
                {SdxCreateFlowConfig.class, failedOperation("New Data Lake creation failed.", true, true), true},
                {DatalakeRestoreFlowConfig.class, failedOperation("Failed to restore data from old Data Lake to new Data Lake.", true, true), true},
                {SdxDeleteFlowConfig.class, failedOperation("Failed to delete old data lake.", false, true), true},
                {UpdateLoadBalancerDNSFlowConfig.class, failedOperation("Load balancer update failed.", false, true), false},
                {DatahubRefreshFlowConfig.class, failedOperation("Data Hub refresh operation failed.", false, true), false},
        };
    }

    @Test
    public void testLightDutyClusterNoResizeFlowLogs() {
        setUpClusters(lightDuty());

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertNull(result);
    }

    @Test
    public void testEnterpriseClusterNoResizeFlowLogs() {
        setUpClusters(enterprise());

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertNull(result);
    }

    @Test
    public void testMediumDutyClusterNoResizeFlowLogs() {
        setUpClusters(mediumDuty());

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertNull(result);
    }

    @Test
    public void testLightDutyToEnterpriseScaleUpInProgress() {
        SdxCluster enterprise = enterprise();
        SdxCluster lightDuty = lightDuty();
        setUpClusters(enterprise, lightDuty);
        setUpResizeFlowChainWithOldCluster(lightDuty);

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertEquals(FLOW_CHAIN_ID, result.getOperationId());
        assertEquals("Data Lake scale up operation is running.", result.getStatusReason());
        assertEquals(true, result.getActive());
        assertEquals(false, result.getFailed());
        assertEquals(false, result.getRetryAllowed());
        assertEquals(false, result.getRollbackAllowed());
    }

    @Test
    public void testLightDutyToEnterpriseScaleUpRollbackFailed() {
        SdxCluster enterprise = enterprise();
        SdxCluster lightDuty = lightDuty();
        setUpClusters(enterprise, lightDuty);
        setUpResizeRollbackFlowChainWithOldCluster(lightDuty, SdxDetachFlowConfig.class);

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertEquals(FLOW_CHAIN_ID, result.getOperationId());
        assertEquals("Data Lake scale up rollback failed.", result.getStatusReason());
        assertEquals(false, result.getActive());
        assertEquals(true, result.getFailed());
        assertEquals(true, result.getRetryAllowed());
        assertEquals(false, result.getRollbackAllowed());
    }

    @ParameterizedTest
    @MethodSource("resizeFailureProvider")
    public void testLightDutyToEnterpriseScaleUpFailures(Class<?> failureClass, SdxResizeOperationResponse expectedResponse, boolean oldDataLakeIsVisible) {
        SdxCluster enterprise = enterprise();
        if (oldDataLakeIsVisible) {
            SdxCluster lightDuty = lightDuty();
            setUpClusters(enterprise, lightDuty);
            setUpResizeFlowChainWithOldCluster(lightDuty, failureClass);
        } else {
            setUpClusters(enterprise);
            setUpResizeFlowChainWithNewCluster(enterprise, failureClass);
        }

        SdxResizeOperationResponse result = getResizeOperationByStatus();

        assertEquals(expectedResponse.getOperationId(), result.getOperationId());
        assertEquals(expectedResponse.getActive(), result.getActive());
        assertEquals(expectedResponse.getFailed(), result.getFailed());
        assertEquals(expectedResponse.getRetryAllowed(), result.getRetryAllowed());
        assertEquals(expectedResponse.getRollbackAllowed(), result.getRollbackAllowed());
    }

    private SdxResizeOperationResponse getResizeOperationByStatus() {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResizeOperationByStatusFromFlowLog(ENV_CRN));
    }

    private void setUpClusters(SdxCluster... clusters) {
        when(sdxService.listAllSdxByEnvCrn(USER_CRN, ENV_CRN)).thenReturn(Arrays.stream(clusters).toList());
    }

    private SdxCluster lightDuty() {
        return cluster(SdxClusterShape.LIGHT_DUTY);
    }

    private SdxCluster mediumDuty() {
        return cluster(SdxClusterShape.MEDIUM_DUTY_HA);
    }

    private SdxCluster enterprise() {
        return cluster(SdxClusterShape.ENTERPRISE);
    }

    private SdxCluster cluster(SdxClusterShape shape) {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(clusterIdSeq.getAndIncrement());
        cluster.setClusterShape(shape);
        return cluster;
    }

    private void setUpResizeFlowChainWithOldCluster(SdxCluster cluster) {
        setUpResizeFlowChainWithOldCluster(cluster, null);
    }

    private void setUpResizeFlowChainWithOldCluster(SdxCluster cluster, Class<?> failedFlowType) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowChainId(FLOW_CHAIN_ID);
        flowLog.setPayloadJackson("{\"flowChainName\": DatalakeResizeFlowEventChainFactory\"}");
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(cluster.getId(), List.of(ClassValue.of(FlowChainInitFlowConfig.class))))
                .thenReturn(List.of(flowLog));
        FlowCheckResponse flowCheck = new FlowCheckResponse();
        flowCheck.setFlowId(FLOW_ID);
        flowCheck.setFlowChainId(FLOW_CHAIN_ID);
        if (failedFlowType == null) {
            flowCheck.setHasActiveFlow(true);
            flowCheck.setLatestFlowFinalizedAndFailed(false);
        } else {
            flowCheck.setHasActiveFlow(false);
            flowCheck.setLatestFlowFinalizedAndFailed(true);
            flowCheck.setFlowType(failedFlowType.getName());
        }
        when(flowService.getFlowChainState(FLOW_CHAIN_ID)).thenReturn(flowCheck);
    }

    private void setUpResizeRollbackFlowChainWithOldCluster(SdxCluster cluster, Class<?> failedFlowType) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowChainId(FLOW_CHAIN_ID);
        flowLog.setPayloadJackson("{\"flowChainName\": DatalakeResizeRecoveryFlowEventChainFactory\"}");
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(cluster.getId(), List.of(ClassValue.of(FlowChainInitFlowConfig.class))))
                .thenReturn(List.of(flowLog));
        FlowCheckResponse flowCheck = new FlowCheckResponse();
        flowCheck.setFlowId(FLOW_ID);
        flowCheck.setFlowChainId(FLOW_CHAIN_ID);
        if (failedFlowType == null) {
            flowCheck.setHasActiveFlow(true);
            flowCheck.setLatestFlowFinalizedAndFailed(false);
        } else {
            flowCheck.setHasActiveFlow(false);
            flowCheck.setLatestFlowFinalizedAndFailed(true);
            flowCheck.setFlowType(failedFlowType.getName());
        }
        when(flowService.getFlowChainState(FLOW_CHAIN_ID)).thenReturn(flowCheck);
    }

    private void setUpResizeFlowChainWithNewCluster(SdxCluster cluster, Class<?> failedFlowType) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowChainId(FLOW_CHAIN_ID);
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(cluster.getId(), List.of(ClassValue.of(SdxCreateFlowConfig.class))))
                .thenReturn(List.of(flowLog));
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(FLOW_CHAIN_ID);
        flowChainLog.setFlowChainType(DatalakeResizeFlowEventChainFactory.class.getSimpleName());
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID))
                .thenReturn(Optional.of(flowChainLog));
        FlowCheckResponse flowCheck = new FlowCheckResponse();
        flowCheck.setFlowId(FLOW_ID);
        flowCheck.setFlowChainId(FLOW_CHAIN_ID);
        if (failedFlowType == null) {
            flowCheck.setHasActiveFlow(true);
            flowCheck.setLatestFlowFinalizedAndFailed(false);
        } else {
            flowCheck.setHasActiveFlow(false);
            flowCheck.setLatestFlowFinalizedAndFailed(true);
            flowCheck.setFlowType(failedFlowType.getName());
        }
        when(flowService.getFlowChainState(FLOW_CHAIN_ID)).thenReturn(flowCheck);
    }

    private static SdxResizeOperationResponse failedOperation(String statusReason, boolean rollback, boolean retry) {
        SdxResizeOperationResponse operation = new SdxResizeOperationResponse();
        operation.setOperationId(FLOW_CHAIN_ID);
        operation.setStatusReason(statusReason);
        operation.setActive(false);
        operation.setFailed(true);
        operation.setRollbackAllowed(rollback);
        operation.setRetryAllowed(retry);
        return operation;
    }
}