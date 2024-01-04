package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsEbsCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@ExtendWith(MockitoExtension.class)
class AwsEbsConsumptionCalculatorTest {

    private static final String VOLUME_NAME = "vol-123345";

    private static final String REGION_NAME = "bucket-location";

    private static final double DOUBLE_ASSERT_EPSILON = 0.001;

    private static final String EBS_OBJECT_PATH = "vol-12312";

    private static final long NO_BYTE_IN_MB = 1024L * 1024L * 1024L;

    private static final String ABFS_OBJECT_PATH = "abfs://FILESYSTEM@STORAGEACCOUNT.dfs.core.windows.net/PATH";

    @Mock
    private AwsEbsCommonService awsEbsCommonService;

    @InjectMocks
    private AwsEbsConsumptionCalculator underTest;

    @Test
    public void getEbsDoesNotExistThrowsException() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withCloudObjectIds(Set.of(VOLUME_NAME))
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        DescribeVolumesResponse statisticsResult = DescribeVolumesResponse.builder()
                .volumes(List.of()).build();
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, VOLUME_NAME))
                .thenReturn(Optional.ofNullable(statisticsResult));

        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () -> underTest.calculate(request));

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, VOLUME_NAME);
        assertEquals(String.format("Unable to describe EBS volume with ID %s",
                VOLUME_NAME), ex.getMessage());
    }

    @Test
    public void getObjectStorageSizeVolume() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withCloudObjectIds(Set.of(VOLUME_NAME))
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Volume description = Volume.builder()
                .size(500).build();
        DescribeVolumesResponse statisticsResult = DescribeVolumesResponse.builder()
                .volumes(List.of(description)).build();
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, VOLUME_NAME))
                .thenReturn(Optional.ofNullable(statisticsResult));

        Set<StorageSizeResponse> result = underTest.calculate(request);

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, VOLUME_NAME);
        assertEquals(500 * NO_BYTE_IN_MB, result.stream().findFirst().get().getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void getObjectStorageSizeLatestDatapointIsUsed() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withCloudObjectIds(Set.of(VOLUME_NAME))
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Volume latestDatapoint = Volume.builder()
                .size(42).build();
        Volume earlierDatapoint = Volume.builder()
                .size(12).build();
        Volume earliestDatapoint = Volume.builder()
                .size(22).build();
        DescribeVolumesResponse statisticsResult = DescribeVolumesResponse.builder()
                .volumes(List.of(latestDatapoint, earlierDatapoint, earliestDatapoint)).build();
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, VOLUME_NAME))
                .thenReturn(Optional.ofNullable(statisticsResult));

        Set<StorageSizeResponse> result = underTest.calculate(request);

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, VOLUME_NAME);
        assertEquals(42 * NO_BYTE_IN_MB, result.stream().findFirst().get().getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void testEbsName() {
        assertEquals("vol-12312", underTest.getObjectId(EBS_OBJECT_PATH));
    }

    @ParameterizedTest(name = "With storageLocation={0}, validation should succeed: {1}")
    @MethodSource("scenariosForValidateCloudStorageType")
    public void testValidateCloudStorageType(String storageLocation, boolean valid) {
        CloudConsumption cloudConsumption = CloudConsumption.builder().withStorageLocation(storageLocation).build();
        if (valid) {
            underTest.validate(cloudConsumption);
        } else {
            assertThrows(ValidationException.class, () -> underTest.validate(cloudConsumption));
        }
    }

    static Object[][] scenariosForValidateCloudStorageType() {
        return new Object[][]{
                {EBS_OBJECT_PATH,     true},
                {ABFS_OBJECT_PATH,   false},
                {"",                 false},
                {null,               false},
        };
    }
}
