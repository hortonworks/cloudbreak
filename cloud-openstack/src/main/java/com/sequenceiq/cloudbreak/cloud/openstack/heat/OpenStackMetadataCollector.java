package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.heat.Stack;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.metadata.CloudInstanceMetaDataExtractor;

@Component
public class OpenStackMetadataCollector implements MetadataCollector {
    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackUtils utils;

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private CloudInstanceMetaDataExtractor cloudInstanceMetaDataExtractor;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        CloudResource resource = utils.getHeatResource(resources);

        String stackName = utils.getStackName(authenticatedContext);
        String heatStackId = resource.getName();

        List<InstanceTemplate> templates = Lists.transform(vms, CloudInstance::getTemplate);

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, from -> {
            return utils.getPrivateInstanceId(from.getGroupName(), Long.toString(from.getPrivateId()));
        });

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);

        List<CloudVmMetaDataStatus> results = new ArrayList<>();


        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            if (!StringUtils.isEmpty(instanceUUID)) {
                Server server = client.compute().servers().get(instanceUUID);
                Map<String, String> metadata = server.getMetadata();
                String privateInstanceId = utils.getPrivateInstanceId(metadata);
                InstanceTemplate template = templateMap.get(privateInstanceId);
                if (template != null) {
                    CloudInstanceMetaData md = cloudInstanceMetaDataExtractor.extractMetadata(client, server, instanceUUID);
                    CloudInstance cloudInstance = new CloudInstance(instanceUUID, template);
                    CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                    results.add(new CloudVmMetaDataStatus(status, md));
                }
            }
        }

        return results;
    }
}
