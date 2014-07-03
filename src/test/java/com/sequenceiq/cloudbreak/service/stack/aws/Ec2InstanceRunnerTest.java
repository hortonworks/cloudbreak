package com.sequenceiq.cloudbreak.service.stack.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceStatusDetails;
import com.amazonaws.services.ec2.model.InstanceStatusSummary;
import com.amazonaws.services.ec2.model.InstanceState;
import com.google.common.collect.Lists;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.Reactor;
import reactor.event.Event;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

public class Ec2InstanceRunnerTest {
    private static final String DUMMY_ROLE_ARN = "dummyRoleArn";
    private static final String RUNNING_STATE = "RUNNING";
    private static final String PASSED = "passed";
    private static final String REACHABILITY = "reachability";
    private static final String RUNNING = "running";
    private static final String OK = "ok";

    @InjectMocks
    @Spy
    private Ec2InstanceRunner underTest = new Ec2InstanceRunner();

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private Ec2UserDataBuilder userDataBuilder;

    @Mock
    private Reactor reactor;

    @Mock
    private AmazonCloudFormationClient cfClient;

    @Mock
    private AmazonEC2Client ec2Client;

    @Mock
    private AmbariClient ambariClient;

    private Event<Stack> stackEvent;

    private AwsCredential credential;

    private AwsTemplate awsTemplate;

    private DescribeStacksResult stackResult;

    private com.amazonaws.services.cloudformation.model.Stack cfStack;

    private Output output1;
    private Output output2;

    private RunInstancesResult runInstancesResult;
    private DescribeInstancesResult describeInstanceResult;
    private DescribeInstanceStatusResult describeInstanceStatusResult;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        User user = AwsStackTestUil.createUser();
        credential = AwsStackTestUil.createAwsCredential();
        credential.setAwsCredentialOwner(user);
        credential.setInstanceProfileRoleArn(DUMMY_ROLE_ARN);
        awsTemplate = AwsStackTestUil.createAwsTemplate(user);
        stackEvent = new Event<>(AwsStackTestUil.createStack(user, credential, awsTemplate));
        output1 = new Output();
        output2 = new Output();
        output1.setOutputKey("Subnet");
        output1.setOutputValue("dummySubnet");
        output2.setOutputKey("SecurityGroup");
        output2.setOutputValue("dummySecurityGroup");
        cfStack = new com.amazonaws.services.cloudformation.model.Stack();
        cfStack.setOutputs(Arrays.asList(output1, output2));
        stackResult = new DescribeStacksResult();
        stackResult.setStacks(Arrays.asList(cfStack));
        runInstancesResult = new RunInstancesResult();
        runInstancesResult.setReservation(generateInstancesForReservation());
        describeInstanceResult = new DescribeInstancesResult();
        describeInstanceResult.setReservations(generateDescribeInstancesForReservation());
        describeInstanceStatusResult = new DescribeInstanceStatusResult();
        describeInstanceStatusResult.setInstanceStatuses(generateInstanceStatuses());
    }

    @Test
    public void testAcceptEventStackCreateMethodFinished() {
        //GIVEN
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(cfClient);
        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, credential)).willReturn(ec2Client);
        given(cfClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(stackResult);
        given(ec2Client.runInstances(any(RunInstancesRequest.class))).willReturn(runInstancesResult);
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(describeInstanceResult);
        given(ec2Client.describeInstanceStatus(any(DescribeInstanceStatusRequest.class))).willReturn(describeInstanceStatusResult);
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.healthCheck()).willReturn(RUNNING_STATE);
        doNothing().when(awsStackUtil).sleep(anyInt());
        //WHEN.
        underTest.accept(stackEvent);
        //THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private Reservation generateInstancesForReservation() {
        Reservation result = new Reservation();
        List<Instance> instances = Lists.newArrayList();
        instances.add(new Instance().withInstanceId(String.valueOf(new Random().nextInt(100))));
        instances.add(new Instance().withInstanceId(String.valueOf(new Random().nextInt(100))));
        instances.add(new Instance().withInstanceId(String.valueOf(new Random().nextInt(100))));
        result.setInstances(instances);
        return result;
    }

    private Collection<Reservation> generateDescribeInstancesForReservation() {
        List<Reservation> reservations = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            Reservation r = new Reservation();
            List<Instance> instances = Lists.newArrayList();
            Instance instance = new Instance().withInstanceId(String.valueOf(new Random().nextInt(100)));
            InstanceNetworkInterface iNetI = new InstanceNetworkInterface();
            iNetI.setNetworkInterfaceId(String.valueOf(i));
            instance.setNetworkInterfaces(Arrays.asList(iNetI));
            instance.setAmiLaunchIndex(0);
            instance.setPublicIpAddress(AwsStackTestUil.AMBARI_IP);
            instances.add(instance);
            r.setInstances(instances);
            reservations.add(r);
        }
        return reservations;
    }

    private Collection<InstanceStatus> generateInstanceStatuses() {
        List<InstanceStatus> statusList = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            InstanceStatus is = new InstanceStatus();
            is.setInstanceId(String.valueOf(new Random().nextInt(100)));

            InstanceStatusSummary stateSummary = new InstanceStatusSummary();
            stateSummary.setStatus(OK);
            InstanceState state = new InstanceState();
            state.setName(RUNNING);
            InstanceStatusDetails details = new InstanceStatusDetails();
            details.setName(REACHABILITY);
            details.setStatus(PASSED);
            stateSummary.setDetails(Arrays.asList(details));
            is.setInstanceStatus(stateSummary);
            is.setInstanceState(state);
            statusList.add(is);
        }
        return statusList;
    }
}
