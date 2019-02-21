package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackService stackService;

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Long workspaceId, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(workspaceId, since);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspaceByStack(Long workspaceId, String stackName) {
        Stack stack = stackService.getByNameInWorkspace(stackName, workspaceId);
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stack.getId());
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }
}
