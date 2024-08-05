package com.sequenceiq.freeipa.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerMetadataCollectionServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaLoadBalancerMetadataCollectionService underTest;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

    @Mock
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Test
    void testCollectLoadBalancerMetadata() {
        LoadBalancerMetadataCollectionRequest request = new LoadBalancerMetadataCollectionRequest(STACK_ID, cloudContext, cloudCredential, null);
        List<CloudResource> cloudResources = new ArrayList<>();
        CloudLoadBalancerMetadata metadata = CloudLoadBalancerMetadata.builder().build();
        LoadBalancer loadBalancer = new LoadBalancer();
        LoadBalancer extendedLoadBalancer = new LoadBalancer();

        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        when(loadBalancerMetadataService.collectMetadata(cloudContext, cloudCredential, List.of(LoadBalancerType.PRIVATE), cloudResources))
                .thenReturn(List.of(metadata));
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);
        when(freeIpaLoadBalancerConfigurationService.extendConfigurationWithMetadata(loadBalancer, metadata)).thenReturn(extendedLoadBalancer);

        underTest.collectLoadBalancerMetadata(request);

        verify(freeIpaLoadBalancerService).save(extendedLoadBalancer);
    }

    @Test
    void testCollectLoadBalancerMetadataWhenMultipleCloudLoadBalancerMetadataIsAvailable() {
        LoadBalancerMetadataCollectionRequest request = new LoadBalancerMetadataCollectionRequest(STACK_ID, cloudContext, cloudCredential, null);
        List<CloudResource> cloudResources = new ArrayList<>();

        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        when(loadBalancerMetadataService.collectMetadata(cloudContext, cloudCredential, List.of(LoadBalancerType.PRIVATE), cloudResources))
                .thenReturn(List.of(CloudLoadBalancerMetadata.builder().build(), CloudLoadBalancerMetadata.builder().build()));

        assertThrows(CloudbreakServiceException.class, () -> underTest.collectLoadBalancerMetadata(request));
    }

}