package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudwatch.model.Statistic.MAXIMUM;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.services.cloudwatch.model.AlarmType;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;

@ExtendWith(MockitoExtension.class)
class AwsNativeCloudWatchServiceTest {

    private static final String REGION = "region";

    private static final String INSTANCE_ID = "instanceId";

    private static final Map<String, String> TAGS = Map.of("key1", "value1", "key2", "value2");

    private CloudResource resource;

    private AwsCredentialView credentialView;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AmazonCloudWatchClient mockAmazonCloudWatchClient;

    @InjectMocks
    private AwsNativeCloudWatchService underTest;

    @Captor
    private ArgumentCaptor<PutMetricAlarmRequest> putMetricAlarmRequestCaptor;

    @Captor
    private ArgumentCaptor<DeleteAlarmsRequest> deleteAlarmsRequestCaptor;

    @BeforeEach
    void setUp() {
        resource = mock(CloudResource.class);
        credentialView = mock(AwsCredentialView.class);
        ReflectionTestUtils.setField(underTest, "alarmSuffix", "-Status-Check-Failed-System");
        ReflectionTestUtils.setField(underTest, "cloudwatchPeriod", 60);
        ReflectionTestUtils.setField(underTest, "cloudwatchEvaluationPeriods", 2);
        ReflectionTestUtils.setField(underTest, "cloudwatchThreshold", 1.0);
        ReflectionTestUtils.setField(underTest, "maxBatchsize", 100);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testAddCloudWatchAlarmsForSystemFailures(boolean govCloud) {
        when(resource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(credentialView.isGovernmentCloudEnabled()).thenReturn(govCloud);
        when(awsTaggingService.prepareCloudWatchTags(TAGS)).thenCallRealMethod();
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);

        underTest.addCloudWatchAlarmsForSystemFailures(resource, REGION, credentialView, TAGS);

        verify(mockAmazonCloudWatchClient).putMetricAlarm(putMetricAlarmRequestCaptor.capture());
        PutMetricAlarmRequest putMetricAlarmRequest = putMetricAlarmRequestCaptor.getValue();
        assertEquals(60, putMetricAlarmRequest.period());
        assertEquals("AWS/EC2", putMetricAlarmRequest.namespace());
        assertEquals("StatusCheckFailed_System", putMetricAlarmRequest.metricName());
        assertEquals("GreaterThanOrEqualToThreshold", putMetricAlarmRequest.comparisonOperator().toString());
        Dimension dimension = putMetricAlarmRequest.dimensions().getFirst();
        assertEquals("InstanceId", dimension.name());
        assertEquals(INSTANCE_ID, dimension.value());
        assertEquals(govCloud ? "arn:aws-us-gov:automate:region:ec2:recover" : "arn:aws:automate:region:ec2:recover",
                putMetricAlarmRequest.alarmActions().getFirst());
        assertEquals(2, putMetricAlarmRequest.evaluationPeriods());
        assertEquals(INSTANCE_ID + "-Status-Check-Failed-System", putMetricAlarmRequest.alarmName());
        assertEquals(1.0, putMetricAlarmRequest.threshold());
        assertEquals(MAXIMUM, putMetricAlarmRequest.statistic());
        assertThat(putMetricAlarmRequest.tags()).extracting(tag -> Map.entry(tag.key(), tag.value()))
                .containsExactlyInAnyOrderElementsOf(TAGS.entrySet());
    }

    @Test
    void testAddCloudWatchAlarmsForSystemFailuresDoesNotThrow() {
        when(resource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(credentialView.isGovernmentCloudEnabled()).thenReturn(false);
        when(awsTaggingService.prepareCloudWatchTags(TAGS)).thenCallRealMethod();
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.putMetricAlarm(any(PutMetricAlarmRequest.class))).thenThrow(CloudWatchException.builder().message("error").build());

        assertDoesNotThrow(() -> underTest.addCloudWatchAlarmsForSystemFailures(resource, REGION, credentialView, TAGS));
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailures() {
        List<String> instanceIds = IntStream.range(0, 150).boxed()
                .map(i -> "instanceId" + i)
                .toList();
        List<MetricAlarm> metricAlarms = instanceIds.stream()
                .map(instanceId -> MetricAlarm.builder()
                        .alarmName(instanceId + "-Status-Check-Failed-System")
                        .build())
                .toList();
        List<DescribeAlarmsResponse> describeAlarmsResponses = List.of(
                DescribeAlarmsResponse.builder()
                        .metricAlarms(metricAlarms.subList(0, 100))
                        .build(),
                DescribeAlarmsResponse.builder()
                        .metricAlarms(metricAlarms.subList(100, 150))
                        .build());
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any(DescribeAlarmsRequest.class)))
                .thenReturn(describeAlarmsResponses.get(0))
                .thenReturn(describeAlarmsResponses.get(1));

        underTest.deleteCloudWatchAlarmsForSystemFailures(REGION, credentialView, instanceIds);

        verify(mockAmazonCloudWatchClient, times(2)).deleteAlarms(deleteAlarmsRequestCaptor.capture());
        List<DeleteAlarmsRequest> deleteAlarmsRequests = deleteAlarmsRequestCaptor.getAllValues();
        assertEquals(100, deleteAlarmsRequests.get(0).alarmNames().size());
        assertEquals(50, deleteAlarmsRequests.get(1).alarmNames().size());
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailuresFallbackToDeleteIndividually() {
        List<String> instanceIds = IntStream.range(0, 150).boxed()
                .map(i -> "instanceId" + i)
                .toList();
        List<MetricAlarm> metricAlarms = instanceIds.stream()
                .map(instanceId -> MetricAlarm.builder()
                        .alarmName(instanceId + "-Status-Check-Failed-System")
                        .build())
                .toList();
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any(DescribeAlarmsRequest.class))).thenThrow(CloudWatchException.builder().message("error").build());

