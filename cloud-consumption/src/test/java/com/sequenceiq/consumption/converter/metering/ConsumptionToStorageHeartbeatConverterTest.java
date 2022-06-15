package com.sequenceiq.consumption.converter.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;

public class ConsumptionToStorageHeartbeatConverterTest {

    private static final String ENV_CRN = "env-crn";

    private static final String VALID_LOCATION = "s3a://bucket-name/folder/file";

    private static final String INVALID_LOCATION = "invalid_location";

    private ConsumptionToStorageHeartbeatConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new ConsumptionToStorageHeartbeatConverter();
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(null, null));
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(null, new StorageConsumptionResult(0)));
        assertThrows(NullPointerException.class, () -> underTest.convertToS3StorageHeartBeat(createConsumption(VALID_LOCATION), null));
    }

    @Test
    public void testNullStorageLocation() {
        Consumption consumption = createConsumption(null);

        StorageConsumptionResult storage = new StorageConsumptionResult(0);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToS3StorageHeartBeat(consumption, storage));
        assertEquals("Storage location must start with 's3a' if required file system type is 'S3'!", ex.getMessage());
    }

    @Test
    public void testInvalidStorageLocation() {
        Consumption consumption = createConsumption(INVALID_LOCATION);

        StorageConsumptionResult storage = new StorageConsumptionResult(0);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToS3StorageHeartBeat(consumption, storage));
        assertEquals("Storage location must start with 's3a' if required file system type is 'S3'!", ex.getMessage());
    }

    @Test
    public void testHeartbeatConvertedCorrectly() {
        Consumption consumption = createConsumption(VALID_LOCATION);

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
