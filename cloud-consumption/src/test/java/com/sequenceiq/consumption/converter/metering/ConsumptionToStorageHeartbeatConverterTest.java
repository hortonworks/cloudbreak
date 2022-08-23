package com.sequenceiq.consumption.converter.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.util.CloudStorageLocationUtil;

@ExtendWith(MockitoExtension.class)
public class ConsumptionToStorageHeartbeatConverterTest {

    private static final String ENV_CRN = "env-crn";

    private static final String VALID_LOCATION = "s3a://bucket-name/folder/file";

    private static final String INVALID_LOCATION = "invalid_location";

    @Mock
    private CloudStorageLocationUtil cloudStorageLocationUtil;

    @InjectMocks
    private ConsumptionToStorageHeartbeatConverter underTest;

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(null, null));
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(null, new StorageConsumptionResult(0)));
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(createConsumption(VALID_LOCATION), null));
    }

    @Test
    public void testNullStorageLocation() {
        Consumption consumption = createConsumption(null);

        when(cloudStorageLocationUtil.getS3BucketName(consumption.getStorageLocation())).thenThrow(new ValidationException("error"));

        StorageConsumptionResult storage = new StorageConsumptionResult(0);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToS3StorageHeartBeat(consumption, storage));
        assertEquals("error", ex.getMessage());
    }

    @Test
    public void testInvalidStorageLocation() {
        Consumption consumption = createConsumption(INVALID_LOCATION);

        when(cloudStorageLocationUtil.getS3BucketName(consumption.getStorageLocation())).thenThrow(new ValidationException("error"));

        StorageConsumptionResult storage = new StorageConsumptionResult(0);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToS3StorageHeartBeat(consumption, storage));
        assertEquals("error", ex.getMessage());
    }

    @Test
    public void testHeartbeatConvertedCorrectly() {
        Consumption consumption = createConsumption(VALID_LOCATION);

        when(cloudStorageLocationUtil.getS3BucketName(consumption.getStorageLocation())).thenReturn("bucket-name");

        StorageConsumptionResult storage = new StorageConsumptionResult(42000000);

        MeteringEventsProto.StorageHeartbeat result = underTest.convertToS3StorageHeartBeat(consumption, storage);

        assertEquals(ENV_CRN, result.getMeteredResourceMetadata().getEnvironmentCrn());
        assertEquals(1, result.getStoragesList().size());
        MeteringEventsProto.Storage storageResult = result.getStoragesList().get(0);
        assertEquals("bucket-name", storageResult.getId());
        assertEquals(42, storageResult.getSizeInMB());
        assertEquals(MeteringEventsProto.StorageType.Value.S3, storageResult.getType());
        assertEquals("", storageResult.getDescription());

    }

    private Consumption createConsumption(String storageLocation) {
        Consumption consumption = new Consumption();
        consumption.setEnvironmentCrn(ENV_CRN);
        consumption.setStorageLocation(storageLocation);
        return consumption;
    }
}
