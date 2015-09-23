package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;

@Component("cloudInstanceMetadataExtractor")
public class PortOrComputeApiMetadataExtractor implements CloudInstanceMetaDataExtractor {
    @Inject
    private CloudInstanceMetaDataExtractor portApiExtractor;
    @Inject
    private CloudInstanceMetaDataExtractor computeApiExtractor;

    @Override
    public CloudInstanceMetaData extractMetadata(OSClient client, Server server, String instanceId) {
        if (server.getAddresses().getAddresses().isEmpty()) {
            return portApiExtractor.extractMetadata(client, server, instanceId);
        } else {
            return computeApiExtractor.extractMetadata(client, server, instanceId);
        }
    }
}
