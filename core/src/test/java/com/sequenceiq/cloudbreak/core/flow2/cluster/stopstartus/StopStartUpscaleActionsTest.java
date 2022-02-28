package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleActions.AbstractStopStartUpscaleActions;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class StopStartUpscaleActionsTest {

    private static final String INSTANCE_GROUP_NAME_ACTIONABLE = "compute";

    private static final String INSTANCE_GROUP_NAME_RANDOM = "other";

    private static final String SELECTOR = "dontcareforthistest";

    private static final Long STACK_ID = 100L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private StopStartUpscaleFlowService stopStartUpscaleFlowService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    @InjectMocks
    private StopStartUpscaleActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private StackView stackView;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private CloudbreakMetricService metricService;

    @Test
    void testStartInstancesAction() throws Exception {
        AbstractStopStartUpscaleActions<StopStartUpscaleTriggerEvent> action = (AbstractStopStartUpscaleActions<StopStartUpscaleTriggerEvent>)
                underTest.startInstancesAction();
        initActionPrivateFields(action);

        StopStartUpscaleContext stopStartUpscaleContext = createContext(5);
        StopStartUpscaleTriggerEvent payload = new StopStartUpscaleTriggerEvent(
                SELECTOR, STACK_ID, INSTANCE_GROUP_NAME_ACTIONABLE,
                5, ClusterManagerType.CLOUDERA_MANAGER);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);
        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartUpscaleContext, payload, Collections.emptyMap());

        verify(stopStartUpscaleFlowService).startingInstances(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(5));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTUPSCALESTARTINSTANCESREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartUpscaleStartInstancesRequest.class);

        verify(instanceMetaDataToCloudInstanceConverter, times(2)).convert(anyCollection(), anyString(), any(StackAuthentication.class));

        StopStartUpscaleStartInstancesRequest req = (StopStartUpscaleStartInstancesRequest) argumentCaptor.getValue();

        Assert.assertEquals(15, req.getAllInstancesInHg().size());
        Assert.assertEquals(10, req.getStoppedCloudInstancesInHg().size());
    }

    @Test
    void testCmCommissionAction() throws Exception {
        // Simple scenario. Adequate instances. Everything started etc.
        AbstractStopStartUpscaleActions<StopStartUpscaleStartInstancesResult> action =
                (AbstractStopStartUpscaleActions<StopStartUpscaleStartInstancesResult>) underTest.cmCommissionAction();
        initActionPrivateFields(action);

        int adjustment = 5;

        StopStartUpscaleContext stopStartUpscaleContext = createContext(adjustment);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> allInstancesInHgMdList = new LinkedList<>();
        allInstancesInHgMdList.addAll(instancesActionableStopped);
        allInstancesInHgMdList.addAll(instancesActionableNotStopped);

        List<CloudInstance> stoppedInstancesInHgList = convertToCloudInstance(instancesActionableStopped);
        List<CloudInstance> allInstancesInHgList = convertToCloudInstance(allInstancesInHgMdList);

        StopStartUpscaleStartInstancesRequest startInstancesRequest =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME_ACTIONABLE,
                        stoppedInstancesInHgList, allInstancesInHgList, Collections.emptyList(), stopStartUpscaleContext.getAdjustment());

        List<CloudVmInstanceStatus> affectedInstances = constructStartedCloudVmInstanceStatus(stoppedInstancesInHgList, adjustment);

        StopStartUpscaleStartInstancesResult payload = new StopStartUpscaleStartInstancesResult(STACK_ID, startInstancesRequest, affectedInstances);

        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartUpscaleContext, payload, Collections.emptyMap());

        ArgumentCaptor<List> instancesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stopStartUpscaleFlowService).instancesStarted(eq(STACK_ID), instancesCaptor.capture());
        Assert.assertEquals(adjustment, instancesCaptor.getValue().size());
        verify(stopStartUpscaleFlowService).upscaleCommissioningNodes(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE),
                instancesCaptor.capture(), eq(Collections.emptyList()));
        Assert.assertEquals(adjustment, instancesCaptor.getValue().size());
        verifyNoMoreInteractions(stopStartUpscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTUPSCALECOMMISSIONVIACMREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartUpscaleCommissionViaCMRequest.class);

        StopStartUpscaleCommissionViaCMRequest req = (StopStartUpscaleCommissionViaCMRequest) argumentCaptor.getValue();
        Assert.assertEquals(adjustment, req.getStartedInstancesToCommission().size());
    }

    @Test
    void testUpscaleFailedAction() throws Exception {
        AbstractStackFailureAction<StopStartUpscaleState, StopStartUpscaleEvent> action =
                (AbstractStackFailureAction<StopStartUpscaleState, StopStartUpscaleEvent>) underTest.clusterUpscaleFailedAction();
        initActionPrivateFields(action);

        StackFailureContext stackFailureContext = new StackFailureContext(flowParameters, stackView);
        Exception exception = new Exception("FailedStart");
        StackFailureEvent stackFailureEvent = new StackFailureEvent(STACK_ID, exception);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);
        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stackFailureContext, stackFailureEvent, Collections.emptyMap());

        verify(stopStartUpscaleFlowService).clusterUpscaleFailed(eq(STACK_ID), eq(exception));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_UPSCALE_FAIL_HANDLED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StackEvent.class);
    }

    @Test
    void testCmCommissionAction2() throws Exception {
        // All instances didn't start from the previous step.
        AbstractStopStartUpscaleActions<StopStartUpscaleStartInstancesResult> action =
                (AbstractStopStartUpscaleActions<StopStartUpscaleStartInstancesResult>) underTest.cmCommissionAction();
        initActionPrivateFields(action);

        int adjustment = 5;

        StopStartUpscaleContext stopStartUpscaleContext = createContext(adjustment);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> allInstancesInHgMdList = new LinkedList<>();
        allInstancesInHgMdList.addAll(instancesActionableStopped);
        allInstancesInHgMdList.addAll(instancesActionableNotStopped);

        List<CloudInstance> stoppedInstancesInHgList = convertToCloudInstance(instancesActionableStopped);
        List<CloudInstance> allInstancesInHgList = convertToCloudInstance(allInstancesInHgMdList);

        StopStartUpscaleStartInstancesRequest startInstancesRequest =
                new StopStartUpscaleStartInstancesRequest(cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME_ACTIONABLE,
                        stoppedInstancesInHgList, allInstancesInHgList, Collections.emptyList(), stopStartUpscaleContext.getAdjustment());

        List<CloudVmInstanceStatus> affectedInstances = constructVmInstanceStatusWithTerminated(stoppedInstancesInHgList, adjustment, 2);
        int expectedCount = adjustment - 2;

        StopStartUpscaleStartInstancesResult payload = new StopStartUpscaleStartInstancesResult(STACK_ID, startInstancesRequest, affectedInstances);

        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartUpscaleContext, payload, Collections.emptyMap());

        ArgumentCaptor<List> instancesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stopStartUpscaleFlowService).instancesStarted(eq(STACK_ID), instancesCaptor.capture());
        Assert.assertEquals(expectedCount, instancesCaptor.getValue().size());
        verify(stopStartUpscaleFlowService).logInstancesFailedToStart(eq(STACK_ID), instancesCaptor.capture());
        Assert.assertEquals(2, instancesCaptor.getValue().size());
        verify(stopStartUpscaleFlowService).warnNotEnoughInstances(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(adjustment), eq(expectedCount));
        verify(stopStartUpscaleFlowService).upscaleCommissioningNodes(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE),
                instancesCaptor.capture(), eq(Collections.emptyList()));
        Assert.assertEquals(expectedCount, instancesCaptor.getValue().size());
        verifyNoMoreInteractions(stopStartUpscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTUPSCALECOMMISSIONVIACMREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartUpscaleCommissionViaCMRequest.class);

        StopStartUpscaleCommissionViaCMRequest req = (StopStartUpscaleCommissionViaCMRequest) argumentCaptor.getValue();
        Assert.assertEquals(expectedCount, req.getStartedInstancesToCommission().size());
    }

    @Test
    void testUpscaleFinishedAction1() throws Exception {
        // All started.
        AbstractStopStartUpscaleActions<StopStartUpscaleCommissionViaCMResult> action
                = (AbstractStopStartUpscaleActions<StopStartUpscaleCommissionViaCMResult>) underTest.upscaleFinishedAction();
        initActionPrivateFields(action);

        int adjustment = 10;

        StopStartUpscaleContext stopStartUpscaleContext = createContext(adjustment);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> startedInstances = instancesActionableStopped.subList(0, adjustment);

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME_ACTIONABLE, startedInstances, Collections.emptyList());
        Set<String> successfullyCommissionedFqdns = startedInstances.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toUnmodifiableSet());
        StopStartUpscaleCommissionViaCMResult payload =
                new StopStartUpscaleCommissionViaCMResult(request, successfullyCommissionedFqdns, Collections.emptyList());

        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartUpscaleContext, payload, Collections.emptyMap());

        verify(stopStartUpscaleFlowService).clusterUpscaleFinished(
                any(), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(startedInstances), eq(DetailedStackStatus.AVAILABLE));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_UPSCALE_FINALIZED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);
    }

    @Test
    void testUpscaleFinishedAction2() throws Exception {
        // Some did not commission.
        AbstractStopStartUpscaleActions<StopStartUpscaleCommissionViaCMResult> action =
                (AbstractStopStartUpscaleActions<StopStartUpscaleCommissionViaCMResult>) underTest.upscaleFinishedAction();
        initActionPrivateFields(action);

        int adjustment = 5;

        StopStartUpscaleContext stopStartUpscaleContext = createContext(adjustment);

        List<InstanceMetaData> instancesActionableNotStopped =
                generateInstances(5, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableStopped =
                generateInstances(10, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomNotStopped =
                generateInstances(3, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomStopped =
                generateInstances(8, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> startedInstances = instancesActionableStopped.subList(0, adjustment - 2);
        List<InstanceMetaData> notCommissioned = instancesActionableStopped.subList(adjustment - 2, adjustment);
        List<String> notCommissionedFqdns = notCommissioned.stream().map(x -> x.getDiscoveryFQDN()).collect(Collectors.toList());

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME_ACTIONABLE, startedInstances, notCommissioned);
        Set<String> successfullyCommissionedFqdns = startedInstances.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toUnmodifiableSet());
        StopStartUpscaleCommissionViaCMResult payload = new StopStartUpscaleCommissionViaCMResult(request, successfullyCommissionedFqdns, notCommissionedFqdns);

        // Mocks
        mockStackEtc(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartUpscaleContext, payload, Collections.emptyMap());

        verify(stopStartUpscaleFlowService).logInstancesFailedToCommission(eq(STACK_ID), eq(notCommissionedFqdns));
        verify(stopStartUpscaleFlowService).clusterUpscaleFinished(
                any(), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(startedInstances), eq(DetailedStackStatus.AVAILABLE));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_UPSCALE_FINALIZED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);
    }

    private StopStartUpscaleContext createContext(int adjustment) {
        return new StopStartUpscaleContext(flowParameters, stack, stackView,
                cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME_ACTIONABLE,
                adjustment, ClusterManagerType.CLOUDERA_MANAGER);
    }

    private void mockStackEtc(List<InstanceMetaData> instancesActionableNotStopped, List<InstanceMetaData> instancesActionableStopped,
            List<InstanceMetaData> instancesRandomNotStopped, List<InstanceMetaData> instancesRandomStopped) {

        List<InstanceMetaData> combined =
                Stream.of(instancesActionableNotStopped, instancesActionableStopped, instancesRandomNotStopped, instancesRandomStopped)
                .flatMap(Collection::stream).collect(Collectors.toList());
        lenient().when(stack.getNotDeletedAndNotZombieInstanceMetaDataList()).thenReturn(combined);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        lenient().when(stack.getId()).thenReturn(STACK_ID);

        lenient().when(stackView.getId()).thenReturn(STACK_ID);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME_ACTIONABLE);
        instanceGroup.setInstanceMetaData(new HashSet<>(combined));
        lenient().when(stack.getInstanceGroupByInstanceGroupName(eq(INSTANCE_GROUP_NAME_ACTIONABLE))).thenReturn(instanceGroup);

        List<InstanceMetaData> instancesHg = Stream.of(instancesActionableNotStopped, instancesActionableStopped)
                .flatMap(Collection::stream).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesActionableStopped = convertToCloudInstance(instancesActionableStopped);
        List<CloudInstance> cloudInstancesActionableAll = convertToCloudInstance(instancesHg);

        lenient().when(instanceMetaDataToCloudInstanceConverter.convert(any(), anyString(), any(StackAuthentication.class)))
                .thenReturn(cloudInstancesActionableStopped, cloudInstancesActionableAll);

        lenient().when(cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(any(), any(), any())).thenCallRealMethod();
    }

    private List<InstanceMetaData> generateInstances(int count, int startIndex,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus, String instanceGroupName) {
        List<InstanceMetaData> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
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

    private List<CloudInstance> convertToCloudInstance(List<InstanceMetaData> instances) {
        List<CloudInstance> cloudInstances = new LinkedList<>();
        for (InstanceMetaData im : instances) {
            CloudInstance cloudInstance = new CloudInstance(im.getInstanceId(), null, null, "blah", "blah");
            cloudInstances.add(cloudInstance);
        }
        return cloudInstances;
    }

    private List<CloudVmInstanceStatus> constructStartedCloudVmInstanceStatus(List<CloudInstance> cloudInstances, int numInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (int i = 0; i < numInstances; i++) {
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(
                    cloudInstances.get(i), com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED);
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> constructVmInstanceStatusWithTerminated(List<CloudInstance> cloudInstances, int numInstances, int numTerminated) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        int termCount = 0;
        for (int i = 0; i < numInstances; i++) {
            com.sequenceiq.cloudbreak.cloud.model.InstanceStatus expStatus;
            if (termCount++ < numTerminated) {
                expStatus = com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
            } else {
                expStatus = com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
            }
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(
                    cloudInstances.get(i), expStatus);
            cloudVmInstanceStatusList.add(cloudVmInstanceStatus);
        }
        return cloudVmInstanceStatusList;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
