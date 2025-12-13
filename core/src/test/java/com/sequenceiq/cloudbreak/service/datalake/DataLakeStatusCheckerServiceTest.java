package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class DataLakeStatusCheckerServiceTest {

    private static final String ENVIRONMENT_CRN = "evn-crn";

    @InjectMocks
    private DataLakeStatusCheckerService underTest;

    @Mock
    private SdxClientService sdxClientService;

    @Test
    void testValidateRunningStateShouldNotThrowExceptionWhenTheSdxIsNotAvailable() {
        Stack stack = createStack();
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Collections.emptyList());

        underTest.validateRunningState(stack);
    }

    @Test
    void testValidateRunningStateShouldNotThrowExceptionWhenTheSdxIsInRunningState() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.RUNNING, "Running");
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateRunningState(stack);
    }

    @Test
    void testValidateRunningStateShouldThrowExceptionWhenTheSdxIsInUpgradeState() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, "Upgrading");
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        assertThrows(BadRequestException.class, () -> underTest.validateRunningState(stack));
    }

    @Test
    void testValidateAvailableStateShouldNotThrowExceptionWhenInBackup() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, "Backup in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateAvailableState(stack);
    }

    @Test
    void testValidateAvailableStateShouldNotThrowExceptionWhenRollingUpgradeInProgress() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_ROLLING_UPGRADE_IN_PROGRESS,
                "Rolling upgrade in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateAvailableState(stack);
    }

    @Test
    void testValidateAvailableStateShouldThrowExceptionWhenSdxIsNotAvailable() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                "Vertical Scale in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateAvailableState(stack));
        assertEquals("This action requires the Data Lake to be available, " +
                "but the status is 'DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS', Reason: 'Vertical Scale in Progress'.", e.getMessage());
    }

    @Test
    void testValidateAvailableStateShouldThrowExceptionWhenSdxIsNotAvailableAndStatusDetailIsNull() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                null);

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateAvailableState(stack));
        assertEquals("This action requires the Data Lake to be available, " +
                "but the status is 'DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS', Reason: ''.", e.getMessage());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

    private List<SdxClusterResponse> createSdxResponse(SdxClusterStatusResponse status, String statusReason) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(status);
        sdxClusterResponse.setStatusReason(statusReason);
        return Collections.singletonList(sdxClusterResponse);
    }
}