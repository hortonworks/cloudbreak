package com.sequenceiq.consumption.service.cloud;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * This class is only required by the cloud-reactor module. The cloud-consumption service does not
 * create or interact with cloud resources, it only uses the cloud-reactor module to query metadata
 * from cloud providers (e.g. Amazon CloudWatch's GetMetricStatistics API), hence the functions of
 * this class does not need implementation.
 */
@Component
public class CloudResourceRetrieverService implements ResourceRetriever {

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        throw new UnsupportedOperationException("Cloud resource retrieval is not supported.");
    }
}
