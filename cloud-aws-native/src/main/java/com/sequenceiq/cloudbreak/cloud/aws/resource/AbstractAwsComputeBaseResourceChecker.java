package com.sequenceiq.cloudbreak.cloud.aws.resource;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                boolean finished = isFinished(context, auth, resource);
                ResourceStatus successStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
                result.add(new CloudResourceStatus(resource, finished ? successStatus : ResourceStatus.IN_PROGRESS));
                if (finished) {
                    if (successStatus == ResourceStatus.CREATED) {
                        LOGGER.debug("Creation of {} was successful", resource);
                    } else {
                        LOGGER.debug("Deletion of {} was successful", resource);
                    }
                }
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new AwsResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }

    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        LOGGER.debug("Default check is not implemented, so return with the default value: true");
        return true;
    }

    public AwsResourceNameService getResourceNameService() {
        return resourceNameService;
    }
}
