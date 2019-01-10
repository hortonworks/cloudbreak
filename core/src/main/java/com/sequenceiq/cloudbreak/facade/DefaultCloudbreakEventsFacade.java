package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Workspace workspace, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(workspace, since);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventV4Response.class);
    }
}
