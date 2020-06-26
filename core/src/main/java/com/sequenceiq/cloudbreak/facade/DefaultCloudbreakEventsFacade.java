package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.converter.StructuredNotificationEventToCloudbreakEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventsFacade.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackService stackService;

    @Inject
    private StructuredNotificationEventToCloudbreakEventV4ResponseConverter eventConverter;

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Long workspaceId, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(workspaceId, since);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }

    @Override
    public Page<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId, Pageable pageable) {
        Page<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId, pageable);
        LOGGER.debug("Convert notification events for stack [{}]", stackId);
        Page<CloudbreakEventV4Response> cloudbreakEventsJsons = cloudbreakEvents.map(eventConverter::convert);
        LOGGER.debug("Convert notification events for stack [{}] is done", stackId);
        return cloudbreakEventsJsons;
    }

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspaceByStack(Long workspaceId, String stackName) {
        Stack stack = stackService.getByNameInWorkspace(stackName, workspaceId);
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stack.getId());
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }
}
