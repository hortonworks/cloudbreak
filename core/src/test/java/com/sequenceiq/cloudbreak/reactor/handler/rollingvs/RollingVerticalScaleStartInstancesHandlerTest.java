package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleStartInstancesHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "worker";

    private static final String INSTANCE_ID_1 = "instance-1";

    private static final String INSTANCE_ID_2 = "instance-2";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private InstanceConnector instanceConnector;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @InjectMocks
    private RollingVerticalScaleStartInstancesHandler underTest;

    private RollingVerticalScaleStartInstancesRequest request;

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    private InstanceMetadataView instanceMetadata1;

    private InstanceMetadataView instanceMetadata2;

    private Optional<String> runtimeVersion;

    @BeforeEach
    void setUp() throws ClusterClientInitException {
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instanceIds, GROUP_NAME);

        List<CloudInstance> cloudInstances = createCloudInstances(instanceIds);
        List<CloudResource> cloudResources = createCloudResources(instanceIds);
        request = new RollingVerticalScaleStartInstancesRequest(STACK_ID, cloudContext, cloudCredential, cloudResources,
                cloudInstances, rollingVerticalScaleResult);

        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform("AWS"), null);
        lenient().when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
        lenient().when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(cloudConnector.instances()).thenReturn(instanceConnector);
        lenient().when(authenticator.authenticate(eq(cloudContext), eq(cloudCredential))).thenReturn(authenticatedContext);
        instanceMetadata1 = createInstanceMetadataView(INSTANCE_ID_1, "host1.example.com", 1L);
        instanceMetadata2 = createInstanceMetadataView(INSTANCE_ID_2, "host2.example.com", 2L);
        lenient().when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getAllAvailableInstances()).thenReturn(List.of(instanceMetadata1, instanceMetadata2));
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getId()).thenReturn(99L);
        runtimeVersion = Optional.of("7.3.1");
        lenient().when(runtimeVersionService.getRuntimeVersion(99L)).thenReturn(runtimeVersion);
        lenient().when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        ExtendedPollingResult successPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        lenient().when(clusterStatusService.waitForHostHealthyServices(anySet(), any())).thenReturn(successPollingResult);
    }

    @Test
    void testAcceptSuccess() throws Exception {
        // GIVEN
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SCALED);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SCALED);

        List<CloudVmInstanceStatus> startedStatuses = createStartedInstanceStatuses();
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(startedStatuses);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(rollingVerticalScaleService).finishStartInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));
        verify(rollingVerticalScaleService, never()).failedStartInstances(anyLong(), anyList(), any(), any());
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2))));
        verify(clusterStatusService).waitForHostHealthyServices(argThat(set -> set.size() == 2), eq(runtimeVersion));
        verify(rollingVerticalScaleService).updateInstancesToServicesHealthy(eq(STACK_ID), argThat(set -> set.size() == 2));

        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = createResultEventCaptor();
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleStartInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult()).isNotNull();
        // Verify that instances are marked as SUCCESS after starting
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_1).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SUCCESS);
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_2).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SUCCESS);
    }

    @Test
    void testAcceptWithPartialFailures() throws Exception {
        // GIVEN
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SCALED);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SCALED);

        List<CloudVmInstanceStatus> partialStatuses = createPartialInstanceStatuses();
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(partialStatuses);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(rollingVerticalScaleService).finishStartInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));
        verify(rollingVerticalScaleService).failedStartInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME), eq(""));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 1 && ids.contains(INSTANCE_ID_1)));
        verify(clusterStatusService).waitForHostHealthyServices(argThat(set -> set.size() == 1), eq(runtimeVersion));
        verify(rollingVerticalScaleService).updateInstancesToServicesHealthy(eq(STACK_ID), argThat(set -> set.size() == 1));

        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = createResultEventCaptor();
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleStartInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
        // Verify that INSTANCE_ID_1 is marked as SUCCESS (it started successfully)
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_1).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SUCCESS);
        // INSTANCE_ID_2 failed to start, but the handler only updates status for instances that are in the returned status list
        // Since it's not in the list (failed), it remains with its previous status (SCALED) and is not updated to RESTART_FAILED
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_2).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SCALED);
    }

    @Test
    void testAcceptWithPollerStoppedException() throws Exception {
        // GIVEN
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SCALED);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SCALED);

        PollerStoppedException pollerException = new PollerStoppedException("Timeout");
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenThrow(pollerException);
        List<CloudVmInstanceStatus> checkStatuses = createStartedInstanceStatuses();
        when(instanceConnector.checkWithoutRetry(eq(authenticatedContext), anyList()))
                .thenReturn(checkStatuses);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(instanceConnector).checkWithoutRetry(eq(authenticatedContext), anyList());
        verify(rollingVerticalScaleService).finishStartInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2))));
        verify(clusterStatusService).waitForHostHealthyServices(argThat(set -> set.size() == 2), eq(runtimeVersion));
        verify(rollingVerticalScaleService).updateInstancesToServicesHealthy(eq(STACK_ID), argThat(set -> set.size() == 2));

        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = createResultEventCaptor();
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleStartInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void testAcceptWithException() throws Exception {
        // GIVEN
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SCALED);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SCALED);

        RuntimeException exception = new RuntimeException("Start failed");
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenThrow(exception);
        when(instanceConnector.checkWithoutRetry(eq(authenticatedContext), anyList()))
                .thenThrow(new RuntimeException("Check also failed"));

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(instanceConnector).checkWithoutRetry(eq(authenticatedContext), anyList());
        // When checkWithoutRetry also fails, the handler wraps the error with "Error while attempting to start instances"
        verify(rollingVerticalScaleService).failedStartInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME), eq("Error while attempting to start instances"));
        verify(rollingVerticalScaleService, never()).waitingForServicesHealthy(anyLong(), any(), any());
        verify(clusterStatusService, never()).waitForHostHealthyServices(anySet(), any());

        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = createResultEventCaptor();
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleStartInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult()).isNotNull();
    }

    @Test
    void testWaitForServicesHealthyUsesRuntimeVersionAndUpdatesStatuses() throws ClusterClientInitException {
        Optional<String> runtimeVersion = Optional.of("7.4.0");
        when(runtimeVersionService.getRuntimeVersion(99L)).thenReturn(runtimeVersion);

        ReflectionTestUtils.invokeMethod(underTest, "waitForServicesHealthy", STACK_ID, List.of(INSTANCE_ID_1, INSTANCE_ID_2), rollingVerticalScaleResult);

        verify(runtimeVersionService).getRuntimeVersion(99L);
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2))));
        verify(clusterStatusService).waitForHostHealthyServices(argThat(set -> set.size() == 2), eq(runtimeVersion));
        verify(rollingVerticalScaleService).updateInstancesToServicesHealthy(eq(STACK_ID),
                argThat(set -> set.size() == 2));
    }

    @Test
    void testWaitForServicesHealthySkipsWhenNoInstanceHasFqdn() throws ClusterClientInitException {
        InstanceMetadataView instanceWithoutFqdn = createInstanceMetadataView(INSTANCE_ID_1, null, 10L);
        when(stackDto.getAllAvailableInstances()).thenReturn(List.of(instanceWithoutFqdn));

        ReflectionTestUtils.invokeMethod(underTest, "waitForServicesHealthy", STACK_ID, List.of(INSTANCE_ID_1), rollingVerticalScaleResult);

        verify(clusterStatusService, never()).waitForHostHealthyServices(anySet(), any());
        verify(rollingVerticalScaleService, never()).updateInstancesToServicesHealthy(anyLong(), anySet());
        verify(rollingVerticalScaleService, never()).waitingForServicesHealthy(anyLong(), any(), any());
    }

    @Test
    void testWaitForServicesHealthyMarksInstancesWhenServicesStayUnhealthy() throws ClusterClientInitException {
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SUCCESS);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SUCCESS);
        ExtendedPollingResult failedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .withPayload(Set.of(1L))
                .build();
        when(clusterStatusService.waitForHostHealthyServices(anySet(), any())).thenReturn(failedPollingResult);

        ReflectionTestUtils.invokeMethod(underTest, "waitForServicesHealthy", STACK_ID, List.of(INSTANCE_ID_1, INSTANCE_ID_2), rollingVerticalScaleResult);

        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2))));
        verify(rollingVerticalScaleService).updateInstancesToServiceUnhealthy(eq(STACK_ID), eq(GROUP_NAME),
                argThat(ids -> ids.size() == 1 && ids.contains(INSTANCE_ID_1)));
        verify(rollingVerticalScaleService, never()).updateInstancesToServicesHealthy(anyLong(), anySet());
        assertThat(rollingVerticalScaleResult.getStatus(INSTANCE_ID_1).getStatus()).isEqualTo(RollingVerticalScaleStatus.SERVICES_UNHEALTHY);
        assertThat(rollingVerticalScaleResult.getStatus(INSTANCE_ID_2).getStatus()).isEqualTo(RollingVerticalScaleStatus.SUCCESS);
    }

    private List<CloudInstance> createCloudInstances(List<String> instanceIds) {
        List<CloudInstance> instances = new ArrayList<>();
        for (String instanceId : instanceIds) {
            CloudInstance instance = new CloudInstance(instanceId, null, null, "az1", null);
            instances.add(instance);
        }
        return instances;
    }

    private List<CloudResource> createCloudResources(List<String> instanceIds) {
        List<CloudResource> resources = new ArrayList<>();
        for (String instanceId : instanceIds) {
            CloudResource resource = CloudResource.builder()
                    .withType(ResourceType.AWS_INSTANCE)
                    .withInstanceId(instanceId)
                    .withName(instanceId)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(new HashMap<>())
                    .build();
            resources.add(resource);
        }
        return resources;
    }

    private List<CloudVmInstanceStatus> createStartedInstanceStatuses() {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        for (String instanceId : List.of(INSTANCE_ID_1, INSTANCE_ID_2)) {
            CloudInstance instance = new CloudInstance(instanceId, null, null, "az1", null);
            CloudVmInstanceStatus status = new CloudVmInstanceStatus(instance, InstanceStatus.STARTED);
            statuses.add(status);
        }
        return statuses;
    }

    private List<CloudVmInstanceStatus> createPartialInstanceStatuses() {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        CloudInstance instance1 = new CloudInstance(INSTANCE_ID_1, null, null, "az1", null);
        statuses.add(new CloudVmInstanceStatus(instance1, InstanceStatus.STARTED));
        // INSTANCE_ID_2 is missing, so it will be considered as failed
        return statuses;
    }

    private InstanceMetadataView createInstanceMetadataView(String instanceId, String fqdn, Long privateId) {
        InstanceMetadataView metadataView = mock(InstanceMetadataView.class);
        lenient().when(metadataView.getInstanceId()).thenReturn(instanceId);
        lenient().when(metadataView.getDiscoveryFQDN()).thenReturn(fqdn);
        lenient().when(metadataView.getPrivateId()).thenReturn(privateId);
        return metadataView;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> createResultEventCaptor() {
        return ArgumentCaptor.forClass((Class<Event<RollingVerticalScaleStartInstancesResult>>) (Class<?>) Event.class);
    }
}

