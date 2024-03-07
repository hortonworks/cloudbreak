package com.sequenceiq.freeipa.service.stack.instance;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Service
public class InstanceValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceValidationService.class);

    @Inject
    private MetadataSetupService metadataSetupService;

    public void finishAddInstances(StackContext context, UpscaleStackResult payload) throws CloudbreakException {
        LOGGER.debug("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context, payload.getErrorDetails(), results);
        Set<Resource> resourceSet = transformResults(results, context.getStack());
        if (resourceSet.isEmpty()) {
            metadataSetupService.cleanupRequestedInstances(context.getStack());
            throw new CloudbreakException("Failed to upscale the cluster since all create request failed. Resource set is empty");
        } else {
            LOGGER.debug("Adding new instances to the stack is DONE");
        }
    }

    private void validateResourceResults(StackContext context, Exception exception, List<CloudResourceStatus> results) throws CloudbreakException {
        if (exception != null) {
            LOGGER.warn(format("Failed to upscale stack: %s", context.getCloudContext()), exception);
            throw new CloudbreakException(exception);
        } else {
            List<CloudResourceStatus> missingResources = results.stream()
                    .filter(result -> CommonResourceType.TEMPLATE == result.getCloudResource().getType().getCommonResourceType())
                    .filter(status -> status.isFailed() || status.isDeleted())
                    .toList();

            if (!missingResources.isEmpty()) {
                StringBuilder message = new StringBuilder("Failed to upscale the stack for ")
                        .append(context.getCloudContext())
                        .append(" due to: ");
                missingResources.forEach(missingResource ->
                        message.append("[privateId: ")
                                .append(missingResource.getPrivateId())
                                .append(", statusReason: ")
                                .append(missingResource.getStatusReason())
                                .append("] "));
                LOGGER.warn(message.toString());
                throw new CloudbreakException(message.toString());
            }
        }
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        return cloudResourceStatuses.stream()
                .filter(Predicate.not(CloudResourceStatus::isFailed))
                .map(cr -> mapToResource(stack, cr.getCloudResource()))
                .collect(Collectors.toSet());
    }

    private Resource mapToResource(Stack stack, CloudResource cloudResource) {
        return new Resource(
                cloudResource.getType(),
                cloudResource.getName(),
                cloudResource.getReference(),
                cloudResource.getStatus(),
                stack,
                null,
                cloudResource.getAvailabilityZone());
    }
}
