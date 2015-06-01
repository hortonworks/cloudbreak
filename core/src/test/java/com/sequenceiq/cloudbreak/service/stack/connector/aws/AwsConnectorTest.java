package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;

import reactor.bus.EventBus;

public class AwsConnectorTest {
    private static final String DUMMY_NUMBER_STR = "1";
    private static final int DUMMY_NUMBER = 1;
    private static final String DUMMY_SERVICE = "Dummy service";
    @InjectMocks
    private AwsConnector underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private EventBus reactor;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonEC2Client ec2Client;

    // Domain models

    private Stack stack;

    private Credential credential;

    private Template awsTemplate;

    private DescribeStacksResult stackResult;

    private DescribeInstancesResult instancesResult;

    private DescribeStackResourcesResult stackResourcesResult;

    @Before
    public void setUp() {
        underTest = new AwsConnector();
        MockitoAnnotations.initMocks(this);
        awsTemplate = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        credential = ServiceTestUtils.createCredential(CloudPlatform.AWS);
        stack = ServiceTestUtils.createStack(awsTemplate, credential, getDefaultResourceSet());
        instancesResult = ServiceTestUtils.createDescribeInstanceResult();
    }

    public Set<Resource> getDefaultResourceSet() {
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, ConnectorTestUtil.CF_STACK_NAME, stack, "master"));
        return resources;
    }

    @Test
    @Ignore
    public void testDeleteStack() {
        // GIVEN
        instancesResult.setReservations(generateReservationsWithInstances());
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(amazonCloudFormationClient);
        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
        // WHEN
        underTest.deleteStack(stack, credential);
        // THEN
        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
    }

    @Test
    @Ignore
    public void testDeleteStackWithoutInstanceResultReservations() {
        // GIVEN
        instancesResult.setReservations(new ArrayList<Reservation>());
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(amazonCloudFormationClient);
        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
        // WHEN
        underTest.deleteStack(stack, credential);
        // THEN
        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
    }

    private AmazonServiceException createAmazonServiceException() {
        AmazonServiceException e = new AmazonServiceException(String.format("Stack:%s does not exist", ConnectorTestUtil.CF_STACK_NAME));
        e.setServiceName("AmazonCloudFormation");
        e.setErrorCode(DUMMY_NUMBER_STR);
        e.setRequestId(DUMMY_NUMBER_STR);
        e.setStatusCode(DUMMY_NUMBER);
        return e;
    }

    private List<Reservation> generateReservationsWithInstances() {
        List<Reservation> reservations = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            Reservation r = new Reservation();
            List<Instance> instances = Lists.newArrayList();
            instances.add(new Instance().withInstanceId(String.valueOf(new Random().nextInt(100))));
            r.setInstances(instances);
            reservations.add(r);
        }
        return reservations;
    }

}
