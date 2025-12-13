package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsContextService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsComputeResourceServiceTest {

    @Mock
    private AwsContextBuilder contextBuilder;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private AwsContextService awsContextService;

    @Mock
    private GroupResourceService groupResourceService;

    @InjectMocks
    private AwsComputeResourceService underTest;

    @Test
    void deleteComputeResourcesWhenNoAwsNativeResourcesAffected() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<CloudResource> cloudResources = List.of(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK));
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);

        underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources);

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
    }

    @Test
    void deleteComputeResourcesWhenNoAwsNativeResourcesAffectedAndComputeResourceDeletionFails() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<CloudResource> cloudResources = List.of(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK));
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);
        when(computeResourceService.deleteResources(awsContext, authenticatedContext, cloudResources, false, true))
                .thenThrow(new RuntimeException("Uh-Oh something bad happened"));

        assertThrows(RuntimeException.class, () -> underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources));

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = ResourceType.class, names = { "AWS_INSTANCE", "AWS_CLOUD_WATCH" })
    void deleteComputeResourcesWhenAwsNativeComputeResourcesAffected(ResourceType awsNativeResourceType) {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = new CloudContext.Builder()
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(AwsConstants.AWS_DEFAULT_VARIANT)
                .build();
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudResource nativeResource = getCloudResource("nativeResource", awsNativeResourceType);
        List<CloudResource> cloudResources = List.of(
                getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK),
                nativeResource);
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);
        AwsContext awsNativeTerminationContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(false))).thenReturn(awsNativeTerminationContext);
        List<CloudResourceStatus> cloudResourceStatuses = List.of(
                new CloudResourceStatus(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK), ResourceStatus.DELETED));
        when(computeResourceService.deleteResources(awsContext, authenticatedContext, cloudResources, false, true))
                .thenReturn(cloudResourceStatuses);

        underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources);

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
        List<CloudResource> nonDeletedNativeResources = List.of(nativeResource);
        ArgumentCaptor<AuthenticatedContext> acCaptor = ArgumentCaptor.forClass(AuthenticatedContext.class);
        verify(computeResourceService, times(1))
                .deleteResources(eq(awsNativeTerminationContext), acCaptor.capture(), eq(nonDeletedNativeResources), eq(false), eq(true));
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), acCaptor.getValue().getCloudContext().getVariant());
    }

    @Test
    void deleteComputeResourcesWhenAwsNativeGroupResourcesAffected() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = new CloudContext.Builder()
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(AwsConstants.AWS_DEFAULT_VARIANT)
                .build();
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudResource nativeResource = getCloudResource("nativeResource", ResourceType.AWS_SECURITY_GROUP);
        List<CloudResource> cloudResources = List.of(
                getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK),
                getCloudResource("volumeset", ResourceType.AWS_VOLUMESET),
                nativeResource);
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);
        AwsContext awsNativeTerminationContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(false))).thenReturn(awsNativeTerminationContext);
        List<CloudResourceStatus> cloudResourceStatuses = List.of(
                new CloudResourceStatus(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK), ResourceStatus.DELETED),
                new CloudResourceStatus(getCloudResource("volumeset", ResourceType.AWS_VOLUMESET), ResourceStatus.CREATED));
        when(computeResourceService.deleteResources(awsContext, authenticatedContext, cloudResources, false, true))
                .thenReturn(cloudResourceStatuses);

        underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources);

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
        List<CloudResource> nonDeletedNativeResources = List.of(nativeResource);
        ArgumentCaptor<AuthenticatedContext> acCaptor = ArgumentCaptor.forClass(AuthenticatedContext.class);
        verify(groupResourceService, times(1))
                .deleteResources(eq(awsNativeTerminationContext), acCaptor.capture(), eq(nonDeletedNativeResources), any(), eq(false));
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), acCaptor.getValue().getCloudContext().getVariant());
    }

    @ParameterizedTest
    @EnumSource(value = ResourceType.class, names = { "AWS_INSTANCE", "AWS_CLOUD_WATCH" })
    void deleteComputeResourcesWhenAwsNativeResourcesAffectedAndAwsNativeComputeResourcesDeletionFails(ResourceType awsNativeResourceType) {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = new CloudContext.Builder()
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(AwsConstants.AWS_DEFAULT_VARIANT)
                .build();
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudResource nativeResource = getCloudResource("nativeResource", awsNativeResourceType);
        List<CloudResource> cloudResources = List.of(
                getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK),
                nativeResource);
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);
        AwsContext awsNativeTerminationContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(false))).thenReturn(awsNativeTerminationContext);
        List<CloudResourceStatus> cloudResourceStatuses = List.of(
                new CloudResourceStatus(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK), ResourceStatus.DELETED));
        when(computeResourceService.deleteResources(awsContext, authenticatedContext, cloudResources, false, true))
                .thenReturn(cloudResourceStatuses);
        List<CloudResource> nonDeletedNativeResources = List.of(nativeResource);
        when(computeResourceService.deleteResources(eq(awsContext), any(), eq(nonDeletedNativeResources), eq(false), eq(true)))
                .thenThrow(new RuntimeException("test"));

        assertThrows(RuntimeException.class, () -> underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources));

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
        ArgumentCaptor<AuthenticatedContext> acCaptor = ArgumentCaptor.forClass(AuthenticatedContext.class);
        verify(computeResourceService, times(1))
                .deleteResources(eq(awsNativeTerminationContext), acCaptor.capture(), eq(nonDeletedNativeResources), eq(false), eq(true));
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), acCaptor.getValue().getCloudContext().getVariant());
    }

    @Test
    void deleteComputeResourcesWhenAwsNativeResourcesAffectedAndAwsNativeGroupResourcesDeletionFails() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = new CloudContext.Builder()
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(AwsConstants.AWS_DEFAULT_VARIANT)
                .build();
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudResource nativeGroupResource = getCloudResource("nativeResource", ResourceType.AWS_SECURITY_GROUP);
        CloudResource nativeComputeResource = getCloudResource("nativeResource", ResourceType.AWS_INSTANCE);
        List<CloudResource> cloudResources = List.of(
                getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK),
                nativeGroupResource,
                nativeComputeResource);
        AwsContext awsContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(true))).thenReturn(awsContext);
        AwsContext awsNativeTerminationContext = mock(AwsContext.class);
        when(contextBuilder.contextInit(any(), any(), any(), eq(false))).thenReturn(awsNativeTerminationContext);
        List<CloudResourceStatus> cloudResourceStatuses = List.of(
                new CloudResourceStatus(getCloudResource("cfstack", ResourceType.CLOUDFORMATION_STACK), ResourceStatus.DELETED));
        when(computeResourceService.deleteResources(awsContext, authenticatedContext, cloudResources, false, true))
                .thenReturn(cloudResourceStatuses);
        List<CloudResource> nonDeletedNativeResources = List.of(nativeGroupResource, nativeComputeResource);
        when(groupResourceService.deleteResources(eq(awsContext), any(), eq(nonDeletedNativeResources), any(), eq(false)))
                .thenThrow(new RuntimeException("test"));

        assertThrows(CloudConnectorException.class, () -> underTest.deleteComputeResources(authenticatedContext, cloudStack, cloudResources));

        verify(computeResourceService, times(1)).deleteResources(awsContext, authenticatedContext, cloudResources, false, true);
        ArgumentCaptor<AuthenticatedContext> acCaptor = ArgumentCaptor.forClass(AuthenticatedContext.class);
        verify(computeResourceService, times(1))
                .deleteResources(eq(awsNativeTerminationContext), acCaptor.capture(), eq(nonDeletedNativeResources), eq(false), eq(true));
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), acCaptor.getValue().getCloudContext().getVariant());
        verify(groupResourceService, times(1))
                .deleteResources(eq(awsNativeTerminationContext), acCaptor.capture(), eq(nonDeletedNativeResources), any(), eq(false));
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), acCaptor.getValue().getCloudContext().getVariant());
    }

    private CloudResource getCloudResource(String resourceName, ResourceType resourceType) {
        return CloudResource.builder()
                .withType(resourceType)
                .withName(resourceName)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Map.of())
                .build();
    }
}