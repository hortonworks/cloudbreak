package com.sequenceiq.datalake.service.resize.recovery;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOPPED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOP_FAILED;
import static com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService.FAILURE_STATES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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
public class ResizeRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    @Mock
    private SdxCluster cluster;

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
        lenient().when(sdxReactorFlowManager.triggerSdxStartFlow(cluster))
                .thenReturn(flowId);
        lenient().when(cluster.getClusterName()).thenReturn("new");
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
    public void testStatusNullNoError() {
        testGetClusterRecoverableForStatusNotRecoverable(null);
    }

    @Test
    public void testRestoreFailedAndRunningRecoverable() {
        SdxCluster old = new SdxCluster();
        old.setClusterName("old");
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(old));
        sdxStatusEntity.setStatusReason("Datalake is running, Datalake restore failed");
        testGetClusterRecoverableForStatusRecoverable(DatalakeStatusEnum.RUNNING);
    }

    @Test
    public void testRestoreNotFailedAndRunningNotRecoverable() {
        SdxCluster old = new SdxCluster();
        old.setClusterName("old");
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(old));
        sdxStatusEntity.setStatusReason("Datalake is running");
        testGetClusterRecoverableForStatusNotRecoverable(DatalakeStatusEnum.RUNNING);
    }

    @Test
    public void testKnownNonRecoverableStates() {
        SdxCluster old = new SdxCluster();
        old.setClusterName("old");
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(old));
        for (DatalakeStatusEnum datalakeStatusEnum : DatalakeStatusEnum.values()) {
            if (!FAILURE_STATES.contains(datalakeStatusEnum)) {
                testGetClusterRecoverableForStatusNotRecoverable(datalakeStatusEnum);
            }
        }
    }

    @Test
    public void testKnownRecoverableStates() {
        SdxCluster old = new SdxCluster();
        old.setClusterName("old");
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(old));
        for (DatalakeStatusEnum datalakeStatusEnum : FAILURE_STATES) {
            testGetClusterRecoverableForStatusRecoverable(datalakeStatusEnum);
        }
    }

    @Test
    public void testTriggerRecoveryShouldStartFlow() {
        sdxStatusEntity.setStatus(STOP_FAILED);

        SdxRecoveryResponse sdxRecoveryResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerRecovery(cluster, request));

        verify(sdxReactorFlowManager).triggerSdxStartFlow(cluster);
        assertEquals(flowId, sdxRecoveryResponse.getFlowIdentifier());

    }

    @Test
    public void testNoEntitlementValidate() {
        when(entitlementService.isDatalakeResizeRecoveryEnabled(anyString())).thenReturn(false);

        sdxStatusEntity.setStatus(STOP_FAILED);
        SdxRecoverableResponse sdxRecoverableResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster));
        assertEquals(RecoveryStatus.NON_RECOVERABLE, sdxRecoverableResponse.getStatus());
        assertEquals("Resize Recovery entitlement not enabled", sdxRecoverableResponse.getReason());
    }

    @Test
    public void testNoEntitlementTrigger() {
        when(entitlementService.isDatalakeResizeRecoveryEnabled(anyString())).thenReturn(false);

        sdxStatusEntity.setStatus(STOP_FAILED);
                BadRequestException result = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerRecovery(cluster, request)));
        assertEquals("Entitlement for resize recovery is missing", result.getMessage());

    }

    @Test
    public void testStoppedAndDetachedRecoverable() {
        SdxCluster old = new SdxCluster();
        old.setClusterName("old");
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(any(), any()))
                .thenReturn(Optional.of(old));
        when(cluster.getClusterName()).thenReturn("old");
        when(cluster.isDetached()).thenReturn(true);
        testGetClusterRecoverableForStatusRecoverable(STOPPED);
    }

    @Test
    public void testStoppedDuringResizeRecoverable() {
        sdxStatusEntity.setStatusReason("SDX detach failed");
        testGetClusterRecoverableForStatusRecoverable(STOPPED);
    }

    @Test
    public void testStopFailedDuringResizeRecoverable() {
        sdxStatusEntity.setStatusReason("Datalake resize failure");
        testGetClusterRecoverableForStatusRecoverable(STOP_FAILED);
    }
}
