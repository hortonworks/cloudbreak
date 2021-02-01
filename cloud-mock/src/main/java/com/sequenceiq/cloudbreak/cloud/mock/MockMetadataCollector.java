package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class MockMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMetadataCollector.class);

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        LOGGER.info("Collect metadata from mock spi");
        try {
            CloudVmMetaDataStatus[] response = mockUrlFactory
                    .get(authenticatedContext, "/spi/cloud_metadata_statuses")
                    .post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), CloudVmMetaDataStatus[].class);
            return Arrays.asList(response);
        } catch (Exception e) {
            throw new CloudbreakServiceException("can't convert to object", e);
        }
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        // no-op
        return Collections.emptyList();
    }
}
