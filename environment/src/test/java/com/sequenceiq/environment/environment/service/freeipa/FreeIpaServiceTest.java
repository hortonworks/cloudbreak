package com.sequenceiq.environment.environment.service.freeipa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaServiceTest {

    private static final String OPERATION = "operation";

    private static final String ENVCRN = "envcrn";

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

    @Test
    void getSyncOperationStatusSuccess() {
        SyncOperationStatus status = createStatus(SynchronizationStatus.COMPLETED, "nope");
        when(userV1Endpoint.getSyncOperationStatus(OPERATION)).thenReturn(status);
        SyncOperationStatus result = underTest.getSyncOperationStatus(ENVCRN, OPERATION);
        assertThat(result).isEqualTo(status);
    }

    @Test
    void getSyncOperationStatusFailure() {
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
        when(userV1Endpoint.getSyncOperationStatus(OPERATION)).thenThrow(new WebApplicationException("network error"));
        assertThatThrownBy(() -> underTest.getSyncOperationStatus(ENVCRN, OPERATION)).isInstanceOf(FreeIpaOperationFailedException.class);
    }

    @Test
    void synchronizeAllUsersInEnvironmentSuccess() {
        SyncOperationStatus status = createStatus(SynchronizationStatus.REQUESTED, "");
        when(userV1Endpoint.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenReturn(status);
        SyncOperationStatus result = underTest.synchronizeAllUsersInEnvironment(ENVCRN);
        assertThat(result).isEqualTo(status);
    }

    @Test
    void synchronizeAllUsersInEnvironmentFailure() {
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
        when(userV1Endpoint.synchronizeAllUsers(any(SynchronizeAllUsersRequest.class))).thenThrow(new WebApplicationException("network error"));
        assertThatThrownBy(() -> underTest.synchronizeAllUsersInEnvironment(ENVCRN)).isInstanceOf(FreeIpaOperationFailedException.class);
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
