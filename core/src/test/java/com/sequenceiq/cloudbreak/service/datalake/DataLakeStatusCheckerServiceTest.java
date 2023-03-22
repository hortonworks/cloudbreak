package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@RunWith(MockitoJUnitRunner.class)
public class DataLakeStatusCheckerServiceTest {

    private static final String ENVIRONMENT_CRN = "evn-crn";

    @InjectMocks
    private DataLakeStatusCheckerService underTest;

    @Mock
    private SdxClientService sdxClientService;

    @Test
    public void testValidateRunningStateShouldNotThrowExceptionWhenTheSdxIsNotAvailable() {
        Stack stack = createStack();
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Collections.emptyList());

        underTest.validateRunningState(stack);
    }

    @Test
    public void testValidateRunningStateShouldNotThrowExceptionWhenTheSdxIsInRunningState() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.RUNNING, "Running");
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateRunningState(stack);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateRunningStateShouldThrowExceptionWhenTheSdxIsInUpgradeState() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, "Upgrading");
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateRunningState(stack);
    }

    @Test
    public void testValidateAvailableStateShouldNotThrowExceptionWhenInBackup() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, "Backup in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateAvailableState(stack);
    }

    @Test
    public void testValidateAvailableStateShouldNotThrowExceptionWhenRollingUpgradeInProgress() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_ROLLING_UPGRADE_IN_PROGRESS,
                "Rolling upgrade in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        underTest.validateAvailableState(stack);
    }

    @Test
    public void testValidateAvailableStateShouldThrowExceptionWhenSdxIsNotAvailable() {
        Stack stack = createStack();
        List<SdxClusterResponse> sdxClusterResponses = createSdxResponse(SdxClusterStatusResponse.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                "Vertical Scale in Progress");

        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(sdxClusterResponses);

        assertThrows(BadRequestException.class, () -> underTest.validateAvailableState(stack));
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