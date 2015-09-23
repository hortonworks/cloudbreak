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
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackHeatUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.metadata.CloudInstanceMetaDataExtractor;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class OpenStackNativeMetaDataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNativeMetaDataCollector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackHeatUtils utils;

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private CloudInstanceMetaDataExtractor cloudInstanceMetaDataExtractor;

    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(vms, new Function<InstanceTemplate, String>() {
            public String apply(InstanceTemplate from) {
                return utils.getPrivateInstanceId(from.getGroupName(), Long.toString(from.getPrivateId()));
            }
        });

        OSClient client = openStackClient.createOSClient(authenticatedContext);
        List<CloudVmInstanceStatus> results = new ArrayList<>();

        for (CloudResource resource : resources) {
            if (resource.getType() == ResourceType.OPENSTACK_INSTANCE) {
                String instanceUUID = resource.getReference();
                Server server = client.compute().servers().get(instanceUUID);
                Map<String, String> metadata = server.getMetadata();
                String privateInstanceId = utils.getPrivateInstanceId(metadata);
                InstanceTemplate template = templateMap.get(privateInstanceId);
                if (template != null) {
                    CloudInstanceMetaData metaData = cloudInstanceMetaDataExtractor.extractMetadata(client, server, instanceUUID);
                    CloudInstance cloudInstance = new CloudInstance(instanceUUID, metaData, template);
                    results.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED));
                }
            }
        }
        return results;
    }
}
