package com.sequenceiq.environment.environment.service.freeipa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

@ExtendWith(MockitoExtension.class)
class FreeIpaServiceTest {

    private static final String OPERATION = "operation";

    private static final String ENVCRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setAccountId("acc")
            .setResource("env")
            .build().toString();

    private static final String USERCRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId("acc")
            .setResource("user")
            .build().toString();

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint;

    @Mock
    private UserV1Endpoint userV1Endpoint;

    @Mock
    private OperationV1Endpoint operationV1Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private EventSenderService eventService;

    @InjectMocks
    private FreeIpaService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(USERCRN);
    }

    @Test
    void synchronizeAllUsersInEnvironmentOngoing() {
        SyncOperationStatus status = createStatus(SynchronizationStatus.RUNNING, "");
        when(userV1Endpoint.getLastSyncOperationStatus(any())).thenReturn(status);
        SyncOperationStatus result = underTest.synchronizeAllUsersInEnvironment(ENVCRN);
        assertThat(result).isEqualTo(status);
        verify(userV1Endpoint, times(0)).synchronizeAllUsers(any());
    }

    @Test
    void synchronizeAllUsersInEnvironmentSuccess() {
        SyncOperationStatus status = createStatus(SynchronizationStatus.REQUESTED, "");
        when(userV1Endpoint.getLastSyncOperationStatus(any())).thenReturn(createStatus(SynchronizationStatus.COMPLETED, ""));
        when(userV1Endpoint.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(status);
        SyncOperationStatus result = underTest.synchronizeAllUsersInEnvironment(ENVCRN);
        assertThat(result).isEqualTo(status);
    }

    @Test
    void synchronizeAllUsersInEnvironmentFailure() {
        when(userV1Endpoint.getLastSyncOperationStatus(any())).thenReturn(createStatus(SynchronizationStatus.COMPLETED, ""));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
        when(userV1Endpoint.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenThrow(new WebApplicationException("network error"));
        assertThatThrownBy(() -> underTest.synchronizeAllUsersInEnvironment(ENVCRN)).isInstanceOf(FreeIpaOperationFailedException.class);
    }

    @Test
    void internalDescribeFreeipaNotFoundTest() {
        ExceptionResponse exceptionResponse = new ExceptionResponse("Freeipa not found");
        final Response response = mock(Response.class);
        when(response.readEntity(Mockito.any(Class.class))).thenReturn(exceptionResponse);
        NotFoundException notFoundException = mock(NotFoundException.class);
        when(notFoundException.getResponse()).thenReturn(response);
        when(freeIpaV1Endpoint.describeInternal(eq(ENVCRN), eq("1111"))).thenThrow(notFoundException);
        Optional<DescribeFreeIpaResponse> describeFreeIpaResponse = underTest.internalDescribe(ENVCRN, "1111");
        assertThat(describeFreeIpaResponse.isEmpty()).isEqualTo(true);
    }

    @Test
    void internalDescribeApiNotFoundTest() {
        when(freeIpaV1Endpoint.describeInternal(eq(ENVCRN), eq("1111")))
                .thenThrow(new jakarta.ws.rs.NotFoundException());
        FreeIpaOperationFailedException freeIpaOperationFailedException = assertThrows(FreeIpaOperationFailedException.class,
                () -> underTest.internalDescribe(ENVCRN, "1111"));
        String errorMessage = freeIpaOperationFailedException.getMessage();
        assertThat(errorMessage).isEqualTo("Freeipa internal describe response is NOT FOUND, but response reason is not the expected type");
    }

    private static SyncOperationStatus createStatus(SynchronizationStatus syncStatus, String error) {
        List<FailureDetails> failureDetails = new ArrayList<>();
        if (StringUtils.isNotBlank(error)) {
            failureDetails.add(new FailureDetails(ENVCRN, error));
        }
        return new SyncOperationStatus(OPERATION, SyncOperationType.USER_SYNC, syncStatus,
                List.of(), failureDetails, error, System.currentTimeMillis(), null);
    }

    @Test
    void getOperationStatusTest() {
        OperationStatus status = new OperationStatus();
        status.setStatus(OperationState.RUNNING);
        when(operationV1Endpoint.getOperationStatus("operationId", "acc")).thenReturn(status);

        OperationStatus result = ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.getOperationStatus("operationId"));
        assertThat(result).isEqualTo(status);
    }

    @Test
    void getOperationStatusFailureTest() {
        when(operationV1Endpoint.getOperationStatus("operationId", "acc")).thenThrow(new WebApplicationException("not found"));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.getOperationStatus("operationId")))
                .hasMessage("custom error")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);
    }

    @Test
    void getLastFlowIdWithFlowTest() {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setFlowId("flowId123");
        when(freeIpaV1FlowEndpoint.getLastFlowByResourceCrn(ENVCRN)).thenReturn(flowLogResponse);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.getLastFlowId(ENVCRN));
        assertThat(flowIdentifier.getType()).isEqualTo(FlowType.FLOW);
        assertThat(flowIdentifier.getPollableId()).isEqualTo("flowId123");
    }

    @Test
    void getLastFlowIdWithFlowChainTest() {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setFlowChainId("flowId123");
        when(freeIpaV1FlowEndpoint.getLastFlowByResourceCrn(ENVCRN)).thenReturn(flowLogResponse);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.getLastFlowId(ENVCRN));
        assertThat(flowIdentifier.getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(flowIdentifier.getPollableId()).isEqualTo("flowId123");
    }

    @Test
    void upgradeCcmTest() {
        OperationStatus status = new OperationStatus("123", OperationType.UPGRADE_CCM, OperationState.REQUESTED, null, null, null, 0, null);
        when(freeIpaV1Endpoint.upgradeCcmInternal(ENVCRN, USERCRN)).thenReturn(status);
        OperationStatus result = ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.upgradeCcm(ENVCRN));
        assertThat(result).isEqualTo(status);
    }

    @Test
    void upgradeCcmFailureTest() {
        when(freeIpaV1Endpoint.upgradeCcmInternal(ENVCRN, USERCRN)).thenThrow(new WebApplicationException("Houston..."));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.upgradeCcm(ENVCRN)))
                .hasMessage("custom error")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);
    }
}
