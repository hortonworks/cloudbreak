package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsEbsCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;

@ExtendWith(MockitoExtension.class)
class AwsEBSConsumptionCalculatorTest {

    private static final String BUCKET_NAME = "vol-123345";

    private static final String REGION_NAME = "bucket-location";

    private static final String ERROR_MESSAGE = "errormessage";

    private static final double DOUBLE_ASSERT_EPSILON = 0.001;

    private static final String EBS_OBJECT_PATH = "vol-12312";

    private static final long NO_BYTE_IN_MB = 1000L * 1000L;

    private static final String ABFS_OBJECT_PATH = "abfs://FILESYSTEM@STORAGEACCOUNT.dfs.core.windows.net/PATH";

    @Mock
    private AwsEbsCommonService awsEbsCommonService;

    @InjectMocks
    private AwsEBSConsumptionCalculator underTest;

    @Test
    public void getEbsDoesNotExistThrowsException() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        DescribeVolumesResult statisticsResult = new DescribeVolumesResult()
                .withVolumes(List.of());
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () -> underTest.calculate(request));

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(String.format("No EBS were returned by ebs id %s and timeframe from %s to %s",
                BUCKET_NAME, startTime, endTime), ex.getMessage());
    }

    @Test
    public void getObjectStorageSizeOneDatapoint() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Volume description = new Volume()
                .withSize(500);
        DescribeVolumesResult statisticsResult = new DescribeVolumesResult()
                .withVolumes(List.of(description));
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        StorageSizeResponse result = underTest.calculate(request);

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(500 * NO_BYTE_IN_MB, result.getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void getObjectStorageSizeLatestDatapointIsUsed() {
        Date startTime = Date.from(Instant.now().minus(42, ChronoUnit.MINUTES));
        Date endTime = Date.from(Instant.now());
        Region region = Region.region(REGION_NAME);
        StorageSizeRequest request = StorageSizeRequest.builder()
                .withObjectStoragePath(BUCKET_NAME)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withRegion(region)
                .build();

        Volume latestDatapoint = new Volume()
                .withSize(42);
        Volume earlierDatapoint = new Volume()
                .withSize(12);
        Volume earliestDatapoint = new Volume()
                .withSize(22);
        DescribeVolumesResult statisticsResult = new DescribeVolumesResult()
                .withVolumes(List.of(latestDatapoint, earlierDatapoint, earliestDatapoint));
        when(awsEbsCommonService.getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME)).thenReturn(statisticsResult);

        StorageSizeResponse result = underTest.calculate(request);

        verify(awsEbsCommonService).getEbsSize(null, REGION_NAME, startTime, endTime, BUCKET_NAME);
        assertEquals(42 * NO_BYTE_IN_MB, result.getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
    }

    @Test
    public void testEbsName() {
        assertEquals("vol-12312", underTest.getObjectId(EBS_OBJECT_PATH));
    }

    @ParameterizedTest(name = "With storageLocation={1}, validation should succeed: {2}")
    @MethodSource("scenarios")
    public void testValidateCloudStorageType(String storageLocation, boolean valid) {
        CloudConsumption cloudConsumption = CloudConsumption.builder().withStorageLocation(storageLocation).build();
        if (valid) {
            underTest.validate(cloudConsumption);
        } else {
            assertThrows(ValidationException.class, () -> underTest.validate(cloudConsumption));
        }
    }

    static Object[][] scenarios() {
        return new Object[][]{
                {EBS_OBJECT_PATH,     true},
                {ABFS_OBJECT_PATH,   false},
                {"",                 false},
                {null,               false},
        };
    }
}