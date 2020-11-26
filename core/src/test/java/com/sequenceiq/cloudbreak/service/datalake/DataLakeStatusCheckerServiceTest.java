package com.sequenceiq.cloudbreak.service.datalake;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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