package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.Iterator;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Container;
import com.sequenceiq.cloudbreak.cloud.yarn.loadbalancer.service.launch.YarnLoadBalancerLaunchService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class YarnMetadataCollector implements MetadataCollector {

    @Inject
    private YarnClientUtil yarnClientUtil;

    @Inject
    private ApplicationNameUtil applicationNameUtil;

    @Inject
    private YarnLoadBalancerLaunchService yarnLoadBalancerLaunchService;

    @Inject
    private YarnApplicationDetailsService yarnApplicationDetailsService;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        CloudResource yarnApplication = getYarnApplcationResource(resources);
        return yarnApplicationDetailsService.collect(authenticatedContext, yarnApplication.getName(), vms);
    }

    private CloudResource getYarnApplcationResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.YARN_APPLICATION) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.YARN_APPLICATION));
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        List<CloudLoadBalancerMetadata> loadBalancerMetadata = Lists.newArrayList();

        if (loadBalancerTypes.size() == 0) {
            return loadBalancerMetadata;
        }

        YarnClient yarnClient = yarnClientUtil.createYarnClient(ac);
        String loadBalancerApplicationName = applicationNameUtil.createLoadBalancerName(ac);
        Iterable<Container> loadBalancerContainers = yarnLoadBalancerLaunchService.getContainers(loadBalancerApplicationName, yarnClient);
        Iterator<Container> containerIterator = loadBalancerContainers.iterator();

        for (LoadBalancerType loadBalancerType : loadBalancerTypes) {
            Container container = containerIterator.next();
            CloudLoadBalancerMetadata metadata = CloudLoadBalancerMetadata.builder()
                    .withType(loadBalancerType)
                    .withName(applicationNameUtil.createLoadBalancerComponentName(loadBalancerApplicationName, loadBalancerType))
                    .withIp(container.getIp()).build();
            loadBalancerMetadata.add(metadata);
        }

        return loadBalancerMetadata;
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return new InstanceStoreMetadata();
    }

    @Override
    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        return new InstanceTypeMetadata();
    }

    @Override
    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        return List.of();
    }
}
