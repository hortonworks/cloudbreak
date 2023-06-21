package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class AwsInstanceFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceFinder.class);

    public String getInstanceId(long privateId, List<CloudResource> computeResources) {
        LOGGER.info("Find instanceid for private id {} from resources {}", privateId, computeResources);
        return computeResources.stream()
                .filter(cr -> cr.getType().equals(AWS_INSTANCE))
                .findFirst()
                .orElseThrow(NotFoundException.notFound("AWS_INSTANCE", privateId))
                .getInstanceId();
    }
}
