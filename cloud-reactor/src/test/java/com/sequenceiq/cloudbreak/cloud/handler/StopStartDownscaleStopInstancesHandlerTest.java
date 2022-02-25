package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class StopStartDownscaleStopInstancesHandlerTest {

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final Long EXPECTED_STOP_POLL_TIMEBOUND_MS = 600_000L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StopStartDownscaleStopInstancesHandler underTest;

    @Mock
    private CloudConnector<Object> cloudConnector;

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
        assertEquals(StopStartDownscaleStopInstancesRequest.class, underTest.type());
    }

    @Test
    void testAllSuccessfullyStopped() {
        List<CloudInstance> cloudInstancesToStop = generateCloudInstances(5);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudInstances(cloudInstancesToStop);

        testExpectedResultInernal(cloudInstancesToStop, stoppedInstanceStatusList);
    }

    @Test
    void testSomeStopAttemptedInstancesInTerminalState() {
        List<CloudInstance> cloudInstancesToStop = generateCloudInstances(5);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateCloudVMInstanceStatusWithSomeInTerminalState(cloudInstancesToStop);
        testExpectedResultInernal(cloudInstancesToStop, stoppedInstanceStatusList);
    }

    @Test
    void testNoInstancesToStop() {
        List<CloudInstance> cloudInstancesToStop = generateCloudInstances(0);
        List<CloudVmInstanceStatus> stoppedInstanceStatusList = generateStoppedCloudInstances(cloudInstancesToStop);
        testExpectedResultInernal(cloudInstancesToStop, stoppedInstanceStatusList, false);
    }

    @Test
    void testFailureFromCloudProviderWhenStoppingInstances() {
        List<CloudInstance> cloudInstancesToStop = generateCloudInstances(5);

        when(instanceConnector.stopWithLimitedRetry(any(AuthenticatedContext.class), eq(null), eq(cloudInstancesToStop), any(Long.class)))
                .thenThrow(new RuntimeException("CloudProviderStopError"));

        StopStartDownscaleStopInstancesRequest request =
                new StopStartDownscaleStopInstancesRequest(cloudContext, cloudCredential, cloudStack, cloudInstancesToStop);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(Object.class), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartDownscaleStopInstancesResult.class, resultEvent.getData().getClass());
        StopStartDownscaleStopInstancesResult result = (StopStartDownscaleStopInstancesResult) resultEvent.getData();

        assertEquals(0, result.getAffectedInstanceStatuses().size());
        assertEquals("CloudProviderStopError", result.getErrorDetails().getMessage());
        assertEquals("STOPSTARTDOWNSCALESTOPINSTANCESRESULT_ERROR", result.selector());
        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    private void testExpectedResultInernal(List<CloudInstance> cloudInstancesToStop, List<CloudVmInstanceStatus> cloudConnectoReturnList) {
        testExpectedResultInernal(cloudInstancesToStop, cloudConnectoReturnList, true);
    }

    private void testExpectedResultInernal(List<CloudInstance> cloudInstancesToStop, List<CloudVmInstanceStatus> cloudConnectoReturnList,
            boolean expectedCloudInteractions) {
        StopStartDownscaleStopInstancesRequest request =
                new StopStartDownscaleStopInstancesRequest(cloudContext, cloudCredential, cloudStack, cloudInstancesToStop);

        lenient().when(instanceConnector.stopWithLimitedRetry(any(AuthenticatedContext.class), eq(null), eq(cloudInstancesToStop), any(Long.class)))
                .thenReturn(cloudConnectoReturnList);

        Event event = new Event(request);
        underTest.accept(event);

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(Object.class), resultCaptor.capture());

        if (expectedCloudInteractions) {
            verify(instanceConnector).stopWithLimitedRetry(any(AuthenticatedContext.class), eq(null), eq(cloudInstancesToStop),
                    eq(EXPECTED_STOP_POLL_TIMEBOUND_MS));
        }
        verifyNoMoreInteractions(instanceConnector);

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StopStartDownscaleStopInstancesResult.class, resultEvent.getData().getClass());
        StopStartDownscaleStopInstancesResult result = (StopStartDownscaleStopInstancesResult) resultEvent.getData();

        assertEquals(cloudConnectoReturnList, result.getAffectedInstanceStatuses());
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

    private List<CloudVmInstanceStatus> generateStoppedCloudInstances(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> generateCloudVMInstanceStatusWithSomeInTerminalState(List<CloudInstance> cloudInstances) {
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
                cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED);
            }

            count++;
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }
}
