package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
class AwsRdsStorageMetricServiceTest {

    private static final String DB_INSTANCE_ID = "myrds";

    private static final long BYTES_IN_GIB = 1024L * 1024L * 1024L;

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.DatabaseServer databaseServer;

    @InjectMocks
    private AwsRdsStorageMetricService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_ID);
    }

    private void stubRegion() {
        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn("us-west-2");
    }

    private void stubAllocatedStorageGib(Integer allocatedGib) {
        DescribeDbInstancesResponse response = DescribeDbInstancesResponse.builder()
                .dbInstances(DBInstance.builder().allocatedStorage(allocatedGib).build())
                .build();
        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(ac, dbStack)).thenReturn(response);
    }

    private void stubFreeStorageAverageBytes(double averageBytes) {
        when(awsCloudWatchService.getLatestAverageMetricValue(any(), eq("us-west-2"), eq("AWS/RDS"), eq("FreeStorageSpace"), any(), anyInt(), anyInt()))
                .thenReturn(Optional.of(averageBytes));
    }

    @Test
    void shouldComputeFreePercentageFromCloudWatchAndAllocatedStorage() {
        stubAllocatedStorageGib(100);
        stubRegion();
        stubFreeStorageAverageBytes(10d * BYTES_IN_GIB);

        Optional<DatabaseServerStorageMetrics> result = underTest.getStorageMetrics(ac, dbStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(10.0d, within(0.001d));
        assertThat(result.get().freeBytes()).isEqualTo(10L * BYTES_IN_GIB);
        assertThat(result.get().allocatedBytes()).isEqualTo(100L * BYTES_IN_GIB);
    }

    @Test
    void shouldReturnEmptyWhenAllocatedStorageMissing() {
        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(ac, dbStack))
                .thenReturn(DescribeDbInstancesResponse.builder().build());

        assertThat(underTest.getStorageMetrics(ac, dbStack)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDescribeReturnsNull() {
        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(ac, dbStack)).thenReturn(null);

        assertThat(underTest.getStorageMetrics(ac, dbStack)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoDatapoints() {
        stubAllocatedStorageGib(100);
        stubRegion();
        when(awsCloudWatchService.getLatestAverageMetricValue(any(), eq("us-west-2"), eq("AWS/RDS"), eq("FreeStorageSpace"), any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        assertThat(underTest.getStorageMetrics(ac, dbStack)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenCloudWatchThrows() {
        stubAllocatedStorageGib(100);
        stubRegion();
        when(awsCloudWatchService.getLatestAverageMetricValue(any(), eq("us-west-2"), eq("AWS/RDS"), eq("FreeStorageSpace"), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        assertThat(underTest.getStorageMetrics(ac, dbStack)).isEmpty();
    }
}
