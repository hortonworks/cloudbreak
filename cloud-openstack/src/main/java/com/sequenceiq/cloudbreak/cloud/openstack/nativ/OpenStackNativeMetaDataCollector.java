package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackNativeMetaDataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNativeMetaDataCollector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackUtils utils;

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private CloudInstanceMetaDataExtractor cloudInstanceMetaDataExtractor;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {

        List<InstanceTemplate> templates = Lists.transform(vms, input -> input.getTemplate());

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, from -> {
            return utils.getPrivateInstanceId(from.getGroupName(), Long.toString(from.getPrivateId()));
        });

        OSClient client = openStackClient.createOSClient(authenticatedContext);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        for (CloudResource resource : resources) {
            if (resource.getType() == ResourceType.OPENSTACK_INSTANCE) {
                String instanceUUID = resource.getReference();
                Server server = client.compute().servers().get(instanceUUID);
                if (server != null) {
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
        }
        return results;
    }
}