        assertDoesNotThrow(() -> underTest.deleteCloudWatchAlarmsForSystemFailures(REGION, credentialView, instanceIds));

        verify(mockAmazonCloudWatchClient, times(150)).deleteAlarms(any(DeleteAlarmsRequest.class));
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailuresThrowsIfDeleteFails() {
        List<MetricAlarm> metricAlarms = List.of(MetricAlarm.builder().alarmName("instanceId0-Status-Check-Failed-System").build());
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any(DescribeAlarmsRequest.class)))
                .thenReturn(DescribeAlarmsResponse.builder().metricAlarms(metricAlarms).build());
        when(mockAmazonCloudWatchClient.deleteAlarms(any(DeleteAlarmsRequest.class))).thenThrow(CloudWatchException.builder().message("error").build());

        assertThrows(CloudConnectorException.class, () -> underTest.deleteCloudWatchAlarmsForSystemFailures(REGION, credentialView, List.of("instanceId0")));
    }

    @Test
    void testGetMetricAlarmsForInstances() {
        List<String> alarmNames = List.of("instanceId0-Status-Check-Failed-System", "instanceId1-Status-Check-Failed-System");
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        DescribeAlarmsRequest describeAlarmsRequest = DescribeAlarmsRequest.builder()
                .alarmTypes(AlarmType.METRIC_ALARM)
                .alarmNames(alarmNames)
                .build();
        DescribeAlarmsResponse describeAlarmsResponse = DescribeAlarmsResponse.builder()
                .metricAlarms(alarmNames.stream()
                        .map(alarmName -> MetricAlarm.builder().alarmName(alarmName).build())
                        .toList())
                .build();
        when(mockAmazonCloudWatchClient.describeAlarms(describeAlarmsRequest)).thenReturn(describeAlarmsResponse);

        List<MetricAlarm> result = underTest.getMetricAlarmsForInstances(REGION, credentialView, List.of("instanceId0", "instanceId1"));

        assertThat(result).extracting(MetricAlarm::alarmName).containsExactlyInAnyOrderElementsOf(alarmNames);
    }
}
