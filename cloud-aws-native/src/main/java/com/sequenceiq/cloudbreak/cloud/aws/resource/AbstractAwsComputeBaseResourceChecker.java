package com.sequenceiq.cloudbreak.cloud.aws.resource;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.exception.AwsResourceException;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractAwsComputeBaseResourceChecker extends AbstractAwsBaseResourceChecker {

    private static final Logger LOGGER = getLogger(AbstractAwsComputeBaseResourceChecker.class);

    @Inject
    private AwsResourceNameService resourceNameService;

    protected List<CloudResourceStatus> checkResources(
            ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}. Build: {}", type, resource, context.isBuild());
            try {
                ResourceStatus resourceStatus = getResourceStatus(context, auth, resource);
                result.add(new CloudResourceStatus(resource, resourceStatus));
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new AwsResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }

    protected ResourceStatus getResourceStatus(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        ResourceStatus resourceStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
        LOGGER.debug("Resource: {} status: {}", resource, resourceStatus);
        return resourceStatus;
    }

    public AwsResourceNameService getResourceNameService() {
        return resourceNameService;
    }
}
