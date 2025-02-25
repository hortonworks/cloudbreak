package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_INSTANCE_CAPACITY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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

import com.dyngr.exception.PollerStoppedException;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsRegionEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsServiceEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.RetryService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "cb.aws.hostkey.verify=true",
        "cb.vm.status.polling.interval=1",
        "cb.vm.status.polling.attempt=3",
        "cb.vm.retry.backoff.delay=10",
        "cb.vm.retry.backoff.multiplier=2",
        "cb.vm.retry.backoff.maxdelay=100",
        "cb.vm.retry.attempt=3"
})
class AwsInstanceConnectorTest {

    private static final int POLLING_LIMIT = 7;

    @Inject
    private AwsInstanceConnector underTest;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @MockBean
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @SpyBean
    private AwsApacheClient awsApacheClient;

    @SpyBean
    private CommonAwsClient commonAwsClient;

    @MockBean
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    @MockBean
    private AwsPageCollector awsPageCollector;

    @MockBean
    private AwsMetricPublisher awsMetricPublisher;

    private AuthenticatedContext authenticatedContext;

    private List<CloudInstance> inputList;

    private List<CloudInstance> largeInputList;

    @BeforeEach
    void awsClientSetup() {
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2Client(any(AwsCredentialView.class));
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2Client(any(AwsCredentialView.class), anyString());
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2ClientForStartInstancesOperation(any(AwsCredentialView.class));
        doReturn(amazonEC2Client).when(commonAwsClient).createEc2ClientForStartInstancesOperation(any(AwsCredentialView.class), anyString());

        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region")))
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential("id", "alma", Map.of("accessKey", "ac", "secretKey", "secret"), "acc");
        authenticatedContext = awsAuthenticator.authenticate(context, credential);

        StopInstancesResponse stopInstancesResult = StopInstancesResponse.builder().build();
        StartInstancesResponse startInstanceResult = StartInstancesResponse.builder().build();
        when(amazonEC2Client.stopInstances(any(StopInstancesRequest.class))).thenReturn(stopInstancesResult);
        when(amazonEC2Client.startInstances(any(StartInstancesRequest.class))).thenReturn(startInstanceResult);

        inputList = getCloudInstances();
        largeInputList = getLargeCountCloudInstances();
    }

    @TestFactory
    Collection<DynamicTest> testCheckStatuses() {
        List<DynamicTest> tests = new ArrayList<>();
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
    void testCheckIfInstanceMissing() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(getDescribeInstancesResult("running", 16));
        List<CloudInstance> extistingInstances = getCloudInstances();
        List<CloudInstance> list = new ArrayList<>(extistingInstances);
        CloudInstance nonExistingInstance = new CloudInstance("i-xxxx", null, null, "subnet-123", "az1");
        list.add(nonExistingInstance);
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = underTest.check(authenticatedContext, list);
        assertThat(cloudVmInstanceStatuses, hasSize(3));
        assertThat(cloudVmInstanceStatuses, hasItem(new CloudVmInstanceStatus(extistingInstances.get(0), InstanceStatus.STARTED)));
        assertThat(cloudVmInstanceStatuses, hasItem(new CloudVmInstanceStatus(extistingInstances.get(1), InstanceStatus.STARTED)));
        assertThat(cloudVmInstanceStatuses, hasItem(new CloudVmInstanceStatus(nonExistingInstance, InstanceStatus.TERMINATED)));
    }

