package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.RetryService;

import io.opentracing.Tracer;

@ExtendWith(SpringExtension.class)

@TestPropertySource(properties = {
        "cb.aws.hostkey.verify=true",
        "cb.vm.status.polling.interval=1",
        "cb.vm.status.polling.attempt=7",
        "cb.vm.retry.backoff.delay=20",
        "cb.vm.retry.backoff.multiplier=2",
        "cb.vm.retry.backoff.maxdelay=10000",
        "cb.vm.retry.attempt=5"
})
public class AwsInstanceConnectorTest {

    private static final int POLLING_LIMIT = 7;

    @Inject
    private AwsInstanceConnector underTest;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @MockBean
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private InstanceProfileCredentialsProvider instanceProfileCredentialsProvider;

    @MockBean
    private Tracer tracer;

    @SpyBean
    private CommonAwsClient commonAwsClient;

    @MockBean
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    private AuthenticatedContext authenticatedContext;

    private List<CloudInstance> inputList;

    @BeforeEach
    public void awsClientSetup() {
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2Client(any(AwsCredentialView.class));
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2Client(any(AwsCredentialView.class), anyString());

        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region")))
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential("id", "alma",
                Map.of("accessKey", "ac", "secretKey", "secret"), false);
        authenticatedContext = awsAuthenticator.authenticate(context, credential);

        StopInstancesResult stopInstancesResult = new StopInstancesResult();
        StartInstancesResult startInstanceResult = new StartInstancesResult();
        when(amazonEC2Client.stopInstances(any(StopInstancesRequest.class))).thenReturn(stopInstancesResult);
        when(amazonEC2Client.startInstances(any(StartInstancesRequest.class))).thenReturn(startInstanceResult);

        inputList = getCloudInstances();
    }

    @TestFactory
    public Collection<DynamicTest> testCheckStatuses() {
        ArrayList<DynamicTest> tests = new ArrayList<>();
        tests.add(
                DynamicTest.dynamicTest(
                        "running state to STARTED",
                        () -> testCheckStates("running", 16, InstanceStatus.STARTED)));
        tests.add(
                DynamicTest.dynamicTest(
                        "terminated state to TERMINATED",
                        () -> testCheckStates("terminated", 48, InstanceStatus.TERMINATED)));
        tests.add(
                DynamicTest.dynamicTest(
                        "stopped state to STOPPED",
                        () -> testCheckStates("stopped", 16, InstanceStatus.STOPPED)));
        tests.add(
                DynamicTest.dynamicTest(
                        "other state to IN_PROGRESS",
                        () -> testCheckStates("anything", 81, InstanceStatus.IN_PROGRESS)));
        tests.add(
                DynamicTest.dynamicTest(
                        "running state to STARTED (not case sensitive)",
                        () -> testCheckStates("runninG", 16, InstanceStatus.STARTED)));
        tests.add(
                DynamicTest.dynamicTest(
                        "Terminated state to TERMINATED (not case sensitive)",
                        () -> testCheckStates("Terminated", 48, InstanceStatus.TERMINATED)));
        tests.add(
                DynamicTest.dynamicTest(
                        "STOPPED state to STOPPED (not case sensitive)",
                        () -> testCheckStates("STOPPED", 16, InstanceStatus.STOPPED)));
        return tests;
    }

