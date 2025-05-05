package com.sequenceiq.datalake.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeValidationResultProviderTest {

    private static final String FLOW_ID = "1";

    private static final String CLUSTER_UPGRADE_VALIDATION_INIT_STATE = "CLUSTER_UPGRADE_VALIDATION_INIT_STATE";

    private static final String CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE = "CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE";

    @InjectMocks
    private SdxUpgradeValidationResultProvider underTest;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxCluster sdxCluster;

    @Test
    void testIsValidationFailedShouldReturnTrue() {
        FlowLogResponse flowLogResponse = createLastFlowLog();
        when(cloudbreakFlowService.getLastFlowId(sdxCluster)).thenReturn(flowLogResponse);
        when(cloudbreakFlowService.getFlowLogsByFlowId(FLOW_ID)).thenReturn(createFlowLogsWithFailedValidation());

        assertTrue(underTest.isValidationFailed(sdxCluster));
        verify(cloudbreakFlowService).getLastFlowId(sdxCluster);
        verify(cloudbreakFlowService).getFlowLogsByFlowId(FLOW_ID);
    }

    @Test
    void testIsValidationFailedShouldReturnFalseWhenTheFlowDoesNotContainsValidationInitState() {
        FlowLogResponse flowLogResponse = createLastFlowLog();
        when(cloudbreakFlowService.getLastFlowId(sdxCluster)).thenReturn(flowLogResponse);
        when(cloudbreakFlowService.getFlowLogsByFlowId(FLOW_ID)).thenReturn(createFlowLogsWithOutValidationInitState());

        assertFalse(underTest.isValidationFailed(sdxCluster));
        verify(cloudbreakFlowService).getLastFlowId(sdxCluster);
        verify(cloudbreakFlowService).getFlowLogsByFlowId(FLOW_ID);
    }

    @Test
    void testIsValidationFailedShouldReturnFalseWhenTheFlowDoesNotContainsFailedState() {
        FlowLogResponse flowLogResponse = createLastFlowLog();
        when(cloudbreakFlowService.getLastFlowId(sdxCluster)).thenReturn(flowLogResponse);
        when(cloudbreakFlowService.getFlowLogsByFlowId(FLOW_ID)).thenReturn(createFlowLogsWithOutFailedState());

        assertFalse(underTest.isValidationFailed(sdxCluster));
        verify(cloudbreakFlowService).getLastFlowId(sdxCluster);
        verify(cloudbreakFlowService).getFlowLogsByFlowId(FLOW_ID);
    }

    @Test
    void testIsValidationFailedShouldReturnFalseWhenTheLastFlowIsMissing() {
        when(cloudbreakFlowService.getLastFlowId(sdxCluster)).thenReturn(null);

        assertFalse(underTest.isValidationFailed(sdxCluster));
        verify(cloudbreakFlowService).getLastFlowId(sdxCluster);
        verify(cloudbreakFlowService, never()).getFlowLogsByFlowId(FLOW_ID);
    }

    private List<FlowLogResponse> createFlowLogsWithFailedValidation() {
        return List.of(
                createFlowLog(CLUSTER_UPGRADE_VALIDATION_INIT_STATE, StateStatus.SUCCESSFUL),
                createFlowLog(CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE, StateStatus.FAILED));
    }

    private List<FlowLogResponse> createFlowLogsWithOutValidationInitState() {
        return List.of(
                createFlowLog(CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE, StateStatus.FAILED));
    }

    private List<FlowLogResponse> createFlowLogsWithOutFailedState() {
        return List.of(
                createFlowLog(CLUSTER_UPGRADE_VALIDATION_INIT_STATE, StateStatus.SUCCESSFUL));
    }

    private FlowLogResponse createLastFlowLog() {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setFlowId(FLOW_ID);
        return flowLogResponse;
    }

    private FlowLogResponse createFlowLog(String currentState, StateStatus stateStatus) {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setStateStatus(stateStatus);
        flowLogResponse.setCurrentState(currentState);
        return flowLogResponse;
    }
}