package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class SaltServiceTest {
    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SaltService underTest;

    @Test
    void rotateSaltPassword() {
        SdxCluster sdxCluster = getSdxCluster();
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerSaltPasswordRotationTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateSaltPassword(sdxCluster));

        assertEquals(sdxFlowIdentifier, result);
        verify(sdxReactorFlowManager).triggerSaltPasswordRotationTracker(sdxCluster);
    }

    @Test
    public void testUpdateSalt() {
        SdxCluster sdxCluster = getSdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatus);
        when(sdxReactorFlowManager.triggerSaltUpdate(sdxCluster)).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = underTest.updateSalt(sdxCluster);

        verify(sdxReactorFlowManager, times(1)).triggerSaltUpdate(sdxCluster);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @ParameterizedTest
    @EnumSource(value = DatalakeStatusEnum.class, names = {"STOPPED", "STOP_IN_PROGRESS", "EXTERNAL_DATABASE_DELETION_IN_PROGRESS", "STACK_DELETED",
            "STACK_DELETION_IN_PROGRESS", "DELETE_REQUESTED", "DELETED", "DELETE_FAILED"}, mode = Mode.INCLUDE)
    public void testUpdateSaltThrowsBadRequestWhenDatalakeNotAvailable(DatalakeStatusEnum status) {
        SdxCluster sdxCluster = getSdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(status);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatus);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.updateSalt(sdxCluster));

        verifyNoInteractions(sdxReactorFlowManager);
        assertEquals(String.format("SaltStack update cannot be initiated as datalake 'test-sdx-cluster' is currently in '%s' state.", status), ex.getMessage());
    }
}