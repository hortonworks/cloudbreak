package com.sequenceiq.cloudbreak.cloud.openstack.metadata;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;

public interface CloudInstanceMetaDataExtractor {
    CloudInstanceMetaData extractMetadata(OSClient client, Server server, String instanceId);
}

