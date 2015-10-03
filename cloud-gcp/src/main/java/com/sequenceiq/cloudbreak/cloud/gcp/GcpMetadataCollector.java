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
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.type.CloudRegion;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class GcpMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpMetadataCollector.class);

    @Override
    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        List<CloudVmInstanceStatus> instanceMetaData = new ArrayList<>();
        CloudCredential credential = authenticatedContext.getCloudCredential();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        Compute compute = GcpStackUtil.buildCompute(credential);
        Map<String, InstanceTemplate> templateMap = groupByInstanceName(resources, vms);
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                try {
                    String resourceName = resource.getName();
                    InstanceTemplate template = templateMap.get(resourceName);
                    if (template != null) {
                        Instance executeInstance = getInstance(cloudContext, credential, compute, resourceName);
                        CloudInstanceMetaData metaData = new CloudInstanceMetaData(
                                executeInstance.getNetworkInterfaces().get(0).getNetworkIP(),
                                executeInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP());
                        CloudInstance cloudInstance = new CloudInstance(resourceName, metaData, template);
                        instanceMetaData.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED));
                    }
                } catch (IOException e) {
                    LOGGER.warn(String.format("Instance %s is not reachable", resource), e);
                }
            }
        }
        return instanceMetaData;
    }

    private Map<String, InstanceTemplate> groupByInstanceName(List<CloudResource> resources, List<InstanceTemplate> vms) {
        Map<String, InstanceTemplate> templateMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (ResourceType.GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                long privateId = GcpStackUtil.getPrivateId(resourceName);
                for (InstanceTemplate vm : vms) {
                    if (vm.getPrivateId() == privateId) {
                        templateMap.put(resourceName, vm);
                        break;
                    }
                }
            }
        }
        return templateMap;
    }

    private Instance getInstance(CloudContext context, CloudCredential credential, Compute compute, String instanceName) throws IOException {
        return compute.instances().get(GcpStackUtil.getProjectId(credential),
                CloudRegion.valueOf(context.getRegion()).value(), instanceName).execute();
    }

}
