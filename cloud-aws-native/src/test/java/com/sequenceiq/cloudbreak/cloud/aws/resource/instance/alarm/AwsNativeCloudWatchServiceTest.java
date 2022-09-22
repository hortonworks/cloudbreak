package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;

class AwsNativeCloudWatchServiceTest {

    private static final String REGION = "region";

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonCloudWatchClient mockAmazonCloudWatchClient;

    private AwsNativeCloudWatchService underTest;

    @BeforeEach
    public void setupConfiguration() {
        MockitoAnnotations.openMocks(this);
        underTest = new AwsNativeCloudWatchService(commonAwsClient);
        ReflectionTestUtils.setField(underTest, "alarmSuffix", "-Status-Check-Failed-System");
        ReflectionTestUtils.setField(underTest, "maxBatchsize", 100);
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailuresWhenOneAlarmHasAlreadyBeenDeleted() {
        String alarm1Name = "i-1-Status-Check-Failed-System";
        String alarm2Name = "i-2-Status-Check-Failed-System";
        String alarm3Name = "i-3-Status-Check-Failed-System";
        String instanceId1 = "i-1";
        String instanceId2 = "i-2";
        String instanceId3 = "i-3";
        MetricAlarm alarm1 = MetricAlarm.builder().alarmName(alarm1Name).build();
        MetricAlarm alarm2 = MetricAlarm.builder().alarmName(alarm2Name).build();
        DescribeAlarmsResponse describeAlarmsResponse = DescribeAlarmsResponse.builder().metricAlarms(alarm1, alarm2).build();
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        CloudInstance instance1 = mock(CloudInstance.class);
        CloudInstance instance2 = mock(CloudInstance.class);
        CloudInstance instance3 = mock(CloudInstance.class);
        List<Group> groups = List.of(group);
        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(List.of(instance1, instance2, instance3));
        when(instance1.getInstanceId()).thenReturn(instanceId1);
        when(instance2.getInstanceId()).thenReturn(instanceId2);
        when(instance3.getInstanceId()).thenReturn(instanceId3);
        when(commonAwsClient.createCloudWatchClient(eq(credentialView), eq(REGION))).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResponse);
        // alarm 3 was already deleted

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(mockAmazonCloudWatchClient, times(1)).describeAlarms(captorDescribe.capture());
        verify(mockAmazonCloudWatchClient, times(1)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarm1Name, alarm2Name, alarm3Name), captorDescribe.getValue().alarmNames());

