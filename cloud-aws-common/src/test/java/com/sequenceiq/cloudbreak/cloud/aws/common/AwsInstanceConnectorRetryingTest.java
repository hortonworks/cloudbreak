package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static org.hamcrest.Matchers.allOf;
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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
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
public class AwsInstanceConnectorRetryingTest {

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
                Map.of("accessKey", "ac", "secretKey", "secret"), "acc", false);
        authenticatedContext = awsAuthenticator.authenticate(context, credential);

        StopInstancesResult stopInstancesResult = new StopInstancesResult();
        StartInstancesResult startInstanceResult = new StartInstancesResult();
        when(amazonEC2Client.stopInstances(any(StopInstancesRequest.class))).thenReturn(stopInstancesResult);
        when(amazonEC2Client.startInstances(any(StartInstancesRequest.class))).thenReturn(startInstanceResult);

        inputList = getCloudInstances();
    }

    @Test
    public void testCheckException() {
        mockDescribeInstanceStatusesException("silence of the lambs", "would you ...");
        List<CloudInstance> list = getCloudInstances();
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.check(authenticatedContext, list));
        MatcherAssert.assertThat(list, hasSize(2));
    }

    @Test
    public void testCheckRemovesUnknownInstancesBasedOnErrorMessage() {
        mockDescribeInstanceStatusesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.check(authenticatedContext, mutableList));
        MatcherAssert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testCheckSdkExceptionRetry() {
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class)))
                .thenThrow(new SdkClientException("lamb"),
                        new SdkClientException("sheep"),
                        new SdkClientException("shepherd"))
                .thenReturn(getDescribeInstanceStatusesResult("running", 16));
        List<CloudInstance> list = getCloudInstances();
        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, list);
        verify(amazonEC2Client, times(4)).describeInstanceStatuses(any(DescribeInstanceStatusRequest.class));
        MatcherAssert.assertThat(result, hasSize(2));
    }

    @Test
    public void testStartPollingWithSuccess() {
        String status = "Running";
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 2, status, lastStatusCode);
        ArgumentCaptor<StartInstancesRequest> captorStart = ArgumentCaptor.forClass(StartInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).startInstances(captorStart.capture());
        Assertions.assertEquals(captorStart.getValue().getInstanceIds().size(), inputList.size());
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStopPollingWithSuccess() {
        String status = "Stopped";
        int lastStatusCode = 16;
        //given
        mockDescribeInstances(POLLING_LIMIT - 2, status, lastStatusCode);
        ArgumentCaptor<StopInstancesRequest> captorStop = ArgumentCaptor.forClass(StopInstancesRequest.class);

        //then
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);

        //then
        verify(amazonEC2Client, times(1)).stopInstances(captorStop.capture());
        Assertions.assertEquals(captorStop.getValue().getInstanceIds().size(), inputList.size());
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STOPPED)))));
    }

    @Test
    public void testStartSomeInstancesStarted() {
        mockDescribeInstancesOneIsRunningLastSuccess(POLLING_LIMIT - 2);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() < inputList.size());
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStopSomeInstancesStopped() {
        mockDescribeInstancesOneIsStoppedLastSuccess(POLLING_LIMIT - 2);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        Assertions.assertTrue(captor.getValue().getInstanceIds().size() < inputList.size());
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STOPPED)))));
    }

    @Test
    public void testRebootEveryInstancesStarted() {
        mockDescribeInstancesAllisRebooted(POLLING_LIMIT - 2);
        ArgumentCaptor<StopInstancesRequest> stopCaptor = ArgumentCaptor.forClass(StopInstancesRequest.class);
        ArgumentCaptor<StartInstancesRequest> startCaptor = ArgumentCaptor.forClass(StartInstancesRequest.class);
        List<CloudVmInstanceStatus> result = underTest.reboot(authenticatedContext, new ArrayList<>(), inputList);

        verify(amazonEC2Client, times(2)).stopInstances(stopCaptor.capture());
        verify(amazonEC2Client, times(2)).startInstances(startCaptor.capture());
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStartEveryInstancesStartedAlready() {
        mockDescribeInstancesAllIsRunning(POLLING_LIMIT - 2);
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, never()).startInstances(any(StartInstancesRequest.class));
        MatcherAssert.assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testStartPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Running", 16);
        ArgumentCaptor<StartInstancesRequest> captor = ArgumentCaptor.forClass(StartInstancesRequest.class);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).startInstances(captor.capture());
        Assertions.assertEquals(captor.getValue().getInstanceIds().size(), inputList.size());
    }

    @Test
    public void testStopPollingWithFail() {
        mockDescribeInstances(POLLING_LIMIT + 2, "Stopped", 41);
        ArgumentCaptor<StopInstancesRequest> captor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        verify(amazonEC2Client, times(1)).stopInstances(captor.capture());
        Assertions.assertEquals(captor.getValue().getInstanceIds().size(), inputList.size());
    }

    @Test
    public void testStartException() {
        mockDescribeInstanceStatusesException("silence of the lambs", "would you ...");
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.start(authenticatedContext, List.of(), inputList));
        MatcherAssert.assertThat(inputList, hasSize(2));
    }

    @Test
    public void testStopException() {
        mockDescribeInstanceStatusesException("silence of the lambs", "would you ...");
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), inputList));
        MatcherAssert.assertThat(inputList, hasSize(2));
    }

    @Test
    public void testStartExceptionHandle() {
        mockDescribeInstanceStatusesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.start(authenticatedContext, List.of(), mutableList));
        MatcherAssert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testStopExceptionHandle() {
        mockDescribeInstanceStatusesException(INSTANCE_NOT_FOUND_ERROR_CODE, "i-1 is a sheep!");
        List<CloudInstance> mutableList = new ArrayList<>(getCloudInstances());
        Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.stop(authenticatedContext, List.of(), mutableList));
        MatcherAssert.assertThat(mutableList, hasSize(1));
    }

    @Test
    public void testStartSdkExceptionRetry() {
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class)))
                .thenThrow(
                        new SdkClientException("lamb"),
                        new SdkClientException("sheep"),
                        new SdkClientException("shepherd"))
                .thenReturn(getDescribeInstanceStatusesResult("running", 16));
        List<CloudVmInstanceStatus> result = underTest.start(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(5)).describeInstanceStatuses(any(DescribeInstanceStatusRequest.class));
        MatcherAssert.assertThat(result, hasSize(2));
    }

    @Test
    public void testStopSdkExceptionRetry() {
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class)))
                .thenThrow(new SdkClientException("lamb"),
                        new SdkClientException("sheep"),
                        new SdkClientException("shepherd"))
                .thenReturn(getDescribeInstanceStatusesResult("stopped", 55));
        List<CloudVmInstanceStatus> result = underTest.stop(authenticatedContext, List.of(), inputList);
        verify(amazonEC2Client, times(5)).describeInstanceStatuses(any(DescribeInstanceStatusRequest.class));
        MatcherAssert.assertThat(result, hasSize(2));
    }

    private void mockDescribeInstances(int pollResponses, String lastStatus, int lastStatusCode) {
        mockListOfDescribeInstances(getDescribeInstanceStatusesResult("notrunning", 16), pollResponses,
                getDescribeInstanceStatusesResult(lastStatus, lastStatusCode));
    }

    private void mockDescribeInstancesOneIsRunningLastSuccess(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstancesResultOneRunning("notrunning", 16), pollResponses,
                getDescribeInstanceStatusesResult("running", 16));
    }

    private void mockDescribeInstancesOneIsStoppedLastSuccess(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstancesResultOneStopped("notrunning", 16), pollResponses,
                getDescribeInstanceStatusesResult("stopped", 16));
    }

    private void mockDescribeInstancesAllIsRunning(int pollResponses) {
        mockListOfDescribeInstances(getDescribeInstanceStatusesResult("running", 16), pollResponses,
                getDescribeInstanceStatusesResult("running", 16));
    }

    private void mockDescribeInstancesAllisRebooted(int pollResponses) {
        mockListOfDescribeInstancesStopAndThenRunning(getDescribeInstancesResultOneRunning("running", 16), pollResponses,
                getDescribeInstanceStatusesResult("stopped", 16));
    }

    private void mockListOfDescribeInstancesStopAndThenRunning(DescribeInstanceStatusResult cons, int repeatNo, DescribeInstanceStatusResult stopped) {
        DescribeInstanceStatusResult[] describeInstancesResults = new DescribeInstanceStatusResult[repeatNo * 2];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[2] = stopped;
        describeInstancesResults[4] = stopped;
        describeInstancesResults[5] = stopped;
        describeInstancesResults[6] = stopped;
        describeInstancesResults[8] = stopped;
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockListOfDescribeInstances(DescribeInstanceStatusResult cons, int repeatNo, DescribeInstanceStatusResult last) {
        DescribeInstanceStatusResult[] describeInstancesResults = new DescribeInstanceStatusResult[repeatNo];
        Arrays.fill(describeInstancesResults, cons);
        describeInstancesResults[repeatNo - 1] = last;
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class))).thenReturn(cons,
                describeInstancesResults);
    }

    private void mockDescribeInstanceStatusesException(String errorCode, String errorMessage) {
        when(amazonEC2Client.describeInstanceStatuses(any(DescribeInstanceStatusRequest.class))).then(invocation -> {
            AmazonEC2Exception exception = new AmazonEC2Exception("Sheep lost control");
            exception.setErrorCode(errorCode);
            exception.setErrorMessage(errorMessage);
            throw exception;
        });
    }

    private DescribeInstanceStatusResult getDescribeInstanceStatusesResult(String state, int code) {
        return new DescribeInstanceStatusResult().withInstanceStatuses(
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-1")
                        .withInstanceState(new InstanceState().withName(state).withCode(code)),
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-2")
                        .withInstanceState(new InstanceState().withName(state).withCode(code)));
    }

    private DescribeInstanceStatusResult getDescribeInstancesResultOneRunning(String state, int code) {
        return new DescribeInstanceStatusResult().withInstanceStatuses(
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-1")
                        .withInstanceState(new InstanceState().withName(state).withCode(code)),
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-2")
                        .withInstanceState(new InstanceState().withName("running").withCode(16)));
    }

    private DescribeInstanceStatusResult getDescribeInstancesResultOneStopped(String state, int code) {
        return new DescribeInstanceStatusResult().withInstanceStatuses(
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-1")
                        .withInstanceState(new InstanceState().withName(state).withCode(code)),
                new com.amazonaws.services.ec2.model.InstanceStatus()
                        .withInstanceId("i-2")
                        .withInstanceState(new InstanceState().withName("stopped").withCode(16)));
    }

    private List<CloudInstance> getCloudInstances() {
        CloudInstance instance1 = new CloudInstance("i-1", null, null, "subnet-123", "az1");
        CloudInstance instance2 = new CloudInstance("i-2", null, null, "subnet-123", "az1");
        return List.of(instance1, instance2);
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
}
