package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class GcpMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpMetadataCollector.class);

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {

        List<CloudVmMetaDataStatus> instanceMetaData = new ArrayList<>();

        Map<String, CloudResource> instanceNameMap = groupByInstanceName(resources);
        Map<Long, CloudResource> privateIdMap = groupByPrivateId(resources);

        for (CloudInstance cloudInstance : vms) {
            String instanceId = cloudInstance.getInstanceId();
            CloudResource cloudResource;
            if (instanceId != null) {
                cloudResource = instanceNameMap.get(instanceId);
            } else {
                cloudResource = privateIdMap.get(cloudInstance.getTemplate().getPrivateId());
            }
            CloudVmMetaDataStatus cloudVmMetaDataStatus = getCloudVmMetaDataStatus(authenticatedContext, cloudResource, cloudInstance);
            instanceMetaData.add(cloudVmMetaDataStatus);
        }

        return instanceMetaData;
    }

    private CloudVmMetaDataStatus getCloudVmMetaDataStatus(AuthenticatedContext authenticatedContext, CloudResource cloudResource,
            CloudInstance matchedInstance) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus;
        if (cloudResource != null) {
            CloudInstance cloudInstance = new CloudInstance(cloudResource.getName(), matchedInstance.getTemplate());
            try {
                CloudCredential credential = authenticatedContext.getCloudCredential();
                CloudContext cloudContext = authenticatedContext.getCloudContext();
                Compute compute = GcpStackUtil.buildCompute(credential);
                Instance executeInstance = getInstance(cloudContext, credential, compute, cloudResource.getName());

                String privateIp = executeInstance.getNetworkInterfaces().get(0).getNetworkIP();
                String publicIp = null;
                List<AccessConfig> acl = executeInstance.getNetworkInterfaces().get(0).getAccessConfigs();
                if (acl != null && acl.get(0) != null) {
                    publicIp = executeInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP();
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

    private Map<String, CloudResource> groupByInstanceName(List<CloudResource> resources) {
        Map<String, CloudResource> instanceNameMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                instanceNameMap.put(resourceName, resource);
            }
        }
        return instanceNameMap;
    }

    private Map<Long, CloudResource> groupByPrivateId(List<CloudResource> resources) {
        Map<Long, CloudResource> privateIdMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                Long privateId = GcpStackUtil.getPrivateId(resourceName);
                if (privateId != null) {
                    privateIdMap.put(privateId, resource);
                }

            }
        }
        return privateIdMap;
    }

    private Instance getInstance(CloudContext context, CloudCredential credential, Compute compute, String instanceName) throws IOException {
        return compute.instances().get(GcpStackUtil.getProjectId(credential),
                context.getLocation().getAvailabilityZone().value(), instanceName).execute();
    }

}
