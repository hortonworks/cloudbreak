package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleActions.AbstractRollingVerticalScaleActions;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesResult;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class RollingVerticalScaleActionsTest {

    private static final String INSTANCE_GROUP_NAME = "master";

    private static final String SELECTOR = "ROLLING_VERTICALSCALE_TRIGGER_EVENT";

    private static final Long STACK_ID = 100L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String PREVIOUS_INSTANCE_TYPE = "m5.large";

    private static final String TARGET_INSTANCE_TYPE = "m5.xlarge";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private com.sequenceiq.cloudbreak.util.StackUtil stackUtil;

    @Mock
    private com.sequenceiq.cloudbreak.service.stack.StackService stackService;

    @Mock
    private com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter cloudStackConverter;

    @InjectMocks
    private RollingVerticalScaleActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private MetricService metricService;

    @Mock
    private InstanceGroupView instanceGroupView;

    @Mock
    private Template template;

    @Test
    void testStopInstancesAction() throws Exception {
        AbstractRollingVerticalScaleActions<RollingVerticalScaleTriggerEvent> action =
                (AbstractRollingVerticalScaleActions<RollingVerticalScaleTriggerEvent>) underTest.stopInstancesAction();
        initActionPrivateFields(action);

        RollingVerticalScaleContext context = createContext();
        List<String> instanceIds = createInstanceIds(3);
        RollingVerticalScaleTriggerEvent request = createTriggerEvent(instanceIds);

        List<InstanceMetadataView> instances = generateInstances(instanceIds, INSTANCE_GROUP_NAME);
        List<CloudInstance> cloudInstances = convertToCloudInstance(instances);
        List<CloudResource> cloudResources = createCloudResources(2);

        mockStackEtc(instances);
        when(instanceMetaDataToCloudInstanceConverter.convert(anyCollection(), any())).thenReturn(cloudInstances);
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Collections.emptyList());
        lenient().when(resourceToCloudResourceConverter.convert(any())).thenReturn(cloudResources.get(0));
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, request, createVariables(instanceIds));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLINGVERTICALSCALESTOPINSTANCESREQUEST", event);
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleStopInstancesRequest.class);

        RollingVerticalScaleStopInstancesRequest req = (RollingVerticalScaleStopInstancesRequest) captor.getValue();
        assertThat(req.getCloudInstances()).hasSameElementsAs(cloudInstances);
        assertThat(req.getTargetInstanceType()).isEqualTo(TARGET_INSTANCE_TYPE);
        assertThat(req.getRollingVerticalScaleResult().getGroup()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(req.getRollingVerticalScaleResult().getInstanceIds()).hasSameElementsAs(instanceIds);
    }

    @Test
    void testScaleInstancesAction() throws Exception {
        AbstractRollingVerticalScaleActions<RollingVerticalScaleStopInstancesResult> action =
                (AbstractRollingVerticalScaleActions<RollingVerticalScaleStopInstancesResult>) underTest.scaleInstancesActions();
        initActionPrivateFields(action);

        RollingVerticalScaleContext context = createContext();
        List<String> instanceIds = createInstanceIds(3);
        RollingVerticalScaleResult result = createRollingVerticalScaleResult(instanceIds, RollingVerticalScaleStatus.STOPPED);
        RollingVerticalScaleStopInstancesResult payload = new RollingVerticalScaleStopInstancesResult(STACK_ID, result);

        List<InstanceMetadataView> instances = generateInstances(instanceIds, INSTANCE_GROUP_NAME);
        List<CloudResource> cloudResources = createCloudResources(instanceIds.size());

        mockStackEtc(instances);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getResources()).thenReturn(Collections.emptySet());
        when(stackDto.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackUtil.getCloudCredential("envCrn")).thenReturn(cloudCredential);
        lenient().when(cloudStackConverter.convert(any())).thenReturn(mock(com.sequenceiq.cloudbreak.cloud.model.CloudStack.class));
        lenient().when(cloudStackConverter.updateWithVerticalScaleRequest(any(), any()))
                .thenReturn(mock(com.sequenceiq.cloudbreak.cloud.model.CloudStack.class));
        lenient().when(resourceToCloudResourceConverter.convert(any())).thenReturn(cloudResources.get(0));
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        initActionPrivateFieldsWithServices(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, createVariables(instanceIds));

        verify(rollingVerticalScaleService).verticalScaleInstances(eq(STACK_ID), eq(instanceIds), any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLINGVERTICALSCALEINSTANCESREQUEST", event);
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleInstancesRequest.class);

        RollingVerticalScaleInstancesRequest req = (RollingVerticalScaleInstancesRequest) captor.getValue();
        assertThat(req.getRollingVerticalScaleResult()).isEqualTo(result);
    }

    @Test
    void testStartInstancesAction() throws Exception {
        AbstractRollingVerticalScaleActions<RollingVerticalScaleInstancesResult> action =
                (AbstractRollingVerticalScaleActions<RollingVerticalScaleInstancesResult>) underTest.startInstancesAction();
        initActionPrivateFields(action);

        RollingVerticalScaleContext context = createContext();
        List<String> instanceIds = createInstanceIds(3);
        RollingVerticalScaleResult result = createRollingVerticalScaleResult(instanceIds, RollingVerticalScaleStatus.SCALED);
        RollingVerticalScaleInstancesResult payload = new RollingVerticalScaleInstancesResult(STACK_ID, result);

        List<InstanceMetadataView> instances = generateInstances(instanceIds, INSTANCE_GROUP_NAME);
        List<CloudInstance> cloudInstances = convertToCloudInstance(instances);
        List<CloudResource> cloudResources = createCloudResources(instanceIds.size());

        mockStackEtc(instances);
        when(instanceMetaDataToCloudInstanceConverter.convert(anyCollection(), any())).thenReturn(cloudInstances);
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Collections.emptyList());
        lenient().when(resourceToCloudResourceConverter.convert(any())).thenReturn(cloudResources.get(0));
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, createVariables(instanceIds));

        verify(rollingVerticalScaleService).startInstances(eq(STACK_ID), eq(instanceIds), eq(INSTANCE_GROUP_NAME));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLINGVERTICALSCALESTARTINSTANCESREQUEST", event);
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleStartInstancesRequest.class);

        RollingVerticalScaleStartInstancesRequest req = (RollingVerticalScaleStartInstancesRequest) captor.getValue();
        assertThat(req.getCloudInstances()).hasSameElementsAs(cloudInstances);
        assertThat(req.getRollingVerticalScaleResult()).isEqualTo(result);
    }

    @Test
    void testVerticalScaleFinishedAction() throws Exception {
        AbstractRollingVerticalScaleActions<RollingVerticalScaleStartInstancesResult> action =
                (AbstractRollingVerticalScaleActions<RollingVerticalScaleStartInstancesResult>) underTest.verticalScaleFinishedAction();
        initActionPrivateFields(action);

        RollingVerticalScaleContext context = createContext();
        List<String> instanceIds = createInstanceIds(3);
        RollingVerticalScaleResult result = createRollingVerticalScaleResult(instanceIds, RollingVerticalScaleStatus.SUCCESS);
        RollingVerticalScaleStartInstancesResult payload = new RollingVerticalScaleStartInstancesResult(STACK_ID, result);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, createVariables(instanceIds));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLING_VERTICALSCALE_FINALIZED_EVENT", event);
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleStartInstancesResult.class);
    }

    @Test
    void testVerticalScaleFinishedActionWithFailures() throws Exception {
        AbstractRollingVerticalScaleActions<RollingVerticalScaleStartInstancesResult> action =
                (AbstractRollingVerticalScaleActions<RollingVerticalScaleStartInstancesResult>) underTest.verticalScaleFinishedAction();
        initActionPrivateFields(action);

        RollingVerticalScaleContext context = createContext();
        List<String> instanceIds = createInstanceIds(3);
        RollingVerticalScaleResult result = createRollingVerticalScaleResultWithFailures(instanceIds);
        RollingVerticalScaleStartInstancesResult payload = new RollingVerticalScaleStartInstancesResult(STACK_ID, result);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, createVariables(instanceIds));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // When there are failures, only the failure event is sent
        verify(reactorEventFactory, times(1)).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLING_VERTICALSCALE_FAILURE_EVENT", event);
        assertThat(captor.getValue()).isInstanceOf(StackFailureEvent.class);
    }

    @Test
    void testVerticalScaleFailedAction() throws Exception {
        AbstractStackFailureAction<RollingVerticalScaleState, RollingVerticalScaleEvent> action =
                (AbstractStackFailureAction<RollingVerticalScaleState, RollingVerticalScaleEvent>) underTest.verticalScaleFailedAction();
        initActionPrivateFields(action);

        StackFailureContext stackFailureContext = new StackFailureContext(flowParameters, stack, STACK_ID);
        Exception exception = new Exception("Failed to scale");
        StackFailureEvent stackFailureEvent = new StackFailureEvent(STACK_ID, exception);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stackFailureContext, stackFailureEvent, Collections.emptyMap());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT", event);
        assertThat(captor.getValue()).isInstanceOf(StackEvent.class);

        StackEvent eventPayload = (StackEvent) captor.getValue();
        assertThat(eventPayload.getResourceId()).isEqualTo(STACK_ID);
    }

    private RollingVerticalScaleContext createContext() {
        return new RollingVerticalScaleContext(flowParameters, stack, createInstanceIds(3),
                createStackVerticalScaleV4Request(), cloudContext, cloudCredential, TARGET_INSTANCE_TYPE);
    }

    private void mockStackEtc(List<InstanceMetadataView> instances) {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getRegion()).thenReturn("us-west-2");
        lenient().when(stack.getAvailabilityZone()).thenReturn("us-west-2a");
        lenient().when(stack.getResourceCrn()).thenReturn("crn:cdp:cloudbreak:us-west-2:123:stack:100");
        lenient().when(stack.getName()).thenReturn("teststack");
        lenient().when(stack.getCloudPlatform()).thenReturn("AWS");
        lenient().when(stack.getPlatformVariant()).thenReturn("AWS");
        lenient().when(stack.getWorkspaceId()).thenReturn(1L);
        lenient().when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        lenient().when(stack.getTenant()).thenReturn(null);

        lenient().when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(instances);
        lenient().when(instanceGroupService.findInstanceGroupViewByStackIdAndGroupName(STACK_ID, INSTANCE_GROUP_NAME))
                .thenReturn(java.util.Optional.of(instanceGroupView));
        lenient().when(instanceGroupView.getTemplate()).thenReturn(template);
        lenient().when(template.getInstanceType()).thenReturn(PREVIOUS_INSTANCE_TYPE);
    }

    private List<InstanceMetadataView> generateInstances(List<String> instanceIds, String instanceGroupName) {
        List<InstanceMetadataView> instances = new ArrayList<>();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
        for (String instanceId : instanceIds) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(instanceId);
            instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private List<String> createInstanceIds(int count) {
        List<String> instanceIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            instanceIds.add(INSTANCE_ID_PREFIX + i);
        }
        return instanceIds;
    }

    private List<CloudInstance> convertToCloudInstance(List<InstanceMetadataView> instances) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (InstanceMetadataView im : instances) {
            CloudInstance cloudInstance = new CloudInstance(im.getInstanceId(), null, null, "blah", "blah");
            cloudInstances.add(cloudInstance);
        }
        return cloudInstances;
    }

    private List<CloudResource> createCloudResources(int count) {
        List<CloudResource> cloudResources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CloudResource cloudResource = CloudResource.builder()
                    .withType(com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE)
                    .withName("instance-" + i)
                    .withReference(String.valueOf(i))
                    .withStatus(com.sequenceiq.common.api.type.CommonStatus.CREATED)
                    .build();
            cloudResources.add(cloudResource);
        }
        return cloudResources;
    }

    private RollingVerticalScaleTriggerEvent createTriggerEvent(List<String> instanceIds) {
        return new RollingVerticalScaleTriggerEvent(SELECTOR, STACK_ID, instanceIds, createStackVerticalScaleV4Request());
    }

    private StackVerticalScaleV4Request createStackVerticalScaleV4Request() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(INSTANCE_GROUP_NAME);
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        templateRequest.setInstanceType(TARGET_INSTANCE_TYPE);
        request.setTemplate(templateRequest);
        return request;
    }

    private RollingVerticalScaleResult createRollingVerticalScaleResult(List<String> instanceIds, RollingVerticalScaleStatus status) {
        RollingVerticalScaleResult result = new RollingVerticalScaleResult(instanceIds, INSTANCE_GROUP_NAME);
        for (String instanceId : instanceIds) {
            result.setStatus(instanceId, status);
        }
        return result;
    }

    private RollingVerticalScaleResult createRollingVerticalScaleResultWithFailures(List<String> instanceIds) {
        RollingVerticalScaleResult result = new RollingVerticalScaleResult(instanceIds, INSTANCE_GROUP_NAME);
        result.setStatus(instanceIds.get(0), RollingVerticalScaleStatus.SUCCESS);
        result.setStatus(instanceIds.get(1), RollingVerticalScaleStatus.SCALING_FAILED, "Failed to scale");
        result.setStatus(instanceIds.get(2), RollingVerticalScaleStatus.STOP_FAILED, "Failed to stop");
        return result;
    }

    private Map<Object, Object> createVariables(List<String> instanceIds) {
        Map<Object, Object> variables = new java.util.HashMap<>();
        variables.put(RollingVerticalScaleActions.AbstractRollingVerticalScaleActions.TARGET_INSTANCES, instanceIds);
        variables.put(RollingVerticalScaleActions.AbstractRollingVerticalScaleActions.STACK_VERTICALSCALE_V4_REQUEST, createStackVerticalScaleV4Request());
        variables.put(RollingVerticalScaleActions.AbstractRollingVerticalScaleActions.TARGET_INSTANCE_TYPE, TARGET_INSTANCE_TYPE);
        variables.put(RollingVerticalScaleActions.AbstractRollingVerticalScaleActions.PREVIOUS_INSTANCE_TYPE, PREVIOUS_INSTANCE_TYPE);
        variables.put(RollingVerticalScaleActions.AbstractRollingVerticalScaleActions.GROUP_BEING_SCALED, INSTANCE_GROUP_NAME);
        return variables;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }

    private void initActionPrivateFieldsWithServices(Action<?, ?> action) {
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "stackUtil", stackUtil);
        ReflectionTestUtils.setField(action, "stackService", stackService);
    }
}