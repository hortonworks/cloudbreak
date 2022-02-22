package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleActions.AbstractStopStartDownscaleActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class StopStartDownscaleActionsTest {

    private static final String INSTANCE_GROUP_NAME_ACTIONABLE = "compute";

    private static final String INSTANCE_GROUP_NAME_RANDOM = "other";

    private static final String SELECTOR = "dontcareforthistest";

    private static final Long STACK_ID = 100L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private StopStartDownscaleFlowService stopStartDownscaleFlowService;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    @InjectMocks
    private StopStartDownscaleActions underTest;

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
    void testDecommissionViaCmAction() throws Exception {
        AbstractStopStartDownscaleActions<StopStartDownscaleTriggerEvent> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleTriggerEvent>) underTest.decommissionViaCmAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        Set<Long> instanceIdsToRemove = instancesActionableStarted.stream().limit(5).map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);
        StopStartDownscaleTriggerEvent payload = new StopStartDownscaleTriggerEvent(SELECTOR, STACK_ID, INSTANCE_GROUP_NAME_ACTIONABLE,
                instanceIdsToRemove, ClusterManagerType.CLOUDERA_MANAGER);

        mockStackEtc();
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        verify(stopStartDownscaleFlowService).clusterDownscaleStarted(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(instanceIdsToRemove));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTDOWNSCALEDECOMMISSIONVIACMREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleDecommissionViaCMRequest.class);

        StopStartDownscaleDecommissionViaCMRequest req = (StopStartDownscaleDecommissionViaCMRequest) argumentCaptor.getValue();
        Assert.assertEquals(instanceIdsToRemove, req.getInstanceIdsToDecommission());
        Assert.assertEquals(INSTANCE_GROUP_NAME_ACTIONABLE, req.getHostGroupName());
    }

    @Test
    void testStopInstancesActionAllDecommissioned() throws Exception {
        AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult>) underTest.stopInstancesAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> expectedToBeStopped = instancesActionableStarted.stream().limit(5).collect(Collectors.toList());
        Set<Long> instanceIdsToRemove = expectedToBeStopped.stream().map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());

        Set<String> decommissionedHostsFqdns =
                expectedToBeStopped.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMRequest r =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME_ACTIONABLE, instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMResult payload = new StopStartDownscaleDecommissionViaCMResult(r, decommissionedHostsFqdns, Collections.emptyList());

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        List<CloudInstance> expectedCloudInstances = mockInstanceMetadataToCloudInstanceConverter(expectedToBeStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        verify(instanceMetaDataToCloudInstanceConverter).convert(eq(expectedToBeStopped), anyString(), any(StackAuthentication.class));
        verify(stopStartDownscaleFlowService).
                clusterDownscalingStoppingInstances(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(decommissionedHostsFqdns));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTDOWNSCALESTOPINSTANCESREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleStopInstancesRequest.class);

        StopStartDownscaleStopInstancesRequest req = (StopStartDownscaleStopInstancesRequest) argumentCaptor.getValue();
        Assert.assertEquals(expectedCloudInstances, req.getCloudInstancesToStop());
    }

    @Test
    void testStopInstancesActionNotAllDecommissioned() throws Exception {
        AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult>) underTest.stopInstancesAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        Set<Long> instanceIdsToRemove = instancesActionableStarted.stream().limit(6).map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());
        List<InstanceMetaData> expectedToBeStopped = instancesActionableStarted.stream().limit(4).collect(Collectors.toList());
        Set<String> decommissionedHostsFqdns =
                expectedToBeStopped.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        List<InstanceMetaData> notDecommissioned = instancesActionableStarted.subList(4, 6);
        List<String> notDecommissionedFqdns = notDecommissioned.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableList());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMRequest r =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME_ACTIONABLE, instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMResult payload = new StopStartDownscaleDecommissionViaCMResult(r, decommissionedHostsFqdns, notDecommissionedFqdns);

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        List<CloudInstance> expectedCloudInstances = mockInstanceMetadataToCloudInstanceConverter(expectedToBeStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        verify(instanceMetaDataToCloudInstanceConverter).convert(eq(expectedToBeStopped), anyString(), any(StackAuthentication.class));
        verify(stopStartDownscaleFlowService).logCouldNotDecommission(eq(STACK_ID), eq(notDecommissionedFqdns));
        verify(stopStartDownscaleFlowService).
                clusterDownscalingStoppingInstances(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(decommissionedHostsFqdns));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTDOWNSCALESTOPINSTANCESREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleStopInstancesRequest.class);

        StopStartDownscaleStopInstancesRequest req = (StopStartDownscaleStopInstancesRequest) argumentCaptor.getValue();
        Assert.assertEquals(expectedCloudInstances, req.getCloudInstancesToStop());
    }

    @Test
    void testStopInstancesActionNoneDecommissioned() throws Exception {
        // This behaviour is not ideal. Letting 0 stopped flow through the actual state machine instead of short-circuiting it.
        AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleDecommissionViaCMResult>) underTest.stopInstancesAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        Set<Long> instanceIdsToRemove = instancesActionableStarted.stream().limit(6).map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());
        List<InstanceMetaData> expectedToBeStopped = Collections.emptyList();
        Set<String> decommissionedHostsFqdns = Collections.emptySet();

        List<InstanceMetaData> notDecommissioned = instancesActionableStarted.subList(0, 6);
        List<String> notDecommissionedFqdns = notDecommissioned.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableList());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMRequest r =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME_ACTIONABLE, instanceIdsToRemove);
        StopStartDownscaleDecommissionViaCMResult payload = new StopStartDownscaleDecommissionViaCMResult(r, decommissionedHostsFqdns, notDecommissionedFqdns);

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        List<CloudInstance> expectedCloudInstances = mockInstanceMetadataToCloudInstanceConverter(expectedToBeStopped);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        verify(instanceMetaDataToCloudInstanceConverter).convert(eq(expectedToBeStopped), anyString(), any(StackAuthentication.class));
        verify(stopStartDownscaleFlowService).logCouldNotDecommission(eq(STACK_ID), eq(notDecommissionedFqdns));
        verify(stopStartDownscaleFlowService).
                clusterDownscalingStoppingInstances(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), eq(decommissionedHostsFqdns));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTARTDOWNSCALESTOPINSTANCESREQUEST", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleStopInstancesRequest.class);

        StopStartDownscaleStopInstancesRequest req = (StopStartDownscaleStopInstancesRequest) argumentCaptor.getValue();
        Assert.assertEquals(expectedCloudInstances, req.getCloudInstancesToStop());
    }

    @Test
    void testDownscaleFinishedActionAllStopped() throws Exception {
        AbstractStopStartDownscaleActions<StopStartDownscaleStopInstancesResult> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleStopInstancesResult>) underTest.downscaleFinishedAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> expectedToBeStopped = instancesActionableStarted.stream().limit(5).collect(Collectors.toList());
        Set<Long> instanceIdsToRemove = expectedToBeStopped.stream().map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        List<CloudInstance> expectedCloudInstances = mockInstanceMetadataToCloudInstanceConverter(expectedToBeStopped);
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = constructStoppedCloudVmInstanceStatus(expectedCloudInstances);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        StopStartDownscaleStopInstancesResult payload =
                new StopStartDownscaleStopInstancesResult(STACK_ID, mock(StopStartDownscaleStopInstancesRequest.class), cloudVmInstanceStatusList);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        ArgumentCaptor<List> listCap = ArgumentCaptor.forClass(List.class);
        verify(stopStartDownscaleFlowService).instancesStopped(eq(STACK_ID), listCap.capture());
        assertThat(new HashSet<>(listCap.getValue())).isEqualTo(new HashSet<>(expectedToBeStopped));
        verify(stopStartDownscaleFlowService).clusterDownscaleFinished(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), listCap.capture());
        assertThat(new HashSet<>(listCap.getValue())).isEqualTo(new HashSet<>(expectedToBeStopped));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_DOWNSCALE_FINALIZED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleStopInstancesResult.class);
    }

    @Test
    void testDownscaleFinishedActionNotAllStopped() throws Exception {
        AbstractStopStartDownscaleActions<StopStartDownscaleStopInstancesResult> action =
                (AbstractStopStartDownscaleActions<StopStartDownscaleStopInstancesResult>) underTest.downscaleFinishedAction();
        initActionPrivateFields(action);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        List<InstanceMetaData> expectedToBeStopped = instancesActionableStarted.stream().limit(5).collect(Collectors.toList());
        Set<Long> instanceIdsToRemove = expectedToBeStopped.stream().map(InstanceMetaData::getId).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleContext stopStartDownscaleContext = createContext(instanceIdsToRemove);

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        List<CloudInstance> expectedCloudInstances = mockInstanceMetadataToCloudInstanceConverter(expectedToBeStopped);
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = constructMixedCloudVmInstanceStatus(expectedCloudInstances);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        StopStartDownscaleStopInstancesResult payload =
                new StopStartDownscaleStopInstancesResult(STACK_ID, mock(StopStartDownscaleStopInstancesRequest.class), cloudVmInstanceStatusList);

        new AbstractActionTestSupport<>(action).doExecute(stopStartDownscaleContext, payload, Collections.emptyMap());

        ArgumentCaptor<List> listCap = ArgumentCaptor.forClass(List.class);
        verify(stopStartDownscaleFlowService).instancesStopped(eq(STACK_ID), listCap.capture());
        assertThat(new HashSet<>(listCap.getValue())).isEqualTo(new HashSet<>(expectedToBeStopped.subList(1, 5)));
        verify(stopStartDownscaleFlowService).clusterDownscaleFinished(eq(STACK_ID), eq(INSTANCE_GROUP_NAME_ACTIONABLE), listCap.capture());
        assertThat(new HashSet<>(listCap.getValue())).isEqualTo(new HashSet<>(expectedToBeStopped.subList(1, 5)));
        verify(stopStartDownscaleFlowService).logInstancesFailedToStop(eq(STACK_ID), eq(cloudVmInstanceStatusList.subList(0, 1)));
        verifyNoMoreInteractions(stopStartDownscaleFlowService);
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_DOWNSCALE_FINALIZED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StopStartDownscaleStopInstancesResult.class);
    }

    @Test
    void testDownscaleFailedAction() throws Exception {
        AbstractStackFailureAction<StopStartDownscaleState, StopStartUpscaleEvent> action =
                (AbstractStackFailureAction<StopStartDownscaleState, StopStartUpscaleEvent>) underTest.clusterDownscaleFailedAction();
        initActionPrivateFields(action);

        StackFailureContext stackFailureContext = new StackFailureContext(flowParameters, stackView);
        Exception exception = new Exception("FailedStop");
        StackFailureEvent stackFailureEvent = new StackFailureEvent(STACK_ID, exception);

        List<InstanceMetaData> instancesActionableStarted = generateInstances(10, 100, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesActionableNotStarted = generateInstances(5, 200, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_ACTIONABLE);
        List<InstanceMetaData> instancesRandomStarted = generateInstances(8, 300, InstanceStatus.SERVICES_HEALTHY, INSTANCE_GROUP_NAME_RANDOM);
        List<InstanceMetaData> instancesRandomNotStarted = generateInstances(3, 400, InstanceStatus.STOPPED, INSTANCE_GROUP_NAME_RANDOM);

        mockStackEtc(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(stackFailureContext, stackFailureEvent, Collections.emptyMap());
        verify(stopStartDownscaleFlowService).handleClusterDownscaleFailure(eq(STACK_ID), eq(exception));
        ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), argumentCaptor.capture());
        verify(eventBus).notify("STOPSTART_DOWNSCALE_FAIL_HANDLED_EVENT", event);
        assertThat(argumentCaptor.getValue()).isInstanceOf(StackEvent.class);
    }

    private StopStartDownscaleContext createContext(Set<Long> instanceIdsToRemove) {
        return new StopStartDownscaleContext(flowParameters, stack, stackView,
                cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME_ACTIONABLE,
                instanceIdsToRemove, ClusterManagerType.CLOUDERA_MANAGER);
    }

    private void mockStackEtc() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);

        lenient().when(stackView.getId()).thenReturn(STACK_ID);
    }

    private void mockStackEtc(List<InstanceMetaData> instancesActionableStarted, List<InstanceMetaData> instancesActionableNotStarted,
            List<InstanceMetaData> instancesRandomStarted, List<InstanceMetaData> instancesRandomNotStarted) {
        mockStackEtc();

        List<InstanceMetaData> combined =
                Stream.of(instancesActionableStarted, instancesActionableNotStarted, instancesRandomStarted, instancesRandomNotStarted)
                        .flatMap(Collection::stream).collect(Collectors.toList());

        lenient().when(stack.getNotDeletedAndNotZombieInstanceMetaDataList()).thenReturn(combined);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME_ACTIONABLE);
        instanceGroup.setInstanceMetaData(new HashSet<>(combined));
        lenient().when(stack.getInstanceGroupByInstanceGroupName(eq(INSTANCE_GROUP_NAME_ACTIONABLE))).thenReturn(instanceGroup);

        lenient().when(stackService.getPrivateIdsForHostNames(any(), any())).thenCallRealMethod();
        lenient().when(cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(any(), anyString(), any())).thenCallRealMethod();
    }

    private List<CloudInstance> mockInstanceMetadataToCloudInstanceConverter(List<InstanceMetaData> src) {
        List<CloudInstance> result = convertToCloudInstance(src);
        lenient().when(instanceMetaDataToCloudInstanceConverter.convert(eq(src), anyString(), any(StackAuthentication.class)))
                .thenReturn(result);
        return result;
    }

    private List<CloudInstance> convertToCloudInstance(List<InstanceMetaData> instances) {
        List<CloudInstance> cloudInstances = new LinkedList<>();
        for (InstanceMetaData im : instances) {
            CloudInstance cloudInstance = new CloudInstance(im.getInstanceId(), null, null, "blah", "blah");
            cloudInstances.add(cloudInstance);
        }
        return cloudInstances;
    }

    private List<InstanceMetaData> generateInstances(int count, int startIndex,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus, String instanceGroupName) {
        List<InstanceMetaData> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
        for (int i = 0; i < count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setPrivateId((long) (startIndex + i));
            instanceMetaData.setId((long) (startIndex + i));
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + (startIndex + i));
            instanceMetaData.setInstanceStatus(instanceStatus);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + (startIndex + i));
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private List<CloudVmInstanceStatus> constructStoppedCloudVmInstanceStatus(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudVmInstanceStatus> constructMixedCloudVmInstanceStatus(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (int i = 0; i < cloudInstances.size(); i++) {
            com.sequenceiq.cloudbreak.cloud.model.InstanceStatus expStatus;
            if (i == 0) {
                expStatus = com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
            } else {
                expStatus = com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
            }
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstances.get(i), expStatus));
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