package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.common.api.type.LoadBalancerType;

/**
 * A selection of methods for collecting metadata about various cloud resources, like private and floating (public) addresses associated with VM instances.
 */
public interface MetadataCollector {

    /**
     * Status with the collected metadata.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param resources            resources managed by Cloudbreak, used to figure out which resources are associated with the given VMs (e.g network port)
     * @param vms                  the VM instances for which the metadata needs to be collected
     * @param allInstances         all VM instances
     * @return status of instances including the metadata
     */
    List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources,
            List<CloudInstance> vms, List<CloudInstance> allInstances);

    List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources);

    InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes);

    InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds);

    List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds);
}
