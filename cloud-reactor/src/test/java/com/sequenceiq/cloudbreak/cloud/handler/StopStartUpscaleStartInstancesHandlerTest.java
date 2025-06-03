package com.sequenceiq.cloudbreak.cloud.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
public class StopStartUpscaleStartInstancesHandlerTest {

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StopStartUpscaleStartInstancesHandler underTest;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private InstanceConnector instanceConnector;

    @BeforeEach
    public void setUp() {
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);

        reset(cloudContext);
        reset(cloudPlatformConnectors);
        reset(cloudConnector);

        lenient().when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        lenient().when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(authenticator.authenticate(any(CloudContext.class), any(CloudCredential.class))).thenReturn(ac);
        lenient().when(cloudConnector.instances()).thenReturn(instanceConnector);
    }

    @Test
    void testType() {
        assertEquals(StopStartUpscaleStartInstancesRequest.class, underTest.type());
    }

    @Test
    void testNotEnoughInstancesAvailableToStartTest1() {
        // Cloud provider instance status matches CB instance status, and fewer instances available to
        // START compared to what was requested
        testNotEnoughInstancesAvailableToStartTestInternal(3, 3, 5);
    }

    @Test
    void testNotEnoughInstancesAvailableToStartCBCloudMismatchTest1() {
        // Cloud provider has more instances in STOPPED state as compared to CB state. Not enough
        // to fullfill the original request.
        testNotEnoughInstancesAvailableToStartTestInternal(3, 4, 5);
    }

    @Test
    void testNotEnoughInstancesAvailableToStartInCBButEnoughInCloudTest1() {
        // Cloud provider has more instances in STOPPED state as compared to CB state. Just enough to satisfy request.
        testNotEnoughInstancesAvailableToStartTestInternal(3, 5, 5);
    }

    @Test
    void testNotEnoughInstancesAvailableToStartInCBButEnoughInCloudTest2() {
        // Cloud provider has more instances in STOPPED state as compared to CB state. More STOPPED
        // instances compared to start list.
        testNotEnoughInstancesAvailableToStartTestInternal(3, 6, 5);
    }

    @Test
    void testMoreStoppedInstancesInCBComparedToCloudTest1() {
        // CB does not have enough instances to START, but more than what the cloud provider thinks as STOPPED.
        testNotEnoughInstancesAvailableToStartTestInternal(3, 2, 5);
    }

    @Test
    void testAdequateStoppedInstancesInCbTest1() {
        // CB has just enough instances to START
        // [Note: but more than what the cloud provider thinks as STOPPED.
        testCbHasAdequateInstancesToStartInternal(5, 5);
    }

    @Test
    void testAdequateStoppedInstancesInCbTest2() {
        // CB has more than enough instances to START,
        // [Note: but more than what the cloud provider thinks as STOPPED
        // The cloud-provider aspect is irrelevant at the moment, given this scenario does not trigger a cloud-provider listing.]
        testCbHasAdequateInstancesToStartInternal(7, 5);
    }

    @Test
    void testCloudProviderInstancesNotInStarterOrStoppedStateTest1() {
        // Cloud provider thinks some of the instances are not in STARTED or STOPPED state - TERMINATED, etc.
        // Adequate instances available to START
        testCloudProviderInstancesInTerminatedEtcStateInternal(3, 5, 5);
    }

    @Test
    void testCloudProviderInstancesNotInStarterOrStoppedStateTest2() {
        // Cloud provider thinks some of the instances are not in STARTED or STOPPED state - TERMINATED, etc.
        // Not enough instances available to START
        testCloudProviderInstancesInTerminatedEtcStateInternal(3, 2, 5);
    }

    @Test
    void testCloudProviderStartEncountersInstancesInOtherStatesTest1() {
        // During the cloud-provider start, some instances may be in different states. (Assuming certain behaviour from the API here,
        //  which neesd to be validated.
        // With CB having just enough instances in STOPPED state.
        testCloudProviderInstancesInTerminatedEtcStateDuringStartInternal(5, 5);
    }

    @Test
    void testCloudProviderStartEncountersInstancesInOtherStatesTest2() {
        // During the cloud-provider start, some instances may be in different states. (Assuming certain behaviour from the API here,
        //  which neesd to be validated.
        // With CB having more than enough instances in STOPPED state
        testCloudProviderInstancesInTerminatedEtcStateDuringStartInternal(6, 5);
    }

    @Test
    void testNegativeInstancesToStart() {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(5);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int numInstancesToStart = -1;

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verifyNoMoreInteractions(instanceConnector);

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();
        assertEquals(0, result.getAffectedInstanceStatuses().size());
    }

    @Test
    void testZeroInstancesToStart() {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(5);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int numInstancesToStart = 0;

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verifyNoMoreInteractions(instanceConnector);

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();
        assertEquals(0, result.getAffectedInstanceStatuses().size());
    }

    @Test
    void testFailureFromCloudProviderWhenStartingInstances() {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(5);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;
        int numInstancesToStart = 5;

        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(stoppedInstancesInHg);
        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(stoppedInstancesInHg);

        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class),
                eq(stoppedInstancesInHg)))
                .thenReturn(stoppedInstanceStatusList).thenReturn(startedInstanceStatusList);
        when(instanceConnector.
                startWithLimitedRetry(
                        any(AuthenticatedContext.class), eq(null), any(List.class), any(Long.class)))
                .thenThrow(new RuntimeException("CloudProviderStartError"));


        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);

        underTest.accept(event);
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        assertNull(result.getErrorDetails());
        assertEquals(numInstancesToStart, result.getAffectedInstanceStatuses().size());
        assertEquals(startedInstanceStatusList, result.getAffectedInstanceStatuses());
        assertEquals(EventStatus.OK, result.getStatus());
        assertEquals("STOPSTARTUPSCALESTARTINSTANCESRESULT", result.selector());
    }

    @Test
    void testRetryWhenInsufficientCapacityExceptionIsThrown() {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(5);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;
        int numInstancesToStart = 5;

        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(stoppedInstancesInHg);
        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(stoppedInstancesInHg);

        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class),
                eq(stoppedInstancesInHg)))
                .thenReturn(stoppedInstanceStatusList).thenReturn(startedInstanceStatusList);
        when(instanceConnector.startWithLimitedRetry(
                any(AuthenticatedContext.class),
                eq(null), anyList(), anyLong()))
                .thenThrow(new InsufficientCapacityException("exception1"), new InsufficientCapacityException("exception2"),
                        new InsufficientCapacityException("exception3"))
                .thenReturn(startedInstanceStatusList);

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);

        underTest.accept(event);
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verify(instanceConnector, times(2)).startWithLimitedRetry(any(AuthenticatedContext.class), eq(null), anyList(), anyLong());
    }

    @Test
    void testUnableToCollectInstancesFromCloudPovider() {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(3);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;
        int numInstancesToStart = 5;

        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class), any(List.class)))
                .thenThrow(new RuntimeException("CloudProviderCheckStateError"));

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);

        underTest.accept(event);
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        assertEquals("CloudProviderCheckStateError", result.getErrorDetails().getMessage());
        assertEquals(0, result.getAffectedInstanceStatuses().size());
        assertEquals(EventStatus.FAILED, result.getStatus());
        assertEquals("STOPSTARTUPSCALESTARTINSTANCESRESULT_ERROR", result.selector());
    }

    @Test
    void testNegativeInstancesToStartIncludingInstancesWithServicesNotRunning() {
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(3);
        List<CloudInstance> startedInstancesWithServicesNotRunning = generateCloudInstances(5);
        int numInstancesToStart = 2;

        StopStartUpscaleStartInstancesRequest request = new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        Event event = new Event(request);

        underTest.accept(event);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), captor.capture());
        verifyNoMoreInteractions(instanceConnector);

        assertEquals(1, captor.getAllValues().size());
        Event resultEvent = captor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        assertThat(result.getAffectedInstanceStatuses()).isEmpty();
    }

    @Test
    void testStartInstancesWhereLessInstancesAvailableOnCloudProvider() {
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(6);
        List<CloudInstance> startedInstancesWithServicesNotRunning = generateCloudInstances(0);
        int numInstancesToStart = 5;

        StopStartUpscaleStartInstancesRequest request = new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        int expectedInstances = 4;
        List<CloudInstance> stoppedInstancesArg = stoppedInstancesInHg.subList(0, 4);
        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(stoppedInstancesArg);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(stoppedInstancesArg);

        when(instanceConnector.checkWithoutRetry(any(AuthenticatedContext.class), eq(stoppedInstancesInHg))).thenReturn(stoppedInstanceStatusList);
        when(instanceConnector.startWithLimitedRetry(any(AuthenticatedContext.class), eq(null), any(), anyLong()))
                .thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), eq(stoppedInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(stoppedInstancesArg), anyLong());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult(startedInstanceStatusList, result.getAffectedInstanceStatuses());
        assertEquals(expectedInstances, result.getAffectedInstanceStatuses().size());
    }

    @Test
    void testStartInstancesIncludingInstancesWithServicesNotRunning() {
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(6);
        List<CloudInstance> startedInstancesWithServicesNotRunning = generateCloudInstances(3);
        int numInstancesToStart = 5;

        StopStartUpscaleStartInstancesRequest request = new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        int expectedInstances = Math.min(stoppedInstancesInHg.size(), numInstancesToStart - startedInstancesWithServicesNotRunning.size());
        List<CloudInstance> stoppedInstancesArg = stoppedInstancesInHg.subList(0, expectedInstances);
        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(stoppedInstancesArg);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(stoppedInstancesInHg);
        when(instanceConnector.checkWithoutRetry(any(AuthenticatedContext.class), eq(stoppedInstancesInHg))).thenReturn(stoppedInstanceStatusList);
        when(instanceConnector.startWithLimitedRetry(any(AuthenticatedContext.class), eq(null), any(), anyLong()))
                .thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), eq(stoppedInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(stoppedInstancesArg), anyLong());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult(startedInstanceStatusList, result.getAffectedInstanceStatuses());
        assertEquals(numInstancesToStart - startedInstancesWithServicesNotRunning.size(), result.getAffectedInstanceStatuses().size());
    }

    private void testNotEnoughInstancesAvailableToStartTestInternal(
            int cbStoppedInstanceCount, int cloudProviderStoppedInstanceCount, int numInstancesToStart) {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(cbStoppedInstanceCount);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int expectedInstances = Math.min(cloudProviderStoppedInstanceCount, numInstancesToStart);

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = generateCloudVmInstanceStatuses(allInstancesInHg, cloudProviderStoppedInstanceCount);
        when(instanceConnector.checkWithoutRetry(any(AuthenticatedContext.class), eq(allInstancesInHg))).thenReturn(cloudVmInstanceStatusList);

        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(allInstancesInHg.subList(0, expectedInstances));
        when(instanceConnector.
                startWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(allInstancesInHg.subList(0, expectedInstances)),
                        any(Long.class))).thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), eq(allInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(allInstancesInHg.subList(0, expectedInstances)), any(Long.class));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult(cloudVmInstanceStatusList.subList(0, expectedInstances), result.getAffectedInstanceStatuses());
    }

    private void testCbHasAdequateInstancesToStartInternal(int cbStoppedInstanceCount, int numInstancesToStart) {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(cbStoppedInstanceCount);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int expectedInstances = Math.min(cbStoppedInstanceCount, numInstancesToStart);

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        List<CloudInstance> stoppedInstancesArg = stoppedInstancesInHg.subList(0, expectedInstances);
        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(stoppedInstancesArg);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(stoppedInstancesArg);
        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class),
                eq(stoppedInstancesInHg)))
                .thenReturn(stoppedInstanceStatusList);
        when(instanceConnector.
                startWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(stoppedInstancesArg),
                        any(Long.class))).thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector, never()).checkWithoutRetry(any(AuthenticatedContext.class), eq(allInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(stoppedInstancesArg), any(Long.class));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult(startedInstanceStatusList, result.getAffectedInstanceStatuses());
    }

    private void testCloudProviderInstancesInTerminatedEtcStateInternal(
            int cbStoppedInstanceCount, int cloudProviderStoppedInstanceCount, int numInstancesToStart) {
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(cbStoppedInstanceCount);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int expectedInstances = Math.min(cloudProviderStoppedInstanceCount, numInstancesToStart);

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        List<CloudVmInstanceStatus> cloudVmInstanceStatusList =
                generateCloudVmInstanceStatusesIncludingOtherStates(allInstancesInHg, cloudProviderStoppedInstanceCount);
        when(instanceConnector.checkWithoutRetry(any(AuthenticatedContext.class), eq(allInstancesInHg))).thenReturn(cloudVmInstanceStatusList);

        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(cloudVmInstanceStatusList, expectedInstances);
        List<CloudInstance> expInvocationList = startedInstanceStatusList.stream().map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
        when(instanceConnector.
                startWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(expInvocationList),
                        any(Long.class))).thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), eq(allInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(any(AuthenticatedContext.class), eq(null), eq(expInvocationList), any(Long.class));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult(startedInstanceStatusList, result.getAffectedInstanceStatuses());
    }

    private void testCloudProviderInstancesInTerminatedEtcStateDuringStartInternal(int cbStoppedInstanceCount, int numInstancesToStart) {
        // This assumes behaviour on the CloudConnector API to ignore certain TERMINAL states, and provide information back on the
        // instances with the state set. If that is not the behaviour (to be verified manually) - this test is pointless.
        List<CloudInstance> stoppedInstancesInHg = generateCloudInstances(cbStoppedInstanceCount);
        List<CloudInstance> allInstancesInHg = generateCloudInstances(10);
        List<CloudInstance> startedInstancesWithServicesNotRunning = null;

        int expectedInstances = Math.min(cbStoppedInstanceCount, numInstancesToStart);

        StopStartUpscaleStartInstancesRequest request =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack,
                        "compute", stoppedInstancesInHg, allInstancesInHg, startedInstancesWithServicesNotRunning, numInstancesToStart);

        List<CloudInstance> stoppedInstancesArg = stoppedInstancesInHg.subList(0, expectedInstances);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList =
                generateStoppedCloudVmInstanceStatuses(stoppedInstancesInHg);
        List<CloudVmInstanceStatus> startedInstanceStatusList =
                generateStartedCloudVmInstanceStatusesIncludingOtherStates(stoppedInstancesArg);
        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class),
                eq(stoppedInstancesInHg)))
                .thenReturn(stoppedInstanceStatusList);
        when(instanceConnector.
                startWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(stoppedInstancesArg),
                        any(Long.class))).thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), eq(stoppedInstancesInHg));
        verify(instanceConnector).startWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(stoppedInstancesInHg.subList(0, expectedInstances)), any(Long.class));
        verifyNoMoreInteractions(instanceConnector);

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartUpscaleStartInstancesResult.class, resultEvent.getData().getClass());
        StopStartUpscaleStartInstancesResult result = (StopStartUpscaleStartInstancesResult) resultEvent.getData();

        verifyAffectedInstancesInResult3(stoppedInstancesInHg.subList(0, expectedInstances), result.getAffectedInstanceStatuses());
    }

    private List<CloudInstance> generateCloudInstances(int numInstances) {
        List<CloudInstance> instances = new LinkedList<>();

        for (int i = 0; i < numInstances; i++) {
            CloudInstance cloudInstance = mock(CloudInstance.class);
            lenient().when(cloudInstance.getInstanceId()).thenReturn(MOCK_INSTANCEID_PREFIX + i);
            instances.add(cloudInstance);
        }
        return instances;
    }

    private List<CloudVmInstanceStatus> generateCloudVmInstanceStatuses(List<CloudInstance> cloudInstances, int numStopped) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();

        int count = 0;
        for (CloudInstance cloudInstance : cloudInstances) {
            CloudVmInstanceStatus cloudVmInstanceStatus;
            if (count++ < numStopped) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED);
            } else {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED);
            }
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateCloudVmInstanceStatusesIncludingOtherStates(List<CloudInstance> cloudInstances, int numStopped) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();

        int count = 0;
        int stopGenCount = 0;
        for (CloudInstance cloudInstance : cloudInstances) {
            CloudVmInstanceStatus cloudVmInstanceStatus;
            if (count == 0) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED);
            } else if (count == 1) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED_BY_PROVIDER);
            } else if (count == 2) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.DELETE_REQUESTED);
            } else {
                if (stopGenCount++ < numStopped) {
                    cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED);
                } else {
                    cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED);
                }
            }

            count++;
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateStartedCloudVmInstanceStatusesIncludingOtherStates(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();

        int count = 0;
        for (CloudInstance cloudInstance : cloudInstances) {
            CloudVmInstanceStatus cloudVmInstanceStatus;
            if (count == 0) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED);
            } else if (count == 1) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED_BY_PROVIDER);
            } else if (count == 2) {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.DELETE_REQUESTED);
            } else {
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED);
            }

            count++;
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateStartedCloudVmInstanceStatuses(List<CloudInstance> cloudInstances) {
        return generateCloudVmInstances(cloudInstances, InstanceStatus.STARTED);
    }

    private List<CloudVmInstanceStatus> generateStoppedCloudVmInstanceStatuses(List<CloudInstance> cloudInstances) {
        return generateCloudVmInstances(cloudInstances, InstanceStatus.STOPPED);
    }

    private List<CloudVmInstanceStatus> generateCloudVmInstances(List<CloudInstance> cloudInstances, InstanceStatus status) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, status));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateStartedCloudVmInstanceStatuses(List<CloudVmInstanceStatus> cloudInstances, int expectedStartCount) {
        List<CloudVmInstanceStatus> startedCloudVmInstanceStatusList = new LinkedList<>();
        int genCount = 0;
        for (CloudVmInstanceStatus cloudVmInstanceStatus : cloudInstances) {
            if (genCount == expectedStartCount) {
                break;
            }
            if (InstanceStatus.STOPPED == cloudVmInstanceStatus.getStatus()) {
                startedCloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudVmInstanceStatus.getCloudInstance(), InstanceStatus.STARTED));
                genCount++;
            }
        }
        return startedCloudVmInstanceStatusList;
    }

    private void verifyAffectedInstancesInResult(List<CloudVmInstanceStatus> srcList, List<CloudVmInstanceStatus> listFromResult) {
        assertEquals(srcList.size(), listFromResult.size());
        for (int i = 0; i < srcList.size(); i++) {
            assertEquals(srcList.get(i).getCloudInstance().getInstanceId(), listFromResult.get(i).getCloudInstance().getInstanceId());
            assertEquals(InstanceStatus.STARTED, listFromResult.get(i).getStatus());
        }
    }

    private void verifyAffectedInstancesInResult2(List<CloudInstance> srcList, List<CloudVmInstanceStatus> listFromResult) {
        assertEquals(srcList.size(), listFromResult.size());
        for (int i = 0; i < srcList.size(); i++) {
            assertEquals(srcList.get(i).getInstanceId(), listFromResult.get(i).getCloudInstance().getInstanceId());
            assertEquals(InstanceStatus.STARTED, listFromResult.get(i).getStatus());
        }
    }

    private void verifyAffectedInstancesInResult3(List<CloudInstance> srcList, List<CloudVmInstanceStatus> listFromResult) {
        assertEquals(srcList.size(), listFromResult.size());
        int count = 0;
        for (CloudInstance cloudInstance : srcList) {
            assertEquals(srcList.get(count).getInstanceId(), listFromResult.get(count).getCloudInstance().getInstanceId());
            CloudVmInstanceStatus cloudVmInstanceStatus;
            if (count == 0) {
                assertEquals(InstanceStatus.TERMINATED, listFromResult.get(count).getStatus());
            } else if (count == 1) {
                assertEquals(InstanceStatus.TERMINATED_BY_PROVIDER, listFromResult.get(count).getStatus());
            } else if (count == 2) {
                assertEquals(InstanceStatus.DELETE_REQUESTED, listFromResult.get(count).getStatus());
            } else {
                assertEquals(InstanceStatus.STARTED, listFromResult.get(count).getStatus());
            }

            count++;
        }
    }
}
