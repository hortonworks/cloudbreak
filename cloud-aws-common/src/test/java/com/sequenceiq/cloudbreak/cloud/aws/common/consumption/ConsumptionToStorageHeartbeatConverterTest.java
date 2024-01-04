package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;

public class ConsumptionToStorageHeartbeatConverterTest {

    private static final String ENV_CRN = "env-crn";

    private static final String VALID_LOCATION = "s3a://bucket-name/folder/file";

    private static final String INVALID_LOCATION = "invalid_location";

    private AwsS3ConsumptionCalculator underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsS3ConsumptionCalculator();
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class, () -> underTest.convertToStorageHeartbeat(null, 0));
    }

    @Test
    public void testNullStorageLocation() {
        CloudConsumption cloudConsumption = createConsumption(null);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToStorageHeartbeat(cloudConsumption, 0));
        assertEquals("Storage location must start with 's3a' if required file system type is 'S3'!", ex.getMessage());
    }

    @Test
    public void testInvalidStorageLocation() {
        CloudConsumption consumption = createConsumption(INVALID_LOCATION);

        ValidationException ex = assertThrows(ValidationException.class, () -> underTest.convertToStorageHeartbeat(consumption, 0));
        assertEquals("Storage location must start with 's3a' if required file system type is 'S3'!", ex.getMessage());
    }

    @Test
    public void testHeartbeatConvertedCorrectly() {
        CloudConsumption consumption = createConsumption(VALID_LOCATION);

        MeteringEventsProto.StorageHeartbeat result = underTest.convertToStorageHeartbeat(consumption, 42000000);

        assertEquals(ENV_CRN, result.getMeteredResourceMetadata().getEnvironmentCrn());
        assertEquals(1, result.getStoragesList().size());
        MeteringEventsProto.Storage storageResult = result.getStoragesList().get(0);
        assertEquals("bucket-name", storageResult.getId());
        assertEquals(42, storageResult.getSizeInMB());
        assertEquals(MeteringEventsProto.StorageType.Value.S3, storageResult.getType());
        assertEquals("", storageResult.getDescription());

    }

    private CloudConsumption createConsumption(String storageLocation) {
        CloudConsumption consumption = CloudConsumption.builder()
                .withEnvironmentCrn(ENV_CRN)
                .withStorageLocation(storageLocation)
                .build();
        return consumption;
    }

}
