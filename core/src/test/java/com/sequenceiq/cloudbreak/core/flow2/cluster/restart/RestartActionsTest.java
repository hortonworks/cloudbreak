package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartActions.AbstractRestartActions;
import com.sequenceiq.cloudbreak.core.flow2.event.RestartInstancesEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class RestartActionsTest {

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String SELECTOR = "dontcareforthistest";

    private static final Long STACK_ID = 100L;

    private static final String ENV_CRN = "envCrn";

    @Mock
    private RestartService restartService;

    @InjectMocks
    private RestartActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private Resource resource;

    @Mock
    private CloudbreakMetricService metricService;

    @Test
    void testRestartInstancesAction() throws Exception {
        AbstractRestartActions<RestartInstancesEvent> action =
                (AbstractRestartActions<RestartInstancesEvent>) underTest.restartAction();
        initActionPrivateFields(action);

        List<InstanceMetadataView> instancesToRestart =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY);

        List<String> instanceIds = instancesToRestart.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        RestartContext context = createContext(instanceIds);
        RestartInstancesEvent request = new RestartInstancesEvent(SELECTOR, STACK_ID, instanceIds, null);
        mockStackEtc(instancesToRestart);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, request, Collections.emptyMap());

        verify(restartService).startInstanceRestart(eq(context));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("RESTARTINSTANCESREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(RestartInstancesRequest.class);

        verify(instanceMetaDataToCloudInstanceConverter, times(1)).convert(anyCollection(), any());
        RestartInstancesRequest req = (RestartInstancesRequest) argumentCaptor.getValue();
        assertEquals(5, req.getCloudInstances().size());
    }

    @Test
    void testRestartInstancesFinishedAction() throws Exception {
        AbstractRestartActions<RestartInstancesResult> action =
                (AbstractRestartActions<RestartInstancesResult>) underTest.restartFinishedAction();
        initActionPrivateFields(action);
        List<InstanceMetadataView> instancesToRestart =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        List<String> instanceIds = instancesToRestart.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        RestartContext context = createContext(instanceIds);
        List<CloudInstance> cloudInstancesToRestart = convertToCloudInstance(instancesToRestart);
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstancesToRestart) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED));
        }
        InstancesStatusResult results = new InstancesStatusResult(cloudContext, cloudVmInstanceStatusList);

        RestartInstancesResult payload = new RestartInstancesResult(STACK_ID, results, instanceIds);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Collections.emptyMap());

        verify(restartService).instanceRestartFinished(eq(context), eq(List.of()), eq(instanceIds));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("RESTARTFINALIZED", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(RestartInstancesResult.class);
    }

    @Test
    void testRestartInstancesFailedAction() throws Exception {
        AbstractRestartActions<StackFailureEvent> action =
                ((AbstractRestartActions<StackFailureEvent>) underTest.restartFailureAction());
        initActionPrivateFields(action);
        List<InstanceMetadataView> instancesToRestart =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY);

        List<String> instanceIds = instancesToRestart.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        RestartContext context = createContext(instanceIds);

        Exception exception = new Exception("FailedRestart");
        StackFailureEvent stackFailureEvent = new StackFailureEvent(STACK_ID, exception);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, stackFailureEvent, Collections.emptyMap());

        verify(restartService).allInstanceRestartFailed(eq(context), eq(exception));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("RESTARTFAILHANDLED", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StackEvent.class);
    }

    private void mockStackEtc(List<InstanceMetadataView> instancesToRestart) {

        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(resourceService.getAllByStackId(STACK_ID))
                .thenReturn(Collections.singletonList(resource));

        lenient().when(resourceToCloudResourceConverter.convert(resource)).thenReturn(cloudResource);
        lenient().when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId())).thenReturn(instancesToRestart);


        List<CloudInstance> cloudInstancesToRestart = convertToCloudInstance(instancesToRestart);

        lenient().when(instanceMetaDataToCloudInstanceConverter.convert(eq(instancesToRestart), any()))
                .thenReturn(cloudInstancesToRestart);
    }

    private RestartContext createContext(List<String> instanceIds) {
        return new RestartContext(flowParameters, stack, instanceIds,
                cloudContext, cloudCredential);
    }

    private List<InstanceMetadataView> generateInstances(int count, int startIndex,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        for (int i = 0; i < count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + (startIndex + i));
            instanceMetaData.setInstanceStatus(instanceStatus);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + (startIndex + i));
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private List<CloudInstance> convertToCloudInstance(List<InstanceMetadataView> instances) {
        List<CloudInstance> cloudInstances = new LinkedList<>();
        for (InstanceMetadataView im : instances) {
            CloudInstance cloudInstance = new CloudInstance(im.getInstanceId(), null, null, "blah", "blah");
            cloudInstances.add(cloudInstance);
        }
        return cloudInstances;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }

}