package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    private static final Long PRIVATE_ID_1 = 101L;

    private static final Long PRIVATE_ID_2 = 102L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RuntimeVersionService runtimeVersionService;

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

    @InjectMocks
    private RollingVerticalScaleStartInstancesHandler underTest;

    private RollingVerticalScaleStartInstancesRequest request;

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    @BeforeEach
    void setUp() {
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instanceIds, GROUP_NAME);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_1, RollingVerticalScaleStatus.SCALED);
        rollingVerticalScaleResult.setStatus(INSTANCE_ID_2, RollingVerticalScaleStatus.SCALED);

        List<CloudInstance> cloudInstances = createCloudInstances(instanceIds);
        List<CloudResource> cloudResources = createCloudResources(instanceIds);
        request = new RollingVerticalScaleStartInstancesRequest(STACK_ID, cloudContext, cloudCredential,
                cloudResources, cloudInstances, rollingVerticalScaleResult);

        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform("AWS"), null);
        lenient().when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        lenient().when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(cloudConnector.instances()).thenReturn(instanceConnector);
        lenient().when(authenticator.authenticate(eq(cloudContext), eq(cloudCredential))).thenReturn(authenticatedContext);
        lenient().when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of("7.2.18"));
    }

    @Test
    void testWaitForServicesHealthySuccessPath() throws Exception {
        // GIVEN: instances start successfully, health check succeeds
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(createStartedInstanceStatuses());

        StackDto stackDto = mockStackDtoWithInstances();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        ClusterStatusService clusterStatusService = mockClusterStatusService(stackDto);
        ExtendedPollingResult successResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterStatusService.waitForHostHealthyServices(any(), any())).thenReturn(successResult);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN: services healthy called, no SERVICES_UNHEALTHY status set
        verify(rollingVerticalScaleService).updateInstancesToServicesHealthy(eq(STACK_ID), any());
        verify(rollingVerticalScaleService, never()).updateInstancesToServiceUnhealthy(any(), any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void testWaitForServicesHealthyWhenGetFailedInstancePrivateIdsReturnsNull() throws Exception {
        // GIVEN: instances start successfully; health check fails via the .failure() builder path
        // which does NOT set failedInstancePrivateIds — it is null. Before the fix, this caused NPE
        // on failedHostIds.contains(). After the fix, Optional.ofNullable().orElse(Set.of()) is used
        // and the flow continues gracefully.
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(createStartedInstanceStatuses());

        StackDto stackDto = mockStackDtoWithInstances();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        ClusterStatusService clusterStatusService = mockClusterStatusService(stackDto);
        // failure() builder leaves failedInstancePrivateIds = null
        ExtendedPollingResult failureWithNullIds = new ExtendedPollingResult.ExtendedPollingResultBuilder().failure().build();
        assertThat(failureWithNullIds.getFailedInstancePrivateIds()).isNull();
        when(clusterStatusService.waitForHostHealthyServices(any(), any())).thenReturn(failureWithNullIds);

        // WHEN — must not throw NPE
        underTest.accept(Event.wrap(request));

        // THEN: flow continues; updateInstancesToServiceUnhealthy called with an empty list (no instances matched)
        verify(rollingVerticalScaleService).updateInstancesToServiceUnhealthy(eq(STACK_ID), eq(GROUP_NAME), eq(List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void testWaitForServicesHealthyWithActualFailedInstances() throws Exception {
        // GIVEN: health check returns INSTANCE_ID_1 as failed (via its privateId)
        when(instanceConnector.startWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(createStartedInstanceStatuses());

        StackDto stackDto = mockStackDtoWithInstances();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        ClusterStatusService clusterStatusService = mockClusterStatusService(stackDto);
        ExtendedPollingResult failureResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .withFailedInstanceIds(Set.of(PRIVATE_ID_1))
                .build();
        when(clusterStatusService.waitForHostHealthyServices(any(), any())).thenReturn(failureResult);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN: INSTANCE_ID_1 marked SERVICES_UNHEALTHY; flow continues
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> failedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rollingVerticalScaleService).updateInstancesToServiceUnhealthy(eq(STACK_ID), eq(GROUP_NAME), failedCaptor.capture());
        assertThat(failedCaptor.getValue()).containsExactly(INSTANCE_ID_1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStartInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)), eventCaptor.capture());
        RollingVerticalScaleResult result = eventCaptor.getValue().getData().getRollingVerticalScaleResult();
        assertThat(result.getStatus(INSTANCE_ID_1).getStatus()).isEqualTo(RollingVerticalScaleStatus.SERVICES_UNHEALTHY);
    }

    private StackDto mockStackDtoWithInstances() {
        StackDto stackDto = mock(StackDto.class);
        InstanceMetadataView view1 = mock(InstanceMetadataView.class);
        when(view1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(view1.getPrivateId()).thenReturn(PRIVATE_ID_1);
        when(view1.getDiscoveryFQDN()).thenReturn("host1.example.com");
        InstanceMetadataView view2 = mock(InstanceMetadataView.class);
        when(view2.getInstanceId()).thenReturn(INSTANCE_ID_2);
        lenient().when(view2.getPrivateId()).thenReturn(PRIVATE_ID_2);
        when(view2.getDiscoveryFQDN()).thenReturn("host2.example.com");
        lenient().when(stackDto.getAllAvailableInstances()).thenReturn(List.of(view1, view2));
        lenient().when(stackDto.getCluster()).thenReturn(null);
        return stackDto;
    }

    private ClusterStatusService mockClusterStatusService(StackDto stackDto) {
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        return clusterStatusService;
    }

    private List<CloudVmInstanceStatus> createStartedInstanceStatuses() {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        for (String instanceId : List.of(INSTANCE_ID_1, INSTANCE_ID_2)) {
            CloudInstance instance = new CloudInstance(instanceId, null, null, "az1", null);
            statuses.add(new CloudVmInstanceStatus(instance, InstanceStatus.STARTED));
        }
        return statuses;
    }

    private List<CloudInstance> createCloudInstances(List<String> instanceIds) {
        List<CloudInstance> instances = new ArrayList<>();
        for (String instanceId : instanceIds) {
            instances.add(new CloudInstance(instanceId, null, null, "az1", null));
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
}
