package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudwatch.model.AmazonCloudWatchException;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@ExtendWith(MockitoExtension.class)
class AwsCloudWatchServiceTest {

    private static final String REGION = "region";

    @Mock
    private AwsCloudFormationClient awsClient;

    @InjectMocks
    private AwsCloudWatchService underTest;

    @BeforeEach
    public void setupConfiguration() {
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
        MetricAlarm alarm1 = mock(MetricAlarm.class);
        MetricAlarm alarm2 = mock(MetricAlarm.class);
        DescribeAlarmsResult describeAlarmsResult = mock(DescribeAlarmsResult.class);
        AmazonCloudWatchClient cloudWatchClient = mock(AmazonCloudWatchClient.class);
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
        when(awsClient.createCloudWatchClient(eq(credentialView), eq(REGION))).thenReturn(cloudWatchClient);
        when(cloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResult);
        // alarm 3 was already deleted
        when(describeAlarmsResult.getMetricAlarms()).thenReturn(List.of(alarm1, alarm2));
        when(alarm1.getAlarmName()).thenReturn(alarm1Name);
        when(alarm2.getAlarmName()).thenReturn(alarm2Name);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient, times(1)).describeAlarms(captorDescribe.capture());
        verify(cloudWatchClient, times(1)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarm1Name, alarm2Name, alarm3Name), captorDescribe.getValue().getAlarmNames());

        // only delete alarms that were not already deleted
        assertEquals(List.of(alarm1Name, alarm2Name), captorDelete.getValue().getAlarmNames());
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
            MetricAlarm alarm = mock(MetricAlarm.class);
            when(alarm.getAlarmName()).thenReturn(alarmName);
            alarms1.add(alarm);
        }
        for (int i = 101; i <= 200; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames2.add(alarmName);
            MetricAlarm alarm = mock(MetricAlarm.class);
            when(alarm.getAlarmName()).thenReturn(alarmName);
            alarms2.add(alarm);
        }
        for (int i = 201; i <= 210; i++) {
            String alarmName = "i-" + i + "-Status-Check-Failed-System";
            alarmNames3.add(alarmName);
            MetricAlarm alarm = mock(MetricAlarm.class);
            when(alarm.getAlarmName()).thenReturn(alarmName);
            alarms3.add(alarm);
        }
        for (int i = 1; i <= 210; i++) {
            String instanceId = "i-" + i;
            CloudInstance cloudInstance = mock(CloudInstance.class);
            when(cloudInstance.getInstanceId()).thenReturn(instanceId);
            cloudInstances.add(cloudInstance);
        }
        DescribeAlarmsResult describeAlarmsResult1 = mock(DescribeAlarmsResult.class);
        DescribeAlarmsResult describeAlarmsResult2 = mock(DescribeAlarmsResult.class);
        DescribeAlarmsResult describeAlarmsResult3 = mock(DescribeAlarmsResult.class);
        when(describeAlarmsResult1.getMetricAlarms()).thenReturn(alarms1);
        when(describeAlarmsResult2.getMetricAlarms()).thenReturn(alarms2);
        when(describeAlarmsResult3.getMetricAlarms()).thenReturn(alarms3);
        AmazonCloudWatchClient cloudWatchClient = mock(AmazonCloudWatchClient.class);
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        List<Group> groups = List.of(group);
        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(cloudInstances);
        when(awsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(cloudWatchClient);
        when(cloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResult1, describeAlarmsResult2, describeAlarmsResult3);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient, times(3)).describeAlarms(captorDescribe.capture());
        verify(cloudWatchClient, times(3)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDescribe.getAllValues().stream().map(DescribeAlarmsRequest::getAlarmNames).collect(Collectors.toList()));
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDelete.getAllValues().stream().map(DeleteAlarmsRequest::getAlarmNames).collect(Collectors.toList()));
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
        AmazonCloudWatchClient cloudWatchClient = mock(AmazonCloudWatchClient.class);
        AwsCredentialView credentialView = mock(AwsCredentialView.class);
        CloudStack stack = mock(CloudStack.class);
        Group group = mock(Group.class);
        List<Group> groups = List.of(group);
        when(stack.getGroups()).thenReturn(groups);
        when(group.getInstances()).thenReturn(cloudInstances);
        when(awsClient.createCloudWatchClient(credentialView, REGION)).thenReturn(cloudWatchClient);
        AmazonCloudWatchException exception = new AmazonCloudWatchException("No permissions to describe cloudwatch alarms");
        when(cloudWatchClient.describeAlarms(any())).thenThrow(exception);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient, times(3)).describeAlarms(captorDescribe.capture());
        verify(cloudWatchClient, times(210)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarmNames1, alarmNames2, alarmNames3),
                captorDescribe.getAllValues().stream().map(DescribeAlarmsRequest::getAlarmNames).collect(Collectors.toList()));
        assertEquals(deleteAlarmNames,
                captorDelete.getAllValues().stream().map(DeleteAlarmsRequest::getAlarmNames).collect(Collectors.toList()));
    }

    @Test
    void testDeleteCloudWatchAlarmsWhenInstanceDeletedOnProvider() {
        String alarm1Name = "i-1-Status-Check-Failed-System";
        String alarm2Name = "i-2-Status-Check-Failed-System";
        String deletedAlarmName = "i-deleted-Status-Check-Failed-System";
        String instanceId1 = "i-1";
        String instanceId2 = "i-2";
        String deletedInstanceId = "i-deleted";
        MetricAlarm alarm1 = mock(MetricAlarm.class);
        MetricAlarm alarm2 = mock(MetricAlarm.class);
        MetricAlarm deletedInstanceAlarm = mock(MetricAlarm.class);
        DescribeAlarmsResult describeAlarmsResult = mock(DescribeAlarmsResult.class);
        AmazonCloudWatchClient cloudWatchClient = mock(AmazonCloudWatchClient.class);
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
        when(awsClient.createCloudWatchClient(eq(credentialView), eq(REGION))).thenReturn(cloudWatchClient);
        when(cloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResult);
        when(describeAlarmsResult.getMetricAlarms()).thenReturn(List.of(alarm1, alarm2, deletedInstanceAlarm));
        when(alarm1.getAlarmName()).thenReturn(alarm1Name);
        when(alarm2.getAlarmName()).thenReturn(alarm2Name);
        when(deletedInstanceAlarm.getAlarmName()).thenReturn(deletedAlarmName);

        underTest.deleteAllCloudWatchAlarmsForSystemFailures(stack, REGION, credentialView);

        ArgumentCaptor<DescribeAlarmsRequest> captorDescribe = ArgumentCaptor.forClass(DescribeAlarmsRequest.class);
        ArgumentCaptor<DeleteAlarmsRequest> captorDelete = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient, times(1)).describeAlarms(captorDescribe.capture());
        verify(cloudWatchClient, times(1)).deleteAlarms(captorDelete.capture());
        assertEquals(List.of(alarm1Name, alarm2Name, deletedAlarmName), captorDescribe.getValue().getAlarmNames());
        assertEquals(List.of(alarm1Name, alarm2Name, deletedAlarmName), captorDelete.getValue().getAlarmNames());
    }
}