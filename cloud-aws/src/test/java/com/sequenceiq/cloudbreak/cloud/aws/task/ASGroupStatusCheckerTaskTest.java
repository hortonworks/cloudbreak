package com.sequenceiq.cloudbreak.cloud.aws.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

/**
 * Created by perdos on 8/25/16.
 */
public class ASGroupStatusCheckerTaskTest {

    @Test
    public void successTest() throws Exception {
        int requiredInstances = 160;
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        CloudContext cloudContext = mock(CloudContext.class);
        String regionName = "eu-west-1";
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(regionName)));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);

        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);

        String asGroupName = "as-group";
        AwsClient awsClient = mock(AwsClient.class);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq(regionName))).thenReturn(amazonEC2Client);

        DescribeInstanceStatusResult firstDescribeInstanceStatusResult = new DescribeInstanceStatusResult();
        List<InstanceStatus> firstInstanceStatuses = returnInstanceStatus(0, 100);
        firstDescribeInstanceStatusResult.setInstanceStatuses(firstInstanceStatuses);

        DescribeInstanceStatusResult secondDescribeInstanceStatusResult = new DescribeInstanceStatusResult();
        List<InstanceStatus> secondInstanceStatuses = returnInstanceStatus(100, 160);
        secondDescribeInstanceStatusResult.setInstanceStatuses(secondInstanceStatuses);

        when(amazonEC2Client.describeInstanceStatus(any(DescribeInstanceStatusRequest.class)))
                .thenReturn(firstDescribeInstanceStatusResult)
                .thenReturn(secondDescribeInstanceStatusResult);

        CloudFormationStackUtil cloudFormationStackUtil = mock(CloudFormationStackUtil.class);

        List<String> instancIds = new ArrayList<>();
        for (int i = 0; i < requiredInstances; i++) {
            instancIds.add(Integer.toString(i));
        }

        AmazonAutoScalingClient autoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(autoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(new DescribeScalingActivitiesResult());

        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq(asGroupName))).thenReturn(instancIds);

        ASGroupStatusCheckerTask asGroupStatusCheckerTask = new ASGroupStatusCheckerTask(authenticatedContext, asGroupName, requiredInstances, awsClient,
                cloudFormationStackUtil);
        Boolean taskResult = asGroupStatusCheckerTask.call();

        ArgumentCaptor<DescribeInstanceStatusRequest> instanceStatusRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeInstanceStatusRequest.class);
        verify(amazonEC2Client, times(2)).describeInstanceStatus(instanceStatusRequestArgumentCaptor.capture());

        List<DescribeInstanceStatusRequest> allValues = instanceStatusRequestArgumentCaptor.getAllValues();
        assertEquals(100, allValues.get(0).getInstanceIds().size());
        assertEquals(60, allValues.get(1).getInstanceIds().size());
        assertTrue(taskResult);
    }

    @Test
    public void failTest() throws Exception {
        int requiredInstances = 160;
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        CloudContext cloudContext = mock(CloudContext.class);
        String regionName = "eu-west-1";
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(regionName)));

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);

        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);

        String asGroupName = "as-group";
        AwsClient awsClient = mock(AwsClient.class);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq(regionName))).thenReturn(amazonEC2Client);

        DescribeInstanceStatusResult firstDescribeInstanceStatusResult = new DescribeInstanceStatusResult();
        List<InstanceStatus> firstInstanceStatuses = returnInstanceStatus(0, 99);
        firstDescribeInstanceStatusResult.setInstanceStatuses(firstInstanceStatuses);

        DescribeInstanceStatusResult secondDescribeInstanceStatusResult = new DescribeInstanceStatusResult();
        List<InstanceStatus> secondInstanceStatuses = returnInstanceStatus(100, 160);
        secondDescribeInstanceStatusResult.setInstanceStatuses(secondInstanceStatuses);

        when(amazonEC2Client.describeInstanceStatus(any(DescribeInstanceStatusRequest.class)))
                .thenReturn(firstDescribeInstanceStatusResult)
                .thenReturn(secondDescribeInstanceStatusResult);

        CloudFormationStackUtil cloudFormationStackUtil = mock(CloudFormationStackUtil.class);

        List<String> instancIds = new ArrayList<>();
        for (int i = 0; i < requiredInstances; i++) {
            instancIds.add(Integer.toString(i));
        }

        AmazonAutoScalingClient autoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(autoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(new DescribeScalingActivitiesResult());

        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq(asGroupName))).thenReturn(instancIds);

        ASGroupStatusCheckerTask asGroupStatusCheckerTask = new ASGroupStatusCheckerTask(authenticatedContext, asGroupName, requiredInstances, awsClient,
                cloudFormationStackUtil);
        Boolean taskResult = asGroupStatusCheckerTask.call();

        ArgumentCaptor<DescribeInstanceStatusRequest> instanceStatusRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeInstanceStatusRequest.class);
        verify(amazonEC2Client, times(2)).describeInstanceStatus(instanceStatusRequestArgumentCaptor.capture());

        List<DescribeInstanceStatusRequest> allValues = instanceStatusRequestArgumentCaptor.getAllValues();
        assertEquals(100, allValues.get(0).getInstanceIds().size());
        assertEquals(60, allValues.get(1).getInstanceIds().size());
        assertFalse(taskResult);
    }

    private List<InstanceStatus> returnInstanceStatus(int start, int end) {
        List<InstanceStatus> instanceStatuses = new ArrayList<>();
        for (int i = start; i < end; i++) {
            InstanceStatus instanceStatus = new InstanceStatus();
            InstanceState instanceState = new InstanceState();
            instanceState.setCode(16);
            instanceStatus.setInstanceState(instanceState);
            instanceStatus.setInstanceId(Integer.toString(i));
            instanceStatuses.add(instanceStatus);
        }
        return instanceStatuses;
    }
}