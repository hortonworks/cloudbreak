package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

@ExtendWith(MockitoExtension.class)
public class AwsInstanceConnectorTest {

    private static final int SMALL_INSTANCE_SIZE = 10;

    private static final int BIG_INSTANCE_SIZE = 203;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @InjectMocks
    private AwsInstanceConnector underTest;

    private AtomicInteger instanceIdCounter = new AtomicInteger(1);

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
    }

    @Test
    public void testCheckCallsDescribeInstanceStatusesWhenVmsAreNotTerminated() {
        List<CloudInstance> vms = cloudInstances(SMALL_INSTANCE_SIZE);
        setVmStatusesOnProvider(runningState());

        underTest.check(ac, vms);

        verify(amazonEc2Client, times(1)).describeInstanceStatuses(describeInstanceStatusRequest(vms));
        verify(amazonEc2Client, times(0)).describeInstances(any());
    }

    @Test
    public void testCheckCallsDescribeInstancesForTerminatedInstances() {
        List<CloudInstance> vms = cloudInstances(SMALL_INSTANCE_SIZE);
        setVmStatusesOnProvider(runningState(), Map.of(
                "i-5", terminatedState(),
                "i-7", terminatedState()));
        setVmsOnProvider(terminatedState());

        underTest.check(ac, vms);

        verify(amazonEc2Client, times(1)).describeInstanceStatuses(describeInstanceStatusRequest(vms));
        verify(amazonEc2Client, times(1)).describeInstances(describeInstancesRequest("i-5", "i-7"));
    }

    @Test
    public void testCheckSplitsStatusChecksWhenTooManyVmsAreDescribed() {
        List<CloudInstance> vms = cloudInstances(BIG_INSTANCE_SIZE);
        setVmStatusesOnProvider(runningState(), Map.of(
                "i-9", terminatedState(),
                "i-109", terminatedState(),
                "i-201", terminatedState()));
        setVmsOnProvider(terminatedState());

        underTest.check(ac, vms);

        verify(amazonEc2Client, times(1)).describeInstanceStatuses(describeInstanceStatusRequest(vms.subList(0, 100)));
        verify(amazonEc2Client, times(1)).describeInstanceStatuses(describeInstanceStatusRequest(vms.subList(100, 200)));
        verify(amazonEc2Client, times(1)).describeInstanceStatuses(describeInstanceStatusRequest(vms.subList(200, 203)));
        verify(amazonEc2Client, times(1)).describeInstances(describeInstancesRequest("i-9", "i-109", "i-201"));
    }

    @TestFactory
    public Collection<DynamicTest> testStatusMapping() {
        return List.of(
                newTest("running state to STARTED", runningState(), STARTED),
                newTest("terminated state to TERMINATED", terminatedState(), TERMINATED),
                newTest("stopped state to STOPPED", stoppedState(), STOPPED),
                newTest("other state to IN_PROGRESS", instanceState("anything", 81), IN_PROGRESS),
                newTest("running state to STARTED (not case sensitive)", instanceState("runninG", 16), STARTED),
                newTest("Terminated state to TERMINATED (not case sensitive)", instanceState("Terminated", 48), TERMINATED),
                newTest("STOPPED state to STOPPED (not case sensitive)", instanceState("STOPPED", 80), STOPPED)
        );
    }

    private DynamicTest newTest(String name, InstanceState instanceState, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus expectedInstanceStatus) {
        return DynamicTest.dynamicTest(name, () -> testStatusMapping(instanceState, expectedInstanceStatus));
    }

    private void testStatusMapping(InstanceState instanceState, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus status) {
        List<CloudInstance> vms = cloudInstances(BIG_INSTANCE_SIZE);
        setVmStatusesOnProvider(instanceState);
        if (AwsInstanceStatusMapper.TERMINATED.equalsIgnoreCase(instanceState.getName())) {
            setVmsOnProvider(instanceState);
        }
        List<CloudVmInstanceStatus> result = underTest.check(ac, vms);
        MatcherAssert.assertThat(result, hasSize(BIG_INSTANCE_SIZE));
        MatcherAssert.assertThat(result, everyItem(hasProperty("status", is(status))));
    }

    private DescribeInstancesRequest describeInstancesRequest(String... ids) {
        return new DescribeInstancesRequest().withInstanceIds(ids);
    }

    private DescribeInstanceStatusRequest describeInstanceStatusRequest(List<CloudInstance> vms) {
        return new DescribeInstanceStatusRequest()
                .withIncludeAllInstances(true)
                .withInstanceIds(vms.stream()
                        .map(CloudInstance::getInstanceId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private List<CloudInstance> cloudInstances(int count) {
        return IntStream.range(0, count)
                .boxed()
                .map(i -> cloudInstance())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private CloudInstance cloudInstance() {
        return new CloudInstance("i-" + instanceIdCounter.getAndIncrement(), null, null, "subnet-1", "az-1");
    }

    private InstanceState runningState() {
        return new InstanceState().withName(AwsInstanceStatusMapper.RUNNING).withCode(16);
    }

    private InstanceState terminatedState() {
        return new InstanceState().withName(AwsInstanceStatusMapper.TERMINATED).withCode(48);
    }

    private InstanceState stoppedState() {
        return new InstanceState().withName(AwsInstanceStatusMapper.STOPPED).withCode(80);
    }

    private InstanceState instanceState(String name, int code) {
        return new InstanceState().withName(name).withCode(code);
    }

    private void setVmStatusesOnProvider(InstanceState instanceState) {
        doAnswer(invocation -> new DescribeInstanceStatusResult()
                .withInstanceStatuses(invocation.getArgument(0, DescribeInstanceStatusRequest.class)
                        .getInstanceIds()
                        .stream()
                        .map(id -> new InstanceStatus()
                                .withInstanceId(id)
                                .withInstanceState(instanceState))
                        .collect(toList())))
                .when(amazonEc2Client).describeInstanceStatuses(any());
    }

    private void setVmStatusesOnProvider(InstanceState instanceState, Map<String, InstanceState> specialStates) {
        doAnswer(invocation -> new DescribeInstanceStatusResult()
                .withInstanceStatuses(invocation.getArgument(0, DescribeInstanceStatusRequest.class)
                        .getInstanceIds()
                        .stream()
                        .map(id -> new InstanceStatus()
                                .withInstanceId(id)
                                .withInstanceState(specialStates.getOrDefault(id, instanceState)))
                        .collect(toList())))
                .when(amazonEc2Client).describeInstanceStatuses(any());
    }

    private void setVmsOnProvider(InstanceState instanceState) {
        doAnswer(invocation -> new DescribeInstancesResult()
                .withReservations(invocation.getArgument(0, DescribeInstancesRequest.class)
                        .getInstanceIds()
                        .stream()
                        .map(id -> new Reservation()
                                .withReservationId("r-" + id)
                                .withInstances(new Instance().withInstanceId(id).withState(instanceState)))
                        .collect(toList()))).when(amazonEc2Client).describeInstances(any());
    }
}