        // only delete alarms that were not already deleted
        assertEquals(List.of(alarm1Name, alarm2Name), captorDelete.getValue().alarmNames());
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailuresBatchesDeletion() {
        List<CloudInstance> cloudInstances = new LinkedList<>();
        List<String> alarmNames1 = new LinkedList<>();
        List<String> alarmNames2 = new LinkedList<>();
        List<String> alarmNames3 = new LinkedList<>();
        List<MetricAlarm> alarms1 = new LinkedList<>();
        List<MetricAlarm> alarms2 = new LinkedList<>();
        List<MetricAlarm> alarms3 = new LinkedList<>();
        for (int i = 1; i <= 100; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames1.add(alarmName);
            MetricAlarm alarm = MetricAlarm.builder().alarmName(alarmName).build();
            alarms1.add(alarm);
        }
        for (int i = 101; i <= 200; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames2.add(alarmName);
            MetricAlarm alarm = MetricAlarm.builder().alarmName(alarmName).build();
            alarms2.add(alarm);
        }
        for (int i = 201; i <= 210; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames3.add(alarmName);
            MetricAlarm alarm = MetricAlarm.builder().alarmName(alarmName).build();
            alarms3.add(alarm);
        }
        for (int i = 1; i <= 210; i++) {
            String instanceId = "i-" + i;
            CloudInstance cloudInstance = mock(CloudInstance.class);
            when(cloudInstance.getInstanceId()).thenReturn(instanceId);
            cloudInstances.add(cloudInstance);
        }
        DescribeAlarmsResponse describeAlarmsResponse1 = DescribeAlarmsResponse.builder().metricAlarms(alarms1).build();
        DescribeAlarmsResponse describeAlarmsResponse2 = DescribeAlarmsResponse.builder().metricAlarms(alarms2).build();
        DescribeAlarmsResponse describeAlarmsResponse3 = DescribeAlarmsResponse.builder().metricAlarms(alarms3).build();
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        List<Group> groups = List.of(group);
        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(cloudInstances);
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResponse1, describeAlarmsResponse2, describeAlarmsResponse3);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(mockAmazonCloudWatchClient, times(3)).describeAlarms(captorDescribe.capture());
        verify(mockAmazonCloudWatchClient, times(3)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDescribe.getAllValues().stream().map(DescribeAlarmsRequest::alarmNames).collect(Collectors.toList()));
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDelete.getAllValues().stream().map(DeleteAlarmsRequest::alarmNames).collect(Collectors.toList()));
    }

    @Test
    void testDeleteCloudWatchAlarmsForSystemFailuresDoesNotBatchDeletionWhenDescribeAlarmPermissionIsMissing() {
        List<CloudInstance> cloudInstances = new LinkedList<>();
        List<List<String>> deleteAlarmNames = new LinkedList<>();
        List<String> alarmNames1 = new LinkedList<>();
        List<String> alarmNames2 = new LinkedList<>();
        List<String> alarmNames3 = new LinkedList<>();
        for (int i = 1; i <= 100; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames1.add(alarmName);
            deleteAlarmNames.add(List.of(alarmName));
        }
        for (int i = 101; i <= 200; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames2.add(alarmName);
            deleteAlarmNames.add(List.of(alarmName));
        }
        for (int i = 201; i <= 210; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames3.add(alarmName);
            deleteAlarmNames.add(List.of(alarmName));
        }
        for (int i = 1; i <= 210; i++) {
            String instanceId = "i-" + i;
            CloudInstance cloudInstance = mock(CloudInstance.class);
            when(cloudInstance.getInstanceId()).thenReturn(instanceId);
            cloudInstances.add(cloudInstance);
        }
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        List<Group> groups = List.of(group);
        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(cloudInstances);
        when(commonAwsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(mockAmazonCloudWatchClient);
        CloudWatchException exception = (CloudWatchException) CloudWatchException.builder().message("No permissions to describe cloudwatch alarms").build();
        when(mockAmazonCloudWatchClient.describeAlarms(any())).thenThrow(exception);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(mockAmazonCloudWatchClient, times(3)).describeAlarms(captorDescribe.capture());
        verify(mockAmazonCloudWatchClient, times(210)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDescribe.getAllValues().stream().map(DescribeAlarmsRequest::alarmNames).collect(Collectors.toList()));
        assertEquals(deleteAlarmNames,
                captorDelete.getAllValues().stream().map(DeleteAlarmsRequest::alarmNames).collect(Collectors.toList()));
    }

    @Test
    void testDeleteCloudWatchAlarmsWhenInstanceDeletedOnProvider() {
        String alarm1Name = "i-1-Status-Check-Failed-System";
        String alarm2Name = "i-2-Status-Check-Failed-System";
        String deletedAlarmName = "i-deleted-Status-Check-Failed-System";
        String instanceId1 = "i-1";
        String instanceId2 = "i-2";
        String deletedInstanceId = "i-deleted";
        MetricAlarm alarm1 = MetricAlarm.builder().alarmName(alarm1Name).build();
        MetricAlarm alarm2 = MetricAlarm.builder().alarmName(alarm2Name).build();
        MetricAlarm deletedInstanceAlarm = MetricAlarm.builder().alarmName(deletedAlarmName).build();
        DescribeAlarmsResponse describeAlarmsResponse = DescribeAlarmsResponse.builder().metricAlarms(alarm1, alarm2, deletedInstanceAlarm).build();
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        Group deletedGroup = mock(Group.class);
        CloudInstance instance1 = mock(CloudInstance.class);
        CloudInstance instance2 = mock(CloudInstance.class);
        CloudInstance deletedInstance = mock(CloudInstance.class);
        List<Group> groups = List.of(group, deletedGroup);

        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(List.of(instance1, instance2));
        when(deletedGroup.getInstances()).thenReturn(List.of(deletedInstance));
        when(instance1.getInstanceId()).thenReturn(instanceId1);
        when(instance2.getInstanceId()).thenReturn(instanceId2);
        when(deletedInstance.getInstanceId()).thenReturn(deletedInstanceId);
        when(commonAwsClient.createCloudWatchClient(eq(credentialView), eq(REGION))).thenReturn(mockAmazonCloudWatchClient);
        when(mockAmazonCloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResponse);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(mockAmazonCloudWatchClient, times(1)).describeAlarms(captorDescribe.capture());
        verify(mockAmazonCloudWatchClient, times(1)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarm1Name, alarm2Name, deletedAlarmName), captorDescribe.getValue().alarmNames());
        assertEquals(List.of(alarm1Name, alarm2Name, deletedAlarmName), captorDelete.getValue().alarmNames());
    }

}