    @Test
    public void testCheckException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        List<CloudInstance> list = getCloudInstances();
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.check(authenticatedContext, list));
        Assert.assertThat(list, hasSize(2));
    }

    @Test
    public void testCheckExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = getCloudInstances().stream().collect(toCollection(ArrayList::new));
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.check(authenticatedContext, mutableList));
        Assert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testCheckSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenThrow(new SdkClientException("lamb"),
                new SdkClientException("sheep"),
                new SdkClientException("shepherd")).thenReturn(getDescribeInstancesResult("running", 16));
        List<CloudInstance> list = getCloudInstances();
        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, list);
        verify(amazonEC2Client, times(4)).describeInstances(any(DescribeInstancesRequest.class));
        Assert.assertThat(result, hasSize(2));
    }

    @Test
    public void testStartPollingWithSuccess() {
        String status = "Running";
        InstanceStatus stopped1 = AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status);
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 2, status, lastStatusCode);
        ArgumentCaptor<StartInstancesRequest> captorStart = ArgumentCaptor.forClass(StartInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).startInstances(captorStart.capture());
        Assertions.assertTrue(captorStart.getValue().getInstanceIds().size() == inputList.size());
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(stopped1)))));
    }

    @Test
    public void testStopPollingWithSuccess() {
        String status = "Stopped";
        InstanceStatus stopped1 = AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status);
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 2, status, lastStatusCode);
        ArgumentCaptor<StopInstancesRequest> captorStop = ArgumentCaptor.forClass(StopInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).stopInstances(captorStop.capture());
        Assertions.assertTrue(captorStop.getValue().getInstanceIds().size() == inputList.size());
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(stopped1)))));
    }

    @Test
    public void testStartSomeInstancesStarted() {
        mockDescribeInstancesOneIsRunningLastSuccess(POLLING_LIMIT - 2);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() < inputList.size());
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStopSomeInstancesStopped() {
        mockDescribeInstancesOneIsStoppedLastSuccess(POLLING_LIMIT - 2);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() < inputList.size());
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STOPPED)))));
    }

    @Test
    public void testRebootEveryInstancesStarted() {
        mockDescribeInstancesAllisRebooted(POLLING_LIMIT - 2);
        ArgumentCaptor<StopInstancesRequest> stopCaptor = ArgumentCaptor.forClass(StopInstancesRequest.class);
        ArgumentCaptor<StartInstancesRequest> startCaptor = ArgumentCaptor.forClass(StartInstancesRequest.class);
        List<CloudVmInstanceStatus> result = underTest.reboot(authenticatedContext, new ArrayList<>(), inputList);

        verify(amazonEC2Client, times(2)).stopInstances(stopCaptor.capture());
        verify(amazonEC2Client, times(2)).startInstances(startCaptor.capture());
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStartEveryInstancesStartedAlready() {
        mockDescribeInstancesAllIsRunning(POLLING_LIMIT - 2);
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, never()).startInstances(any(StartInstancesRequest.class));
        Assert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStartPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Running", 16);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() == inputList.size());
    }

    @Test
    public void testStopPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Stopped", 41);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() == inputList.size());
    }

    @Test
    public void testStartException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        Assert.assertThat(inputList, hasSize(2));
    }

    @Test
    public void testStopException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        Assert.assertThat(inputList, hasSize(2));
    }

    @Test
    public void testStartExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = getCloudInstances().stream().collect(toCollection(ArrayList::new));
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.start(authenticatedContext, List.of(), mutableList));
        Assert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testStopExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = getCloudInstances().stream().collect(toCollection(ArrayList::new));
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), mutableList));
        Assert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testStartSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenThrow(new SdkClientException("lamb"),
                new SdkClientException("sheep"),
                new SdkClientException("shepherd")).thenReturn(getDescribeInstancesResult("running", 16));
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(5)).describeInstances(any(DescribeInstancesRequest.class));
        Assert.assertThat(result, hasSize(2));
    }

    @Test
    public void testStopSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenThrow(new SdkClientException("lamb"),
                new SdkClientException("sheep"),
                new SdkClientException("shepherd")).thenReturn(getDescribeInstancesResult("stopped", 55));
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(5)).describeInstances(any(DescribeInstancesRequest.class));
        Assert.assertThat(result, hasSize(2));
    }

    private void mockDescribeInstances(int pollResponses, String lastStatus, int lastStatusCode) {
        mockListOfDescribeInstances(getDescribeInstancesResult("notrunning", 16), pollResponses,
                getDescribeInstancesResult(lastStatus, lastStatusCode));
    }

    private void mockDescribeInstancesOneIsRunningLastSuccess(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstancesResultOneRunning("notrunning", 16), pollResponses,
                getDescribeInstancesResult("running", 16));
    }

    private void mockDescribeInstancesOneIsStoppedLastSuccess(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstancesResultOneStopped("notrunning", 16), pollResponses,
                getDescribeInstancesResult("stopped", 16));
    }

    private void mockDescribeInstancesAllIsRunning(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstancesResult("running", 16), pollResponses,
                getDescribeInstancesResult("running", 16));
    }

    private void mockDescribeInstancesAllisRebooted(int pollResponses) {
        mockListOfDescribeInstancesStopAndThenRunning(getDescribeInstancesResultOneRunning("running", 16), pollResponses,
                getDescribeInstancesResult("stopped", 16));
    }

    private void mockListOfDescribeInstancesStopAndThenRunning(DescribeInstancesResult cons, int repeatNo, DescribeInstancesResult stopped) {
        DescribeInstancesResult[] describeInstancesResults = new DescribeInstancesResult[repeatNo * 2];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[2] = stopped;
        describeInstancesResults[4] = stopped;
        describeInstancesResults[5] = stopped;
        describeInstancesResults[6] = stopped;
        describeInstancesResults[8] = stopped;
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockListOfDescribeInstances(DescribeInstancesResult cons, int repeatNo, DescribeInstancesResult last) {
        DescribeInstancesResult[] describeInstancesResults = new DescribeInstancesResult[repeatNo];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[repeatNo - 1] = last;
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockDescribeInstancesException(String errorCode, String errorMessage) {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).then(invocation -> {
            AmazonEC2Exception exception = new AmazonEC2Exception("Sheep lost control");
            exception.setErrorCode(errorCode);
            exception.setErrorMessage(errorMessage);
            throw exception;
        });
    }

    private void testCheckStates(String running, int code, InstanceStatus status) {
        List<CloudInstance> list = getCloudInstances();
        DescribeInstancesResult instancesResult = getDescribeInstancesResult(running, code);
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(instancesResult);
        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, list);
        Assert.assertThat(result, hasSize(2));
        Assert.assertThat(result, everyItem(hasProperty("status", is(status))));
    }

    private DescribeInstancesResult getDescribeInstancesResult(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", state, code);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return new DescribeInstancesResult().withReservations(reservation1, reservation2);
    }

    private DescribeInstancesResult getDescribeInstancesResultOneRunning(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", "running", 16);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return new DescribeInstancesResult().withReservations(reservation1, reservation2);
    }

    private DescribeInstancesResult getDescribeInstancesResultOneStopped(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", "stopped", 16);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return new DescribeInstancesResult().withReservations(reservation1, reservation2);
    }

    private List<CloudInstance> getCloudInstances() {
        CloudInstance instance1 = new CloudInstance("i-1", null, null, "subnet-123", "az1");
        CloudInstance instance2 = new CloudInstance("i-2", null, null, "subnet-123", "az1");
        return List.of(instance1, instance2);
    }

    private Reservation getReservation(Instance instances1, String s) {
        return new Reservation().withReservationId(s).withInstances(instances1);
    }

    private Instance getAwsInstance(String s, String state, int code) {
        return new Instance().withState(new InstanceState().withName(state).withCode(code)).withInstanceId(s);
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    @Import({AwsInstanceConnector.class,
            AwsAuthenticator.class,
            CommonAwsClient.class,
            PollerUtil.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class
    })
    static class Config {
    }

    interface TestCall {
        List<CloudVmInstanceStatus> testCall(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms);
    }
}
