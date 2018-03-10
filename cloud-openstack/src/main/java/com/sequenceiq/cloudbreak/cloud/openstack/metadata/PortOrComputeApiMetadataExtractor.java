package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;

@Component("cloudInstanceMetadataExtractor")
public class PortOrComputeApiMetadataExtractor implements CloudInstanceMetaDataExtractor {

    @Inject
    private PortApiExtractor portApiExtractor;

    @Inject
    private ComputeApiExtractor computeApiExtractor;

    @Override
    public CloudInstanceMetaData extractMetadata(OSClient<?> client, Server server, String instanceId) {
        return server.getAddresses().getAddresses().isEmpty() ? portApiExtractor.extractMetadata(client, server, instanceId)
                : computeApiExtractor.extractMetadata(client, server, instanceId);
    }
}
