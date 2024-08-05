package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Service
public class FreeIpaLoadBalancerMetadataCollectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerMetadataCollectionService.class);

    private static final int EXPECTED_NUMBER_OF_LOAD_BALANCER_METADATA = 1;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Inject
    private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Inject
    private ResourceService resourceService;

    public void collectLoadBalancerMetadata(LoadBalancerMetadataCollectionRequest request) {
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(request.getResourceId());
        CloudLoadBalancerMetadata metadata = getCloudLoadBalancerMetadata(request, cloudResources);
        LoadBalancer loadBalancer = freeIpaLoadBalancerService.getByStackId(request.getResourceId());
        LoadBalancer loadBalancerWithMetadata = freeIpaLoadBalancerConfigurationService.extendConfigurationWithMetadata(loadBalancer, metadata);
        LOGGER.debug("Saving extended load balancer {}", loadBalancerWithMetadata);
        freeIpaLoadBalancerService.save(loadBalancerWithMetadata);
    }

    private CloudLoadBalancerMetadata getCloudLoadBalancerMetadata(LoadBalancerMetadataCollectionRequest request, List<CloudResource> cloudResources) {
        List<CloudLoadBalancerMetadata> metadataList = loadBalancerMetadataService.collectMetadata(request.getCloudContext(),
                request.getCloudCredential(), List.of(LoadBalancerType.PRIVATE), cloudResources);
        if (metadataList.size() == EXPECTED_NUMBER_OF_LOAD_BALANCER_METADATA) {
            return metadataList.getFirst();
        } else {
            throw new CloudbreakServiceException(String.format("The size of the retrieved load balancer metadata is %d but the expected size is %d",
                    metadataList.size(), EXPECTED_NUMBER_OF_LOAD_BALANCER_METADATA));
        }
    }
}
