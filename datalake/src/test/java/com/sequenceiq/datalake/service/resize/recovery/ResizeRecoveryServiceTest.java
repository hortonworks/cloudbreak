package com.sequenceiq.datalake.service.resize.recovery;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOPPED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOP_FAILED;
import static com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService.FAILURE_STATES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@ExtendWith(MockitoExtension.class)
class ResizeRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    @Mock
    private SdxCluster cluster;

    @Mock
    private SdxCluster otherCluster;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @InjectMocks
    private ResizeRecoveryService underTest;

    private SdxRecoveryRequest request;

    private final SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();

    private final FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "FLOW_ID");

    @BeforeEach
    public void setup() {
        request = new SdxRecoveryRequest();
        request.setType(SdxRecoveryType.RECOVER_WITHOUT_DATA);
        lenient().when(sdxStatusService.getActualStatusForSdx(cluster)).thenReturn(sdxStatusEntity);
        lenient().when(entitlementService.isDatalakeResizeRecoveryEnabled(anyString())).thenReturn(true);
        lenient().when(sdxReactorFlowManager.triggerSdxResizeRecovery(any(), any())).thenReturn(flowId);
        lenient().when(cluster.getId()).thenReturn(0L);
        lenient().when(otherCluster.getId()).thenReturn(1L);
        lenient().when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(otherCluster));
        sdxStatusEntity.setStatusReason("");
    }

    public void testGetClusterRecoverableForStatusNotRecoverable(DatalakeStatusEnum status) {
        sdxStatusEntity.setStatus(status);
        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster));
        assertEquals(RecoveryStatus.NON_RECOVERABLE, sdxRecoverableResponse.getStatus(), status + " should be non-recoverable");
    }

    public void testGetClusterRecoverableForStatusRecoverable(DatalakeStatusEnum status) {
        sdxStatusEntity.setStatus(status);
        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster));
        assertEquals(RecoveryStatus.RECOVERABLE, sdxRecoverableResponse.getStatus(), status + " should be recoverable");
    }

    @Test
    public void testStatusReasonNullNoError() {
        sdxStatusEntity.setStatusReason(null);
        testGetClusterRecoverableForStatusNotRecoverable(RUNNING);
    }

    @Test
    public void testRestoreFailedAndRunningRecoverable() {
        sdxStatusEntity.setStatusReason("Datalake is running, Datalake restore failed");
        testGetClusterRecoverableForStatusRecoverable(RUNNING);
    }

    @Test
    public void testRestoreNotFailedAndRunningNotRecoverable() {
        sdxStatusEntity.setStatusReason("Datalake is running");
        testGetClusterRecoverableForStatusNotRecoverable(RUNNING);
    }

    @Test
    public void testKnownNonRecoverableStates() {
        for (DatalakeStatusEnum datalakeStatusEnum : DatalakeStatusEnum.values()) {
            if (!FAILURE_STATES.contains(datalakeStatusEnum)) {
                testGetClusterRecoverableForStatusNotRecoverable(datalakeStatusEnum);
            }
        }
    }

    @Test
    public void testKnownRecoverableStates() {
        for (DatalakeStatusEnum datalakeStatusEnum : FAILURE_STATES) {
            testGetClusterRecoverableForStatusRecoverable(datalakeStatusEnum);
        }
    }

    @Test
    public void testNoEntitlementValidate() {
        when(entitlementService.isDatalakeResizeRecoveryEnabled(anyString())).thenReturn(false);
        SdxRecoverableResponse sdxRecoverableResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster));
        assertEquals(RecoveryStatus.NON_RECOVERABLE, sdxRecoverableResponse.getStatus());
        assertEquals("Resize Recovery entitlement not enabled", sdxRecoverableResponse.getReason());
    }

    @Test
    public void testTriggerRecoveryShouldStartFlow() {
        SdxRecoveryResponse sdxRecoveryResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerRecovery(cluster, request));
        verify(sdxReactorFlowManager).triggerSdxResizeRecovery(otherCluster, Optional.ofNullable(cluster));
        assertEquals(flowId, sdxRecoveryResponse.getFlowIdentifier());
    }

    @Test
    public void testOnlyOneClusterStatusNullNoError() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.empty());
        testGetClusterRecoverableForStatusNotRecoverable(null);
    }

    @Test
    public void testOnlyOneClusterStatusReasonNullNoError() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.empty());
        sdxStatusEntity.setStatusReason(null);
        testGetClusterRecoverableForStatusNotRecoverable(STOP_FAILED);
    }

    @Test
    public void testOnlyOneClusterStoppedAndDetachedRecoverable() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(cluster));
        when(cluster.isDetached()).thenReturn(true);
        testGetClusterRecoverableForStatusRecoverable(STOPPED);
    }

    @Test
    public void testOnlyOneClusterStoppedDuringResizeRecoverable() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.empty());
        sdxStatusEntity.setStatusReason("SDX detach failed");
        testGetClusterRecoverableForStatusRecoverable(STOPPED);
    }

    @Test
    public void testOnlyOneClusterStopFailedDuringResizeRecoverable() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.empty());
        sdxStatusEntity.setStatusReason("Datalake resize failure");
        testGetClusterRecoverableForStatusRecoverable(STOP_FAILED);
    }

    @Test
    public void testOnlyOneClusterTriggerRecoveryShouldStartFlow() {
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.empty());
        SdxRecoveryResponse sdxRecoveryResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerRecovery(cluster, request));
        verify(sdxReactorFlowManager).triggerSdxResizeRecovery(cluster, Optional.empty());
        assertEquals(flowId, sdxRecoveryResponse.getFlowIdentifier());
    }
}
