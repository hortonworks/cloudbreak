package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesResult;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleInstancesHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "worker";

    private static final String INSTANCE_ID_1 = "instance-1";

    private static final String INSTANCE_ID_2 = "instance-2";

    private static final String REQUESTED_INSTANCE_TYPE = "m5.xlarge";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUpscaleService stackUpscaleService;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private CoreVerticalScaleService coreVerticalScaleService;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private MetadataCollector metadataCollector;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private RollingVerticalScaleInstancesHandler underTest;

    private RollingVerticalScaleInstancesRequest request;

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    @BeforeEach
    void setUp() {
        List<String> instanceIds = List.of(INSTANCE_ID_1, INSTANCE_ID_2);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instanceIds, GROUP_NAME);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setGroup(GROUP_NAME);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType(REQUESTED_INSTANCE_TYPE);
        stackVerticalScaleV4Request.setTemplate(template);

        List<CloudResource> cloudResources = createCloudResources(instanceIds);
        request = new RollingVerticalScaleInstancesRequest(STACK_ID, cloudContext, cloudCredential, null, cloudResources,
                stackVerticalScaleV4Request, rollingVerticalScaleResult);

        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform("AWS"), null);
        lenient().when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
        lenient().when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        lenient().when(cloudConnector.metadata()).thenReturn(metadataCollector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(authenticator.authenticate(eq(cloudContext), eq(cloudCredential))).thenReturn(authenticatedContext);
    }

    @Test
    void testAcceptSuccess() throws Exception {
        // GIVEN
        List<CloudResourceStatus> resourceStatuses = createSuccessfulResourceStatuses();
        Map<String, String> instanceTypeMetadata = createInstanceTypeMetadata(REQUESTED_INSTANCE_TYPE);

        when(stackUpscaleService.verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME)))
                .thenReturn(resourceStatuses);
        when(metadataCollector.collectInstanceTypes(any(), anyList()))
                .thenReturn(new InstanceTypeMetadata(instanceTypeMetadata));
        when(stackUpscaleService.getInstanceStorageInfo(any(), anyString(), any()))
                .thenReturn(new InstanceStoreMetadata());
        lenient().doNothing().when(coreVerticalScaleService).updateTemplateWithVerticalScaleInformation(any(), any(), anyInt(), anyInt());

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(stackUpscaleService).verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME));
        verify(metadataCollector).collectInstanceTypes(any(), anyList());
        verify(rollingVerticalScaleService).finishVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class));
        // failedVerticalScaleInstances is always called, even with empty list when all instances succeed
        verify(rollingVerticalScaleService).failedVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class), eq(""));

        ArgumentCaptor<Event<RollingVerticalScaleInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult()).isNotNull();
    }

    @Test
    void testAcceptWithFailedInstances() throws Exception {
        // GIVEN
        List<CloudResourceStatus> resourceStatuses = createMixedResourceStatuses();
        Map<String, String> instanceTypeMetadata = createInstanceTypeMetadata(REQUESTED_INSTANCE_TYPE);
        instanceTypeMetadata.put(INSTANCE_ID_2, "m5.large");

        when(stackUpscaleService.verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME)))
                .thenReturn(resourceStatuses);
        when(metadataCollector.collectInstanceTypes(any(), anyList()))
                .thenReturn(new InstanceTypeMetadata(instanceTypeMetadata));
        when(stackUpscaleService.getInstanceStorageInfo(any(), anyString(), any()))
                .thenReturn(new InstanceStoreMetadata());
        lenient().doNothing().when(coreVerticalScaleService).updateTemplateWithVerticalScaleInformation(any(), any(), anyInt(), anyInt());

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(stackUpscaleService).verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME));
        verify(metadataCollector).collectInstanceTypes(any(), anyList());
        verify(rollingVerticalScaleService).finishVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class));
        verify(rollingVerticalScaleService).failedVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class), anyString());

        ArgumentCaptor<Event<RollingVerticalScaleInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void testAcceptWithException() throws Exception {
        // GIVEN
        Exception exception = new Exception("Scale failed");
        when(stackUpscaleService.verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME)))
                .thenThrow(exception);

        // WHEN
        underTest.accept(Event.wrap(request));

        // THEN
        verify(stackUpscaleService).verticalScale(any(), eq(request), eq(cloudConnector), eq(GROUP_NAME));
        verify(rollingVerticalScaleService).failedVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class), eq("Scale failed"));
        verify(rollingVerticalScaleService, never()).finishVerticalScaleInstances(eq(STACK_ID), anyList(), any(StackVerticalScaleV4Request.class));

        ArgumentCaptor<Event<RollingVerticalScaleInstancesResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RollingVerticalScaleInstancesResult.class)), eventCaptor.capture());
        Event<RollingVerticalScaleInstancesResult> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getData().getResourceId()).isEqualTo(STACK_ID);
        assertThat(capturedEvent.getData().getRollingVerticalScaleResult()).isNotNull();
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

    private List<CloudResourceStatus> createSuccessfulResourceStatuses() {
        List<CloudResourceStatus> statuses = new ArrayList<>();
        for (String instanceId : List.of(INSTANCE_ID_1, INSTANCE_ID_2)) {
            CloudResource resource = CloudResource.builder()
                    .withType(ResourceType.AWS_INSTANCE)
                    .withInstanceId(instanceId)
                    .withName(instanceId)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(new HashMap<>())
                    .build();
            CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.UPDATED);
            statuses.add(status);
        }
        return statuses;
    }

    private List<CloudResourceStatus> createMixedResourceStatuses() {
        List<CloudResourceStatus> statuses = new ArrayList<>();
        CloudResource resource1 = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withInstanceId(INSTANCE_ID_1)
                .withName(INSTANCE_ID_1)
                .withStatus(CommonStatus.CREATED)
                .withParameters(new HashMap<>())
                .build();
        statuses.add(new CloudResourceStatus(resource1, ResourceStatus.UPDATED));

        CloudResource resource2 = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withInstanceId(INSTANCE_ID_2)
                .withName(INSTANCE_ID_2)
                .withStatus(CommonStatus.CREATED)
                .withParameters(new HashMap<>())
                .build();
        statuses.add(new CloudResourceStatus(resource2, ResourceStatus.FAILED, "Failed to scale"));
        return statuses;
    }

    private Map<String, String> createInstanceTypeMetadata(String instanceType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(INSTANCE_ID_1, instanceType);
        metadata.put(INSTANCE_ID_2, instanceType);
        return metadata;
    }
}
