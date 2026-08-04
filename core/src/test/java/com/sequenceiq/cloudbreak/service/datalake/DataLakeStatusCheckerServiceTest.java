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
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperations;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class DataLakeStatusCheckerServiceTest {

    private static final String ENVIRONMENT_CRN = "evn-crn";

    @InjectMocks
    private DataLakeStatusCheckerService underTest;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

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
    void testValidateStartOperationBasedOnDatalakeShouldNotThrowException() {
        Stack stack = createStack();
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setAllowed(true);
        distroXOperationValidationView.setOperation(DistroXOperations.START);
        DistroXOperationValidationView distroXOperationValidationView1 = new DistroXOperationValidationView();
        distroXOperationValidationView1.setOperation(DistroXOperations.CREATE);
        distroXOperationValidationView1.setAllowed(true);
        when(platformAwareSdxConnector.validateDistroxOperations(ENVIRONMENT_CRN))
                .thenReturn(List.of(distroXOperationValidationView, distroXOperationValidationView1));

        underTest.validateStartOperationBasedOnDatalake(stack);
    }

    @Test
    void testValidateStartOperationBasedOnDatalakeShouldNotThrowExceptionCreateNotAllowed() {
        Stack stack = createStack();
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setAllowed(true);
        distroXOperationValidationView.setOperation(DistroXOperations.START);
        DistroXOperationValidationView distroXOperationValidationView1 = new DistroXOperationValidationView();
        distroXOperationValidationView1.setOperation(DistroXOperations.CREATE);
        distroXOperationValidationView1.setAllowed(false);
        when(platformAwareSdxConnector.validateDistroxOperations(ENVIRONMENT_CRN))
                .thenReturn(List.of(distroXOperationValidationView, distroXOperationValidationView1));

        underTest.validateStartOperationBasedOnDatalake(stack);
    }

    @Test
    void testValidateAvailableStateShouldThrowExceptionWhenStartIsNotAvailable() {
        Stack stack = createStack();
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setAllowed(false);
        distroXOperationValidationView.setOperation(DistroXOperations.START);
        distroXOperationValidationView.setReason("Instance health check failed for SDX cluster");
        DistroXOperationValidationView distroXOperationValidationView1 = new DistroXOperationValidationView();
        distroXOperationValidationView1.setOperation(DistroXOperations.CREATE);
        distroXOperationValidationView1.setAllowed(true);
        when(platformAwareSdxConnector.validateDistroxOperations(ENVIRONMENT_CRN))
                .thenReturn(List.of(distroXOperationValidationView, distroXOperationValidationView1));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateStartOperationBasedOnDatalake(stack));
        assertEquals("Data Hub start is not allowed due to Data Lake being unavailable. Reason: " +
                "'Instance health check failed for SDX cluster'.", e.getMessage());
    }

    @Test
    void testValidateAvailableStateShouldThrowExceptionWhenSdxInstanceIsNotAvailableAndStatusDetailIsNull() {
        Stack stack = createStack();
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setAllowed(false);
        distroXOperationValidationView.setOperation(DistroXOperations.START);
        DistroXOperationValidationView distroXOperationValidationView1 = new DistroXOperationValidationView();
        distroXOperationValidationView1.setOperation(DistroXOperations.CREATE);
        distroXOperationValidationView1.setAllowed(true);
        when(platformAwareSdxConnector.validateDistroxOperations(ENVIRONMENT_CRN))
                .thenReturn(List.of(distroXOperationValidationView, distroXOperationValidationView1));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateStartOperationBasedOnDatalake(stack));
        assertEquals("Data Hub start is not allowed due to Data Lake being unavailable. Reason: ''.", e.getMessage());
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