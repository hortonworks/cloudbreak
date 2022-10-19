package com.sequenceiq.cloudbreak.service.consumption;

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

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.consumption.api.v1.consumption.endpoint.ConsumptionInternalEndpoint;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceCrnEndpoints;

@ExtendWith(MockitoExtension.class)
class ConsumptionClientServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String INITIATOR_USER_CRN = "initiatorUserCrn";

    private static final String MONITORED_RESOURCE_CRN = "monitoredResourceCrn";

    private static final String CLOUD_RESOURCE_ID = "cloudResourceId";

    private static final String ERROR_MESSAGE = "Bad luck";

    @Mock
    private ConsumptionInternalCrnClient consumptionInternalCrnClient;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ConsumptionClientService underTest;

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
    void scheduleCloudResourceConsumptionCollectionTestWhenSuccess() {
        CloudResourceConsumptionRequest cloudResourceConsumptionRequest = new CloudResourceConsumptionRequest();

        underTest.scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, cloudResourceConsumptionRequest, INITIATOR_USER_CRN);

        verify(consumptionInternalEndpoint).scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, cloudResourceConsumptionRequest, INITIATOR_USER_CRN);
    }

    @Test
    void scheduleCloudResourceConsumptionCollectionTestWhenFailure() {
        CloudResourceConsumptionRequest cloudResourceConsumptionRequest = new CloudResourceConsumptionRequest();
        WebApplicationException e = new WebApplicationException();
        doThrow(e).when(consumptionInternalEndpoint).scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, cloudResourceConsumptionRequest, INITIATOR_USER_CRN);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn(ERROR_MESSAGE);

        ConsumptionOperationFailedException consumptionOperationFailedException = assertThrows(ConsumptionOperationFailedException.class,
                () -> underTest.scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, cloudResourceConsumptionRequest, INITIATOR_USER_CRN));

        verifyException(e, consumptionOperationFailedException);
    }

    private void verifyException(WebApplicationException e, ConsumptionOperationFailedException consumptionOperationFailedException) {
        assertThat(consumptionOperationFailedException).isNotNull();
        assertThat(consumptionOperationFailedException).hasMessage(ERROR_MESSAGE);
        assertThat(consumptionOperationFailedException).hasCauseReference(e);
    }

    @Test
    void unscheduleCloudResourceConsumptionCollectionTestWhenSuccess() {
        underTest.unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, MONITORED_RESOURCE_CRN, CLOUD_RESOURCE_ID, INITIATOR_USER_CRN);

        verify(consumptionInternalEndpoint).unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, MONITORED_RESOURCE_CRN, CLOUD_RESOURCE_ID,
                INITIATOR_USER_CRN);
    }

    @Test
    void unscheduleCloudResourceConsumptionCollectionTestWhenFailure() {
        WebApplicationException e = new WebApplicationException();
        doThrow(e).when(consumptionInternalEndpoint).unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID,
                MONITORED_RESOURCE_CRN, CLOUD_RESOURCE_ID, INITIATOR_USER_CRN);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn(ERROR_MESSAGE);

        ConsumptionOperationFailedException consumptionOperationFailedException = assertThrows(ConsumptionOperationFailedException.class,
                () -> underTest.unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, MONITORED_RESOURCE_CRN, CLOUD_RESOURCE_ID, INITIATOR_USER_CRN));

        verifyException(e, consumptionOperationFailedException);
    }

}