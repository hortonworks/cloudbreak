package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesResult;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleStopInstancesHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "worker";

    private static final String INSTANCE_ID_1 = "instance-1";

    private static final String INSTANCE_ID_2 = "instance-2";

    private static final String TARGET_INSTANCE_TYPE = "m5.xlarge";

    private static final String CURRENT_INSTANCE_TYPE = "m5.large";

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

    @InjectMocks
    private RollingVerticalScaleStopInstancesHandler underTest;

    private RollingVerticalScaleStopInstancesRequest request;

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    @BeforeEach
    void setUp() {
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instanceIds, GROUP_NAME);

        List<CloudInstance> cloudInstances = createCloudInstances(instanceIds);
        List<CloudResource> cloudResources = createCloudResources(instanceIds);
        Map<String, String> instanceTypeByInstanceId = Map.of(INSTANCE_ID_1, CURRENT_INSTANCE_TYPE, INSTANCE_ID_2, CURRENT_INSTANCE_TYPE);
        request = new RollingVerticalScaleStopInstancesRequest(STACK_ID, cloudContext, cloudCredential, cloudResources,
                cloudInstances, TARGET_INSTANCE_TYPE, rollingVerticalScaleResult, instanceTypeByInstanceId);

        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform("AWS"), null);
        lenient().when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
        lenient().when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(cloudConnector.instances()).thenReturn(instanceConnector);
        lenient().when(authenticator.authenticate(eq(cloudContext), eq(cloudCredential))).thenReturn(authenticatedContext);
    }

    @Test
    void testAcceptSuccess() throws Exception {
        // GIVEN — instances have a different type than the target, so both are included for scaling
        List<CloudVmInstanceStatus> stoppedStatuses = createStoppedInstanceStatuses();
        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(stoppedStatuses);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(rollingVerticalScaleService).stopInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));
        verify(instanceConnector).stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(rollingVerticalScaleService).finishStopInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));
        verify(rollingVerticalScaleService).failedToStopInstance(eq(STACK_ID), anyList(), eq(GROUP_NAME), eq(""));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStopInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(eventCaptor.getValue().getData().getRollingVerticalScaleResult()).isNotNull();
    }

    @Test
    void testAcceptWithInstancesAlreadyAtTargetType() throws Exception {
        // GIVEN — instances already have the target type; they should be skipped (marked SUCCESS) and not stopped
        Map<String, String> alreadyAtTargetMap = Map.of(INSTANCE_ID_1, TARGET_INSTANCE_TYPE, INSTANCE_ID_2, TARGET_INSTANCE_TYPE);
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        RollingVerticalScaleStopInstancesRequest requestAtTarget = new RollingVerticalScaleStopInstancesRequest(
                STACK_ID, cloudContext, cloudCredential, createCloudResources(instanceIds),
                createCloudInstances(instanceIds), TARGET_INSTANCE_TYPE,
                new RollingVerticalScaleResult(instanceIds, GROUP_NAME), alreadyAtTargetMap);
        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), eq(List.of()), eq(1_200_000L)))
                .thenReturn(List.of());

        // WHEN
        underTest.accept(Event.wrap(requestAtTarget));

        // THEN
        verify(rollingVerticalScaleService).stopInstances(eq(STACK_ID), eq(List.of()), eq(GROUP_NAME));
        verify(instanceConnector).stopWithLimitedRetry(eq(authenticatedContext), anyList(), eq(List.of()), eq(1_200_000L));
        verify(rollingVerticalScaleService).finishStopInstances(eq(STACK_ID), eq(List.of()), eq(GROUP_NAME));
        verify(rollingVerticalScaleService).failedToStopInstance(eq(STACK_ID), eq(List.of()), eq(GROUP_NAME), eq(""));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStopInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_1).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SUCCESS);
        assertThat(eventCaptor.getValue().getData().getRollingVerticalScaleResult().getStatus(INSTANCE_ID_2).getStatus())
                .isEqualTo(RollingVerticalScaleStatus.SUCCESS);
    }

    @Test
    void testGetTargetCloudInstancesWhenProviderInstanceTypeIsNullInstanceIncludedForScaling() {
        // GIVEN — providerInstanceType was null in the action (filtered out); empty map passed in request
        // instances should be conservatively included for scaling, not skipped
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        RollingVerticalScaleStopInstancesRequest requestWithNullTypes = new RollingVerticalScaleStopInstancesRequest(
                STACK_ID, cloudContext, cloudCredential, createCloudResources(instanceIds),
                createCloudInstances(instanceIds), TARGET_INSTANCE_TYPE,
                new RollingVerticalScaleResult(instanceIds, GROUP_NAME), Map.of());

        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(createStoppedInstanceStatuses());

        // WHEN
        underTest.accept(Event.wrap(requestWithNullTypes));

        // THEN — both instances should be included for scaling (stop called with both)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> stopCaptor = ArgumentCaptor.forClass(List.class);
        verify(rollingVerticalScaleService).stopInstances(eq(STACK_ID), stopCaptor.capture(), eq(GROUP_NAME));
        assertThat(stopCaptor.getValue()).containsExactlyInAnyOrder(INSTANCE_ID_1, INSTANCE_ID_2);
    }

    @Test
    void testGetTargetCloudInstancesWhenInstanceNotInDbInstanceIncludedForScaling() {
        // GIVEN — empty map (action found no metadata for these instances)
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        RollingVerticalScaleStopInstancesRequest requestWithEmptyMap = new RollingVerticalScaleStopInstancesRequest(
                STACK_ID, cloudContext, cloudCredential, createCloudResources(instanceIds),
                createCloudInstances(instanceIds), TARGET_INSTANCE_TYPE,
                new RollingVerticalScaleResult(instanceIds, GROUP_NAME), Map.of());

        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenReturn(createStoppedInstanceStatuses());

        // WHEN
        underTest.accept(Event.wrap(requestWithEmptyMap));

        // THEN — both instances should be included for scaling
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> stopCaptor = ArgumentCaptor.forClass(List.class);
        verify(rollingVerticalScaleService).stopInstances(eq(STACK_ID), stopCaptor.capture(), eq(GROUP_NAME));
        assertThat(stopCaptor.getValue()).containsExactlyInAnyOrder(INSTANCE_ID_1, INSTANCE_ID_2);
    }

    @Test
    void testAcceptWithPollerStoppedException() throws Exception {
        // GIVEN
        PollerStoppedException pollerException = new PollerStoppedException("Timeout");
        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenThrow(pollerException);
        List<CloudVmInstanceStatus> checkStatuses = createStoppedInstanceStatuses();
        when(instanceConnector.checkWithoutRetry(eq(authenticatedContext), anyList()))
                .thenReturn(checkStatuses);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(instanceConnector).checkWithoutRetry(eq(authenticatedContext), anyList());
        verify(rollingVerticalScaleService).finishStopInstances(eq(STACK_ID), anyList(), eq(GROUP_NAME));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStopInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void testAcceptWithException() throws Exception {
        // GIVEN
        RuntimeException exception = new RuntimeException("Stop failed");
        when(instanceConnector.stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L)))
                .thenThrow(exception);
        when(instanceConnector.checkWithoutRetry(eq(authenticatedContext), anyList()))
                .thenThrow(new RuntimeException("Check also failed"));

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(instanceConnector).stopWithLimitedRetry(eq(authenticatedContext), anyList(), anyList(), eq(1_200_000L));
        verify(instanceConnector).checkWithoutRetry(eq(authenticatedContext), anyList());
        verify(rollingVerticalScaleService).failedToStopInstance(eq(STACK_ID), anyList(), eq(GROUP_NAME), eq("Error while attempting to stop instances"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event<RollingVerticalScaleStopInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesResult.class)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(eventCaptor.getValue().getData().getRollingVerticalScaleResult()).isNotNull();
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
            resources.add(CloudResource.builder()
                    .withType(ResourceType.AWS_INSTANCE)
                    .withInstanceId(instanceId)
                    .withName(instanceId)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(new HashMap<>())
                    .build());
        }
        return resources;
    }

    private List<CloudVmInstanceStatus> createStoppedInstanceStatuses() {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        for (String instanceId : List.of(INSTANCE_ID_1, INSTANCE_ID_2)) {
            CloudInstance instance = new CloudInstance(instanceId, null, null, "az1", null);
            statuses.add(new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED));
        }
        return statuses;
    }
}