    @Test
    void testCheckException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        List<CloudInstance> list = getCloudInstances();
        assertThrows(Ec2Exception.class, () -> underTest.check(authenticatedContext, list));
        assertThat(list, hasSize(2));
    }

    @Test
    void testCheckExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        assertThrows(Ec2Exception.class, () -> underTest.check(authenticatedContext, mutableList));
        assertThat(mutableList, hasSize(1));
    }

    @Test
    void testCheckSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(
                        SdkClientException.builder().message("lamb").build(),
                        SdkClientException.builder().message("sheep").build())
                .thenReturn(getDescribeInstancesResult("running", 16));
        List<CloudInstance> list = getCloudInstances();
        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, list);
        verify(amazonEC2Client, times(3)).describeInstances(any(DescribeInstancesRequest.class));
        assertThat(result, hasSize(2));
    }

    @Test
    void testStartPollingWithSuccess() {
        String status = "Running";
        InstanceStatus stopped1 = AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status);
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 6, status, lastStatusCode);
        ArgumentCaptor<StartInstancesRequest> captorStart = ArgumentCaptor.forClass(StartInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).startInstances(captorStart.capture());
        assertEquals(inputList.size(), captorStart.getValue().instanceIds().size());
        assertThat(result, hasItem(allOf(hasProperty("status", is(stopped1)))));
    }

    @Test
    void testSuccessfulChunkedStartDuringResilientStart() {
        Predicate<List<String>> instanceIdChecker = instanceIdList ->
                instanceIdList.stream().anyMatch(instanceId -> StringUtils.contains(instanceId, "large"));
        List<String> batchesAwsDescribeExecutedAgainstOnce = new ArrayList<>();
        when(amazonEC2Client.describeInstances(any()))
                .thenAnswer(i -> {
                    DescribeInstancesRequest request = (DescribeInstancesRequest) i.getArguments()[0];
                    DescribeInstancesResponse notRunning = DescribeInstancesResponse.builder().reservations(request.instanceIds().stream().map(instanceId ->
                            getReservation(getAwsInstance(instanceId, "stopped", 16), instanceId)).toList()).build();
                    if (instanceIdChecker.test(request.instanceIds())) {
                        return notRunning;
                    } else {
                        String batch = Joiner.on(',').join(request.instanceIds());
                        if (!batchesAwsDescribeExecutedAgainstOnce.contains(batch)) {
                            batchesAwsDescribeExecutedAgainstOnce.add(batch);
                            return notRunning;
                        } else {
                            return DescribeInstancesResponse.builder().reservations(request.instanceIds().stream().map(instanceId ->
                                    getReservation(getAwsInstance(instanceId, "running", 16), instanceId)).toList()).build();
                        }
                    }
                });
        AwsServiceException awsServiceException = Ec2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("whatever").build())
                .message("sad")
                .build();
        when(amazonEC2Client.startInstances(argThat(request -> instanceIdChecker.test(request.instanceIds())))).thenThrow(awsServiceException);

        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), largeInputList);

        verify(amazonEC2Client, times(22)).startInstances(any());
        assertThat(result, hasItem(allOf(
                hasProperty("status", is(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Running"))),
                hasProperty("cloudInstance",
                        hasProperty("template", hasProperty("groupName", is("small1")))))));
        assertThat(result, hasItem(allOf(
                hasProperty("status", is(AwsInstanceStatusMapper.getInstanceStatusByAwsStatus("Running"))),
                hasProperty("cloudInstance",
                        hasProperty("template", hasProperty("groupName", is("small2")))))));
    }

    @Test
    void testSpringRetrySkipDuringResilientStart() {
        AtomicBoolean toggle = new AtomicBoolean(true);
        when(amazonEC2Client.describeInstances(any())).thenThrow(SdkClientException.builder().message("whatever").build());

        assertThrows(CloudConnectorException.class, () -> underTest.start(authenticatedContext, List.of(), largeInputList));

        verify(amazonEC2Client, never()).startInstances(any());
    }

    @Test
    void testStopPollingWithSuccess() {
        String status = "Stopped";
        InstanceStatus stopped1 = AwsInstanceStatusMapper.getInstanceStatusByAwsStatus(status);
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 6, status, lastStatusCode);
        ArgumentCaptor<StopInstancesRequest> captorStop = ArgumentCaptor.forClass(StopInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).stopInstances(captorStop.capture());
        assertEquals(inputList.size(), captorStop.getValue().instanceIds().size());
        assertThat(result, hasItem(allOf(hasProperty("status", is(stopped1)))));
    }

    @Test
    void testStartSomeInstancesStarted() {
        mockDescribeInstancesOneIsRunningLastSuccess(POLLING_LIMIT - 6);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        assertTrue(captor.getValue().instanceIds().size() < inputList.size());
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    void testStopSomeInstancesStopped() {
        mockDescribeInstancesOneIsStoppedLastSuccess(POLLING_LIMIT - 6);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        assertTrue(captor.getValue().instanceIds().size() < inputList.size());
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STOPPED)))));
    }

    @Test
    void testRebootEveryInstancesStarted() {
        mockDescribeInstancesAllisRebooted(8);
        ArgumentCaptor<StopInstancesRequest> stopCaptor = ArgumentCaptor.forClass(StopInstancesRequest.class);
        ArgumentCaptor<StartInstancesRequest> startCaptor = ArgumentCaptor.forClass(StartInstancesRequest.class);
        List<CloudVmInstanceStatus> result = underTest.reboot(authenticatedContext, new ArrayList<>(), inputList);

        verify(amazonEC2Client, times(1)).stopInstances(stopCaptor.capture());
        verify(amazonEC2Client, times(1)).startInstances(startCaptor.capture());
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    void testReStartEveryInstancesStarted() {
        mockDescribeInstancesAllisRebooted(8);
        List<CloudVmInstanceStatus> result = underTest.restartWithLimitedRetry(authenticatedContext, new ArrayList<>(),
                inputList, 600_000L, List.of(InstanceStatus.STOPPED, InstanceStatus.ZOMBIE, InstanceStatus.TERMINATED,
                        InstanceStatus.TERMINATED_BY_PROVIDER, InstanceStatus.DELETE_REQUESTED));

        verify(amazonEC2Client, times(1)).stopInstances(any(StopInstancesRequest.class));
        verify(amazonEC2Client, times(1)).startInstances(any(StartInstancesRequest.class));
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    void testReStartWithEveryInstanceTerminated() {
        mockDescribeInstancesNoneRestarted(8);
        List<CloudVmInstanceStatus> result = underTest.restartWithLimitedRetry(authenticatedContext, new ArrayList<>(),
                inputList, 600_000L, List.of(InstanceStatus.STOPPED, InstanceStatus.ZOMBIE, InstanceStatus.TERMINATED,
                        InstanceStatus.TERMINATED_BY_PROVIDER, InstanceStatus.DELETE_REQUESTED));

        verify(amazonEC2Client, times(0)).stopInstances(any(StopInstancesRequest.class));
        verify(amazonEC2Client, times(0)).startInstances(any(StartInstancesRequest.class));
    }

    @Test
    void testStartEveryInstancesStartedAlready() {
        mockDescribeInstancesAllIsRunning(POLLING_LIMIT - 2);
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, never()).startInstances(any(StartInstancesRequest.class));
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    void testStartPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Running", 16);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        assertThrows(PollerStoppedException.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        assertEquals(inputList.size(), captor.getValue().instanceIds().size());
    }

    @Test
    void testStopPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Stopped", 41);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        assertThrows(PollerStoppedException.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        assertEquals(inputList.size(), captor.getValue().instanceIds().size());
    }

    @Test
    void testStartException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        assertThrows(Ec2Exception.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        assertThat(inputList, hasSize(2));
    }

    @Test
    void testStopException() {
        mockDescribeInstancesException("silence of the lambs", "would you ...");
        assertThrows(Ec2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        assertThat(inputList, hasSize(2));
    }

    @Test
    void testStartExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        assertThrows(Ec2Exception.class, () -> underTest.start(authenticatedContext, List.of(), mutableList));
        assertThat(mutableList, hasSize(1));
    }

    @Test
    void testCheckInsufficientCapacityExceptionHandle() {
        mockDescribeInstancesException(INSUFFICIENT_INSTANCE_CAPACITY, "Insufficient capacity on i-432");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        assertThatThrownBy(() -> underTest.startWithLimitedRetry(authenticatedContext, List.of(), mutableList, null))
                .isInstanceOf(InsufficientCapacityException.class);
        assertThat(mutableList, hasSize(2));
    }

    @Test
    void testStopExceptionHandle() {
        mockDescribeInstancesException(INSTANCE_NOT_FOUND, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        assertThrows(Ec2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), mutableList));
        assertThat(mutableList, hasSize(1));
    }

    @Test
    void testStartSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(
                        SdkClientException.builder().message("lamb").build(),
                        SdkClientException.builder().message("sheep").build())
                .thenReturn(getDescribeInstancesResult("running", 16));
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(4)).describeInstances(any(DescribeInstancesRequest.class));
        assertThat(result, hasSize(2));
    }

    @Test
    void testStopSdkExceptionRetry() {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(
                        SdkClientException.builder().message("lamb").build(),
                        SdkClientException.builder().message("sheep").build())
                .thenReturn(getDescribeInstancesResult("stopped", 55));
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(4)).describeInstances(any(DescribeInstancesRequest.class));
        assertThat(result, hasSize(2));
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
        mockListOfDescribeInstancesRunningThenStoppedThenRunning(getDescribeInstancesResultOneRunning("running", 16), pollResponses,
                getDescribeInstancesResult("stopped", 16));
    }

    private void mockDescribeInstancesNoneRestarted(int pollResponses) {
        mockListOfDescribeInstancesTerminated(getDescribeInstanceResultBothTerminated(), pollResponses, getDescribeInstanceResultBothTerminated());
    }

    private void mockListOfDescribeInstancesRunningThenStoppedThenRunning(DescribeInstancesResponse cons, int repeatNo, DescribeInstancesResponse stopped) {
        DescribeInstancesResponse[] describeInstancesResults = new DescribeInstancesResponse[repeatNo];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[3] = stopped;
        describeInstancesResults[4] = stopped;
        describeInstancesResults[5] = stopped;
        describeInstancesResults[6] = stopped;
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockListOfDescribeInstancesTerminated(DescribeInstancesResponse initial, int repeatNo, DescribeInstancesResponse after) {
        DescribeInstancesResponse[] describeInstancesResults = new DescribeInstancesResponse[repeatNo];
        Arrays.fill(describeInstancesResults, initial);
        describeInstancesResults[3] = after;
        describeInstancesResults[4] = after;
        describeInstancesResults[5] = after;
        describeInstancesResults[6] = after;
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(initial,
                describeInstancesResults);
    }

    private void mockListOfDescribeInstances(DescribeInstancesResponse cons, int repeatNo, DescribeInstancesResponse last) {
        DescribeInstancesResponse[] describeInstancesResults = new DescribeInstancesResponse[repeatNo];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[repeatNo - 1] = last;
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockDescribeInstancesException(String errorCode, String errorMessage) {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).then(invocation -> {
            Ec2Exception exception = (Ec2Exception) Ec2Exception.builder()
                    .message("Sheep lost control")
                    .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).errorMessage(errorMessage).build())
                    .build();
            throw exception;
        });
    }

    private void testCheckStates(String running, int code, InstanceStatus status) {
        List<CloudInstance> list = getCloudInstances();
        DescribeInstancesResponse instancesResult = getDescribeInstancesResult(running, code);
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(instancesResult);
        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, list);
        assertThat(result, hasSize(2));
        assertThat(result, everyItem(hasProperty("status", is(status))));
    }

    private DescribeInstancesResponse getDescribeInstancesResult(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", state, code);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return DescribeInstancesResponse.builder().reservations(reservation1, reservation2).build();
    }

    private DescribeInstancesResponse getDescribeInstancesResultOneRunning(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", "running", 16);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return DescribeInstancesResponse.builder().reservations(reservation1, reservation2).build();
    }

    private DescribeInstancesResponse getDescribeInstanceResultBothTerminated() {
        Instance instances1 = getAwsInstance("i-1", "terminated", 48);
        Instance instances2 = getAwsInstance("i-2", "terminated", 48);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return DescribeInstancesResponse.builder().reservations(reservation1, reservation2).build();
    }

    private DescribeInstancesResponse getDescribeInstancesResultOneStopped(String state, int code) {
        Instance instances1 = getAwsInstance("i-1", state, code);
        Instance instances2 = getAwsInstance("i-2", "stopped", 16);
        Reservation reservation1 = getReservation(instances1, "1");
        Reservation reservation2 = getReservation(instances2, "2");

        return DescribeInstancesResponse.builder().reservations(reservation1, reservation2).build();
    }

    private List<CloudInstance> getCloudInstances() {
        CloudInstance instance1 = new CloudInstance("i-1", null, null, "subnet-123", "az1");
        CloudInstance instance2 = new CloudInstance("i-2", null, null, "subnet-123", "az1");
        return List.of(instance1, instance2);
    }

    private List<CloudInstance> getLargeCountCloudInstances() {
        List<CloudInstance> result = new ArrayList<>();
        for (int i = 1; i < 101; i++) {
            InstanceTemplate instanceTemplate = new InstanceTemplate(null, "large", null, null, null, null, null, null, null, null);
            result.add(new CloudInstance("large" + i, instanceTemplate, null, "subnet-123", "az1"));
        }
        for (int i = 1; i < 3; i++) {
            InstanceTemplate instanceTemplate = new InstanceTemplate(null, "small1", null, null, null, null, null, null, null, null);
            result.add(new CloudInstance("small1" + i, instanceTemplate, null, "subnet-123", "az1"));
        }
        for (int i = 1; i < 3; i++) {
            InstanceTemplate instanceTemplate = new InstanceTemplate(null, "small2", null, null, null, null, null, null, null, null);
            result.add(new CloudInstance("small2" + i, instanceTemplate, null, "subnet-123", "az1"));
        }
        return result;
    }

    private Reservation getReservation(Instance instances, String reservationId) {
        return Reservation.builder().reservationId(reservationId).instances(instances).build();
    }

    private Instance getAwsInstance(String instanceId, String state, int code) {
        return Instance.builder().state(InstanceState.builder().name(state).code(code).build()).instanceId(instanceId).build();
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
            RetryService.class,
            AwsRegionEndpointProvider.class,
            AwsServiceEndpointProvider.class
    })
    static class Config {
    }

    interface TestCall {
        List<CloudVmInstanceStatus> testCall(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms);
    }
}
