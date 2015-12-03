package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component("ProvisioningFinishedAction")
public class ProvisioningFinishedAction extends AbstractStackCreationAction<LaunchStackResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningFinishedAction.class);

    @Inject
    private StackCreationService stackCreationService;

    public ProvisioningFinishedAction() {
        super(LaunchStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        stackCreationService.provisioningFinished(context, payload, getStartDateIfExist(variables));
        Set<Resource> resources = transformResults(payload.getResults(), stack);
        sendEvent(context.getFlowId(), FlowPhases.METADATA_SETUP.name(),
                new ProvisioningContext.Builder()
                        .setDefaultParams(context.getStack().getId(), Platform.platform(stack.cloudPlatform()))
                        .setProvisionSetupProperties(new HashMap<String, Object>())
                        .setProvisionedResources(resources)
                        .build());
        sendEvent(context.getFlowId(), StackCreationEvent.STACK_CREATION_FINISHED_EVENT.stringRepresentation(), null);
    }

    private Date getStartDateIfExist(Map<Object, Object> variables) {
        Date result = null;
        Object startDateObj = variables.get(StackProvisionConstants.START_DATE);
        if (startDateObj != null && startDateObj instanceof Date) {
            result = (Date) startDateObj;
        }
        return result;
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            if (!cloudResourceStatus.isFailed()) {
                CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                Resource resource = new Resource(cloudResource.getType(), cloudResource.getName(), cloudResource.getReference(), cloudResource.getStatus(),
                        stack, null);
                retSet.add(resource);
            }
        }
        return retSet;
    }

    @Override
    protected Long getStackId(LaunchStackResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }
}
