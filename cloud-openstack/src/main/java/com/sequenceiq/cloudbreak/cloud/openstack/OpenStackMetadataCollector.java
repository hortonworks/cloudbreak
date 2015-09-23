package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.heat.Stack;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.metadata.CloudInstanceMetaDataExtractor;

@Component
public class OpenStackMetadataCollector implements MetadataCollector {
    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackHeatUtils utils;

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private CloudInstanceMetaDataExtractor cloudInstanceMetaDataExtractor;

    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        CloudResource resource = utils.getHeatResource(resources);

        String stackName = authenticatedContext.getCloudContext().getStackName();
        String heatStackId = resource.getName();

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(vms, new Function<InstanceTemplate, String>() {
            public String apply(InstanceTemplate from) {
                return utils.getPrivateInstanceId(from.getGroupName(), Long.toString(from.getPrivateId()));
            }
        });

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);

        List<CloudVmInstanceStatus> results = new ArrayList<>();


        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            Server server = client.compute().servers().get(instanceUUID);
            Map<String, String> metadata = server.getMetadata();
            String privateInstanceId = utils.getPrivateInstanceId(metadata);
            InstanceTemplate template = templateMap.get(privateInstanceId);
            if (template != null) {
                CloudInstance cloudInstance = createInstanceMetaData(client, server, instanceUUID, template);
                results.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED));
            }
        }

        return results;
    }

    private CloudInstance createInstanceMetaData(OSClient client, Server server, String instanceId, InstanceTemplate template) {
        CloudInstanceMetaData md = cloudInstanceMetaDataExtractor.extractMetadata(client, server, instanceId);
        return new CloudInstance(instanceId, md, template);
    }
}
