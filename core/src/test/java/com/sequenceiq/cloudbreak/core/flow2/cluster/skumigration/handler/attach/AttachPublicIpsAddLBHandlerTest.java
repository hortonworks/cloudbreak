package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AttachPublicIpsAddLBHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Mock
    private MetadataSetupService metadataSetupService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private AttachPublicIpsAddLBHandler underTest;

    @Test
    void testDoAccept() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PRIVATE);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(loadBalancer));

        CloudCredential cloudCredential = new CloudCredential();
        List<CloudResource> resources = new ArrayList<>();
        CloudResource resource = mock(CloudResource.class);
        resources.add(resource);

        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        CloudStack cloudStack = mock(CloudStack.class);
        when(resourceConnector.attachPublicIpAddressesForVMsAndAddLB(authenticatedContext, cloudStack, persistenceNotifier)).thenReturn(resources);
        CloudResource armTemplate = mock(CloudResource.class);
        when(resourceRetriever.findByStatusAndTypeAndStack(CREATED, ARM_TEMPLATE, cloudContext.getId())).thenReturn(Optional.of(armTemplate));
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = mock(CloudLoadBalancerMetadata.class);
        List<CloudLoadBalancerMetadata> loadBalancerMetadataList = List.of(cloudLoadBalancerMetadata);
        ArgumentCaptor<List<CloudResource>> summedCloudResources = ArgumentCaptor.forClass(List.class);
        when(loadBalancerMetadataService.collectMetadata(eq(cloudContext), eq(cloudCredential), eq(List.of(LoadBalancerType.PRIVATE)),
                summedCloudResources.capture())).thenReturn(loadBalancerMetadataList);
        CloudInstance cloudInstance1 = new CloudInstance("i-1", null, null, null, null);
        CloudInstance cloudInstance2 = new CloudInstance("i-2", null, null, null, null);
        List<CloudInstance> cloudInstances = List.of(cloudInstance1, cloudInstance2);
        ArrayList<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        instanceMetadataViews.add(mock(InstanceMetadataView.class));
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(instanceMetadataViews);
        when(cloudInstanceConverter.convert(instanceMetadataViews, stack)).thenReturn(cloudInstances);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        CloudVmMetaDataStatus cloudVmMetaDataStatus1 =
                new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance1, InstanceStatus.CREATED),
                        new CloudInstanceMetaData("1.1.1.1", "80.1.1.1"));
        CloudVmMetaDataStatus cloudVmMetaDataStatus2 =
                new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance2, InstanceStatus.CREATED),
                        new CloudInstanceMetaData("1.1.1.2", "80.1.1.2"));
        when(metadataCollector.collect(eq(authenticatedContext), any(), eq(cloudInstances), eq(cloudInstances)))
                .thenReturn(List.of(cloudVmMetaDataStatus1, cloudVmMetaDataStatus2));

        AttachPublicIpsAddLBRequest request = new AttachPublicIpsAddLBRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<AttachPublicIpsAddLBRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(AttachPublicIpsAddLBResult.class, result.getClass());
        List<CloudResource> cloudResourcesForCollect = summedCloudResources.getValue();
        assertThat(cloudResourcesForCollect).containsAll(resources).contains(armTemplate);
        verify(metadataSetupService, times(1)).saveLoadBalancerMetadata(stack, loadBalancerMetadataList);
        verify(resourceConnector).attachPublicIpAddressesForVMsAndAddLB(authenticatedContext, cloudStack, persistenceNotifier);
        verify(instanceMetaDataService, times(1)).updatePublicIp("i-1", "80.1.1.1");
        verify(instanceMetaDataService, times(1)).updatePublicIp("i-2", "80.1.1.2");
    }

    @Test
    void testDoAcceptWithoutAvailableInstances() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PRIVATE);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(loadBalancer));

        CloudCredential cloudCredential = new CloudCredential();
        List<CloudResource> resources = new ArrayList<>();
        CloudResource resource = mock(CloudResource.class);
        resources.add(resource);

        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        CloudStack cloudStack = mock(CloudStack.class);
        when(resourceConnector.attachPublicIpAddressesForVMsAndAddLB(authenticatedContext, cloudStack, persistenceNotifier)).thenReturn(resources);
        CloudResource armTemplate = mock(CloudResource.class);
        when(resourceRetriever.findByStatusAndTypeAndStack(CREATED, ARM_TEMPLATE, cloudContext.getId())).thenReturn(Optional.of(armTemplate));
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = mock(CloudLoadBalancerMetadata.class);
        List<CloudLoadBalancerMetadata> loadBalancerMetadataList = List.of(cloudLoadBalancerMetadata);
        ArgumentCaptor<List<CloudResource>> summedCloudResources = ArgumentCaptor.forClass(List.class);
        when(loadBalancerMetadataService.collectMetadata(eq(cloudContext), eq(cloudCredential), eq(List.of(LoadBalancerType.PRIVATE)),
                summedCloudResources.capture())).thenReturn(loadBalancerMetadataList);
        ArrayList<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(instanceMetadataViews);

        AttachPublicIpsAddLBRequest request = new AttachPublicIpsAddLBRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<AttachPublicIpsAddLBRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(AttachPublicIpsAddLBResult.class, result.getClass());
        List<CloudResource> cloudResourcesForCollect = summedCloudResources.getValue();
        assertThat(cloudResourcesForCollect).containsAll(resources).contains(armTemplate);
        verify(metadataSetupService, times(1)).saveLoadBalancerMetadata(stack, loadBalancerMetadataList);
        verify(resourceConnector).attachPublicIpAddressesForVMsAndAddLB(authenticatedContext, cloudStack, persistenceNotifier);
        verify(instanceMetaDataService, times(0)).updatePublicIp("i-1", "80.1.1.1");
        verify(instanceMetaDataService, times(0)).updatePublicIp("i-2", "80.1.1.2");
    }

    @Test
    void testDoAcceptFailure() {
        when(loadBalancerPersistenceService.findByStackId(any())).thenThrow(new RuntimeException("error"));

        AttachPublicIpsAddLBRequest request = new AttachPublicIpsAddLBRequest(mock(StackView.class), mock(CloudContext.class),
                mock(CloudCredential.class), mock(CloudConnector.class), mock(CloudStack.class));
        HandlerEvent<AttachPublicIpsAddLBRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);
        assertNotNull(result);
        assertEquals(SkuMigrationFailedEvent.class, result.getClass());
        assertEquals(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), result.getSelector());
    }

}