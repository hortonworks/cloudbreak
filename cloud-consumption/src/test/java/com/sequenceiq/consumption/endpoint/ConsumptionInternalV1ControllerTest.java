package com.sequenceiq.consumption.endpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.job.storage.StorageConsumptionJobService;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class ConsumptionInternalV1ControllerTest {

    private static final String MONITORED_CRN = "crn:cdp:datalake:us-west-1:hortonworks:datalake:guid";

    private static final String LOCATION = "s3a://location";

    private static final String ACCOUNT_ID = "123";

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private ConsumptionApiConverter consumptionApiConverter;

    @Mock
    private StorageConsumptionJobService jobService;

    @InjectMocks
    private ConsumptionInternalV1Controller underTest;

    @Test
    public void testStorageConsumptionCollectionExists() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(true);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertTrue(response.isExists());
    }

    @Test
    public void testStorageConsumptionCollectionDoesntExist() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(false);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertFalse(response.isExists());
    }
}
