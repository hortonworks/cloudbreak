package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpMetadataCollector.class);

    @Inject
    private GcpNetworkInterfaceProvider gcpNetworkInterfaceProvider;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {

        List<CloudVmMetaDataStatus> instanceMetaData = new ArrayList<>();
        Map<String, CloudResource> instanceNameMap = groupByInstanceName(resources);
        Map<Long, CloudResource> privateIdMap = groupByPrivateId(resources);
        Map<String, Optional<NetworkInterface>> networkInterfacesByInstance = getNetworkInterfaceByInstance(authenticatedContext, instanceNameMap);

        for (CloudInstance cloudInstance : vms) {
            String instanceId = cloudInstance.getInstanceId();
            CloudResource cloudResource;
            cloudResource = instanceId != null ? instanceNameMap.get(instanceId) : privateIdMap.get(cloudInstance.getTemplate().getPrivateId());
            CloudVmMetaDataStatus cloudVmMetaDataStatus = getCloudVmMetaDataStatus(cloudResource, cloudInstance, networkInterfacesByInstance);
            instanceMetaData.add(cloudVmMetaDataStatus);
        }
        return instanceMetaData;
    }

    private Map<String, CloudResource> groupByInstanceName(Iterable<CloudResource> resources) {
        Map<String, CloudResource> instanceNameMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                instanceNameMap.put(resourceName, resource);
            }
        }
        return instanceNameMap;
    }

    private Map<Long, CloudResource> groupByPrivateId(Iterable<CloudResource> resources) {
        Map<Long, CloudResource> privateIdMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                Long privateId = gcpStackUtil.getPrivateId(resourceName);
                if (privateId != null) {
                    privateIdMap.put(privateId, resource);
                }
            }
        }
        return privateIdMap;
    }

    private Map<String, Optional<NetworkInterface>> getNetworkInterfaceByInstance(AuthenticatedContext authenticatedContext,
            Map<String, CloudResource> instanceNameMap) {
        return gcpNetworkInterfaceProvider.provide(authenticatedContext, new ArrayList<>(instanceNameMap.values()));
    }

    private CloudVmMetaDataStatus getCloudVmMetaDataStatus(CloudResource cloudResource, CloudInstance matchedInstance,
            Map<String, Optional<NetworkInterface>> networkInterfacesByInstance) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus;
        if (cloudResource != null) {
            CloudInstance cloudInstance = new CloudInstance(
                    cloudResource.getName(),
                    matchedInstance.getTemplate(),
                    matchedInstance.getAuthentication(),
                    matchedInstance.getSubnetId(),
                    matchedInstance.getAvailabilityZone());
            Optional<NetworkInterface> networkInterface = networkInterfacesByInstance.get(cloudResource.getName());
            try {
                String privateIp = networkInterface
                        .map(NetworkInterface::getNetworkIP)
                        .orElseThrow(() -> new IOException("Private IP must not be null."));
                String publicIp = null;
                List<AccessConfig> acl = networkInterface.get().getAccessConfigs();
                if (acl != null && acl.get(0) != null) {
                    publicIp = networkInterface.get().getAccessConfigs().get(0).getNatIP();
                }
                CloudInstanceMetaData metaData = new CloudInstanceMetaData(privateIp, publicIp);
                CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, metaData);

            } catch (IOException e) {
                LOGGER.warn(String.format("Instance %s is not reachable", cloudResource.getName()), e);
                CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.UNKNOWN);
                cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA);
            }
        } else {
            CloudVmInstanceStatus status = new CloudVmInstanceStatus(matchedInstance, InstanceStatus.TERMINATED);
            cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA);
        }
        return cloudVmMetaDataStatus;
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        // no-op
        return Collections.emptyList();
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return new InstanceStoreMetadata();
    }
}
