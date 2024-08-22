package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
class RestartInstanceHandlerTest {

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final List<InstanceStatus> EXCLUDED_STATUSES = List.of(
            InstanceStatus.STOPPED,
            InstanceStatus.ZOMBIE,
            InstanceStatus.TERMINATED,
            InstanceStatus.TERMINATED_BY_PROVIDER,
            InstanceStatus.DELETE_REQUESTED
    );

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RestartInstanceHandler underTest;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

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
        assertEquals(RestartInstancesRequest.class, underTest.type());
    }

    @Test
    void testInstanceRestartWithoutError() {
        List<CloudInstance> instancesToRestart = generateCloudInstances(10);
        RestartInstancesRequest request = new RestartInstancesRequest(cloudContext, cloudCredential, null, instancesToRestart);

        List<CloudVmInstanceStatus> startedInstanceStatusList = generateStartedCloudVmInstanceStatuses(instancesToRestart);
        when(instanceConnector.
                restartWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(instancesToRestart),
                        any(Long.class),
                        eq(EXCLUDED_STATUSES))).thenReturn(startedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).restartWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(instancesToRestart), any(Long.class), eq(EXCLUDED_STATUSES));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(RestartInstancesResult.class, resultEvent.getData().getClass());
        RestartInstancesResult result = (RestartInstancesResult) resultEvent.getData();
        verifyAffectedInstancesInResult(startedInstanceStatusList, result.getResults().getResults());
    }

    @Test
    void testInstanceRestartWithSomeInstancesFailToRestart() {
        List<CloudInstance> instancesToRestart = generateCloudInstances(10);
        RestartInstancesRequest request = new RestartInstancesRequest(cloudContext, cloudCredential, null, instancesToRestart);
        List<CloudVmInstanceStatus> halfStartedInstanceStatusList = generateHalfStartedHalfStoppedCloudVmInstanceStatuses(instancesToRestart);

        when(instanceConnector.
                restartWithLimitedRetry(
                        any(AuthenticatedContext.class),
                        eq(null),
                        eq(instancesToRestart),
                        any(Long.class),
                        eq(EXCLUDED_STATUSES))).thenReturn(halfStartedInstanceStatusList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).restartWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(instancesToRestart), any(Long.class), eq(EXCLUDED_STATUSES));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(RestartInstancesResult.class, resultEvent.getData().getClass());
        RestartInstancesResult result = (RestartInstancesResult) resultEvent.getData();
        assertEquals(halfStartedInstanceStatusList.size() / 2, result.getResults().getResults().size());
    }

    @Test
    void testFailureFromCloudProviderWhenRestartingInstances() {
        List<CloudInstance> instancesToRestart = generateCloudInstances(10);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(instancesToRestart);

        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class),
                eq(instancesToRestart)))
                .thenReturn(stoppedInstanceStatusList);
        when(instanceConnector.
                restartWithLimitedRetry(
                        any(AuthenticatedContext.class), any(List.class), any(List.class), any(Long.class), any(List.class)))
                .thenThrow(new RuntimeException("CloudProviderStartError"));

        RestartInstancesRequest request = new RestartInstancesRequest(cloudContext, cloudCredential, null, instancesToRestart);

        Event event = new Event(request);

        underTest.accept(event);
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).restartWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(instancesToRestart), any(Long.class), eq(EXCLUDED_STATUSES));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        RestartInstancesResult result = (RestartInstancesResult) resultEvent.getData();
        assertEquals(RestartInstancesResult.class, resultEvent.getData().getClass());
        assertEquals(result.getResults().getResults().size(), 0);
        assertEquals(EventStatus.OK, result.getStatus());
    }

    @Test
    void testUnableToRestartInstancesAndNotAbleToCheckInstances() {
        List<CloudInstance> instancesToRestart = generateCloudInstances(10);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudVmInstanceStatuses(instancesToRestart);

        when(instanceConnector.
                restartWithLimitedRetry(
                        any(AuthenticatedContext.class), any(List.class), any(List.class), any(Long.class), any(List.class)))
                .thenThrow(new RuntimeException("CloudProviderStartError"));

        when(instanceConnector.checkWithoutRetry(
                any(AuthenticatedContext.class), any(List.class)))
                .thenThrow(new RuntimeException("CloudProviderCheckStateError"));

        RestartInstancesRequest request = new RestartInstancesRequest(cloudContext, cloudCredential, null, instancesToRestart);

        Event event = new Event(request);

        underTest.accept(event);
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), resultCaptor.capture());
        verify(instanceConnector).restartWithLimitedRetry(
                any(AuthenticatedContext.class), eq(null), eq(instancesToRestart), any(Long.class), eq(EXCLUDED_STATUSES));

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        RestartInstancesResult result = (RestartInstancesResult) resultEvent.getData();

        assertEquals("Error while attempting to restart instances", result.getErrorDetails().getMessage());
        assertNull(result.getResults());
        assertEquals(EventStatus.FAILED, result.getStatus());
        assertEquals("RESTARTINSTANCESRESULT_ERROR", result.selector());
    }

    private void verifyAffectedInstancesInResult(List<CloudVmInstanceStatus> srcList, List<CloudVmInstanceStatus> listFromResult) {
        assertEquals(srcList.size(), listFromResult.size());
        for (int i = 0; i < srcList.size(); i++) {
            assertEquals(srcList.get(i).getCloudInstance().getInstanceId(), listFromResult.get(i).getCloudInstance().getInstanceId());
            assertEquals(InstanceStatus.STARTED, listFromResult.get(i).getStatus());
        }
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

    private List<CloudVmInstanceStatus> generateStartedCloudVmInstanceStatuses(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateStoppedCloudVmInstanceStatuses(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateHalfStartedHalfStoppedCloudVmInstanceStatuses(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        boolean instanceStateChange = false;
        for (CloudInstance cloudInstance : cloudInstances) {
            if (instanceStateChange) {
                cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED));
                instanceStateChange = false;
            } else {
                cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED));
                instanceStateChange = true;
            }
        }
        return cloudVmInstanceStatusList;
    }
}