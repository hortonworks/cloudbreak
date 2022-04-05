package com.sequenceiq.environment.environment.service.freeipa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaServiceTest {

    private static final String OPERATION = "operation";

    private static final String ENVCRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setAccountId("acc")
            .setResource("env")
            .build().toString();

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private UserV1Endpoint userV1Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private FreeIpaService underTest;

    @BeforeEach
    void setUp() {
    }

//    @Test
//    void getSyncOperationStatusSuccess() {
//        SyncOperationStatus status = createStatus(SynchronizationStatus.COMPLETED, "nope");
//        when(userV1Endpoint.getSyncOperationStatusInternal(any(), eq(OPERATION))).thenReturn(status);
//        SyncOperationStatus result = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
//                underTest.getSyncOperationStatus(ENVCRN, OPERATION));
//        assertThat(result).isEqualTo(status);
//    }

//    @Test
//    void getSyncOperationStatusFailure() {
//        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
//        when(userV1Endpoint.getSyncOperationStatusInternal(any(), eq(OPERATION))).thenThrow(new WebApplicationException("network error"));
//        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAsInternalActor(() ->
//                underTest.getSyncOperationStatus(ENVCRN, OPERATION))).isInstanceOf(FreeIpaOperationFailedException.class);
//    }

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
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.readEntity(Mockito.any(Class.class))).thenReturn(exceptionResponse);
        NotFoundException notFoundException = Mockito.mock(NotFoundException.class);
        when(notFoundException.getResponse()).thenReturn(response);
        when(freeIpaV1Endpoint.describeInternal(eq(ENVCRN), eq("1111"))).thenThrow(notFoundException);
        Optional<DescribeFreeIpaResponse> describeFreeIpaResponse = underTest.internalDescribe(ENVCRN, "1111");
        assertThat(describeFreeIpaResponse.isEmpty()).isEqualTo(true);
    }

    @Test
    void internalDescribeApiNotFoundTest() {
        when(freeIpaV1Endpoint.describeInternal(eq(ENVCRN), eq("1111")))
                .thenThrow(new javax.ws.rs.NotFoundException());
        FreeIpaOperationFailedException freeIpaOperationFailedException = Assertions.assertThrows(FreeIpaOperationFailedException.class,
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
}
