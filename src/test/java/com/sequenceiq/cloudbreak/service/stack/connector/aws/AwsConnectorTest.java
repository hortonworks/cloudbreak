package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import reactor.core.Reactor;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsStackDescription;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.DetailedAwsStackDescription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;

public class AwsConnectorTest {
    private static final String DUMMY_NUMBER_STR = "1";
    private static final int DUMMY_NUMBER = 1;
    private static final String DUMMY_SERVICE = "Dummy service";
    @InjectMocks
    private AwsConnector underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private Reactor reactor;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonEC2Client ec2Client;

    // Domain models

    private User user;

    private Stack stack;

    private AwsCredential credential;

    private AwsTemplate awsTemplate;

    private DescribeStacksResult stackResult;

    private DescribeInstancesResult instancesResult;

    private DescribeStackResourcesResult stackResourcesResult;

    @Before
    public void setUp() {
        underTest = new AwsConnector();
        MockitoAnnotations.initMocks(this);
        user = AwsConnectorTestUtil.createUser();
        awsTemplate = AwsConnectorTestUtil.createAwsTemplate(user);
        credential = AwsConnectorTestUtil.createAwsCredential();
        stack = AwsConnectorTestUtil.createStack(user, credential, awsTemplate);
        instancesResult = AwsConnectorTestUtil.createDescribeInstanceResult();
    }

    @Test
    public void testDescribeStack() {
        // GIVEN
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(stackResult);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        StackDescription result = underTest.describeStack(user, stack, credential);
        // THEN
        verify(ec2Client, times(1)).describeInstances(any(DescribeInstancesRequest.class));
        assertTrue(result.getClass().isAssignableFrom(AwsStackDescription.class));
    }

    @Test
    public void testDescribeStackWhenDescribeStacksShouldThrowAmazonServiceException() {
        // GIVEN
        AmazonServiceException e = createAmazonServiceException();
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willThrow(e);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        StackDescription result = underTest.describeStack(user, stack, credential);
        // THEN
        verify(ec2Client, times(1)).describeInstances(any(DescribeInstancesRequest.class));
        assertTrue(result.getClass().isAssignableFrom(AwsStackDescription.class));
    }

    @Test(expected = AmazonServiceException.class)
    public void testDescribeStackWhenDescribeStacksShouldThrowAmazonServiceExceptionWithoutCloudFormService() {
        // GIVEN
        AmazonServiceException e = createAmazonServiceException();
        e.setServiceName(DUMMY_SERVICE);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willThrow(e);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        underTest.describeStack(user, stack, credential);
    }

    @Test
    public void testDescribeStackWithResources() {
        // GIVEN
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(stackResult);
        given(amazonCloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class))).willReturn(stackResourcesResult);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(ec2Client, times(1)).describeInstances(any(DescribeInstancesRequest.class));
        assertTrue(result.getClass().isAssignableFrom(DetailedAwsStackDescription.class));
    }

    @Test
    public void testDescribeStackWithResourcesWhenDescribeStacksShouldThrowAmazonServiceException() {
        // GIVEN
        AmazonServiceException e = createAmazonServiceException();
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willThrow(e);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(ec2Client, times(1)).describeInstances(any(DescribeInstancesRequest.class));
        assertTrue(result.getClass().isAssignableFrom(DetailedAwsStackDescription.class));
    }

    @Test
    public void testDescribeStackWithResourcesWhenDescribeStackResourcesShouldThrowAmazonServiceException() {
        // GIVEN
        AmazonServiceException e = createAmazonServiceException();
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(stackResult);
        given(amazonCloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class))).willThrow(e);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        // WHEN
        underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(ec2Client, times(1)).describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test(expected = AmazonServiceException.class)
    public void testDescribeStackWithResourcesWhenDescribeStackResourcesShouldThrowAmazonServiceExceptionWithoutCloudFormService() {
        // GIVEN
        AmazonServiceException e = createAmazonServiceException();
        e.setServiceName(DUMMY_SERVICE);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(stackResult);
        given(amazonCloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class))).willThrow(e);
        // WHEN
        underTest.describeStackWithResources(user, stack, credential);
    }

    @Test
    public void testDeleteStack() {
        // GIVEN
        instancesResult.setReservations(generateReservationsWithInstances());
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
    }

    @Test
    public void testDeleteStackWithoutInstanceResultReservations() {
        // GIVEN
        instancesResult.setReservations(new ArrayList<Reservation>());
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(amazonCloudFormationClient);
        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
    }

    @Test
    public void testDeleteStackWithoutCfStackName() {
        // GIVEN
        instancesResult.setReservations(new ArrayList<Reservation>());
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
        stack.setCfStackName(null);
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(amazonCloudFormationClient, times(0)).deleteStack(any(DeleteStackRequest.class));
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
