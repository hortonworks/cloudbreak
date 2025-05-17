package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class MockMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMetadataCollector.class);

    private static final String LARGE = "large";

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

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return new InstanceStoreMetadata();
    }

    @Override
    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        Map<String, String> instanceTypes = instanceIds.stream().collect(Collectors.toMap(i -> i, i -> LARGE));
        return new InstanceTypeMetadata(instanceTypes);
    }

    @Override
    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        return List.of();
    }
}
