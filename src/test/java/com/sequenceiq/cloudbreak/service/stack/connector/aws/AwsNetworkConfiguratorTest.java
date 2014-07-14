package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;


public class AwsNetworkConfiguratorTest {

    @InjectMocks
    private AwsNetworkConfigurator underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    private Stack stack;

    private DescribeStackResourceResult dsrResult;

    private DescribeAutoScalingGroupsResult dasgResult;

    private DescribeInstancesResult diResult;

    @Before
    public void setUp() {
        underTest = new AwsNetworkConfigurator();
        MockitoAnnotations.initMocks(this);
        User user = AwsConnectorTestUtil.createUser();
        AwsCredential credential = AwsConnectorTestUtil.createAwsCredential();
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate(user);
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_TEMPLATE_NAME, "", stack));
        stack = AwsConnectorTestUtil.createStack(user, credential, template, resources);
        dsrResult = new DescribeStackResourceResult();
        dsrResult.setStackResourceDetail(new StackResourceDetail());
        dasgResult = createAutoScaldingGroups();
        diResult = createDescribeInstanceResult();
        mockClients();

    }

    @Test
    public void testDescribeSourceDestCheck() {
        // GIVEN
        given(amazonCloudFormationClient.describeStackResource(any(DescribeStackResourceRequest.class)))
                .willReturn(dsrResult);
        given(amazonAutoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .willReturn(dasgResult);
        given(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .willReturn(diResult);
        doNothing().when(amazonEC2Client).modifyNetworkInterfaceAttribute(any(ModifyNetworkInterfaceAttributeRequest.class));
        // WHEN
        underTest.disableSourceDestCheck(stack);
        // THEN
        verify(amazonEC2Client, times(1)).modifyNetworkInterfaceAttribute(any(ModifyNetworkInterfaceAttributeRequest.class));
    }

    private void mockClients() {
        given(awsStackUtil.createAutoScalingClient(any(Regions.class), any(AwsCredential.class)))
                .willReturn(amazonAutoScalingClient);
        given(awsStackUtil.createCloudFormationClient(any(Regions.class), any(AwsCredential.class)))
                .willReturn(amazonCloudFormationClient);
        given(awsStackUtil.createEC2Client(any(Regions.class), any(AwsCredential.class)))
                .willReturn(amazonEC2Client);
    }

    private DescribeAutoScalingGroupsResult createAutoScaldingGroups() {
        DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult();
        List<AutoScalingGroup> autoScalingGroups = new ArrayList<>();
        AutoScalingGroup asg = new AutoScalingGroup();
        Instance instance1 = new Instance();
        Instance instance2 = new Instance();
        instance1.setInstanceId("instanceId1");
        instance2.setInstanceId("instanceId2");
        asg.setInstances(Arrays.asList(instance1, instance2));
        autoScalingGroups.add(asg);
        result.setAutoScalingGroups(autoScalingGroups);
        return result;
    }

    private DescribeInstancesResult createDescribeInstanceResult() {
        DescribeInstancesResult result = new DescribeInstancesResult();
        List<Reservation> reservations = new ArrayList<>();
        Reservation res = new Reservation();
        com.amazonaws.services.ec2.model.Instance instance = new com.amazonaws.services.ec2.model.Instance();
        InstanceNetworkInterface ini = new InstanceNetworkInterface();
        ini.setNetworkInterfaceId("iniId");
        instance.setNetworkInterfaces(Arrays.asList(ini));
        res.setInstances(Arrays.asList(instance));
        reservations.add(res);
        result.setReservations(reservations);
        return result;
    }
}
