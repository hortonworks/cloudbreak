package com.sequenceiq.environment.environment.service.consumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.consumption.api.v1.consumption.endpoint.ConsumptionInternalEndpoint;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceCrnEndpoints;
import com.sequenceiq.environment.exception.ConsumptionOperationFailedException;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String INITIATOR_USER_CRN = "initiatorUserCrn";

    private static final String MONITORED_RESOURCE_CRN = "monitoredResourceCrn";

    private static final String STORAGE_LOCATION = "s3a://foo/bar";

    private static final String ERROR_MESSAGE = "Bad luck";

    @Mock
    private ConsumptionInternalCrnClient consumptionInternalCrnClient;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ConsumptionService underTest;

    @Mock
    private ConsumptionServiceCrnEndpoints consumptionServiceCrnEndpoints;

    @Mock
    private ConsumptionInternalEndpoint consumptionInternalEndpoint;

    @BeforeEach
    void setUp() {
        when(consumptionInternalCrnClient.withInternalCrn()).thenReturn(consumptionServiceCrnEndpoints);
        when(consumptionServiceCrnEndpoints.consumptionEndpoint()).thenReturn(consumptionInternalEndpoint);
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenSuccess() {
        StorageConsumptionRequest storageConsumptionRequest = new StorageConsumptionRequest();

        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.scheduleStorageConsumptionCollection(ACCOUNT_ID, storageConsumptionRequest));

        verify(consumptionInternalEndpoint).scheduleStorageConsumptionCollection(ACCOUNT_ID, storageConsumptionRequest, INITIATOR_USER_CRN);
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenFailure() {
        StorageConsumptionRequest storageConsumptionRequest = new StorageConsumptionRequest();
        WebApplicationException e = new WebApplicationException();
        doThrow(e).when(consumptionInternalEndpoint).scheduleStorageConsumptionCollection(ACCOUNT_ID, storageConsumptionRequest, INITIATOR_USER_CRN);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn(ERROR_MESSAGE);

        ConsumptionOperationFailedException consumptionOperationFailedException = assertThrows(ConsumptionOperationFailedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.scheduleStorageConsumptionCollection(
                        ACCOUNT_ID, storageConsumptionRequest)));

        verifyException(e, consumptionOperationFailedException);
    }

    private void verifyException(WebApplicationException e, ConsumptionOperationFailedException consumptionOperationFailedException) {
        assertThat(consumptionOperationFailedException).isNotNull();
        assertThat(consumptionOperationFailedException).hasMessage(ERROR_MESSAGE);
        assertThat(consumptionOperationFailedException).hasCauseReference(e);
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenSuccess() {
        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.unscheduleStorageConsumptionCollection(
                ACCOUNT_ID, MONITORED_RESOURCE_CRN, STORAGE_LOCATION));

        verify(consumptionInternalEndpoint).unscheduleStorageConsumptionCollection(ACCOUNT_ID, MONITORED_RESOURCE_CRN, STORAGE_LOCATION, INITIATOR_USER_CRN);
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenFailure() {
        WebApplicationException e = new WebApplicationException();
        doThrow(e).when(consumptionInternalEndpoint).unscheduleStorageConsumptionCollection(
                ACCOUNT_ID, MONITORED_RESOURCE_CRN, STORAGE_LOCATION, INITIATOR_USER_CRN);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn(ERROR_MESSAGE);

        ConsumptionOperationFailedException consumptionOperationFailedException = assertThrows(ConsumptionOperationFailedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.unscheduleStorageConsumptionCollection(
                        ACCOUNT_ID, MONITORED_RESOURCE_CRN, STORAGE_LOCATION)));

        verifyException(e, consumptionOperationFailedException);
    }

}