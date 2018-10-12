package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
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
    public List<CloudbreakEventsJson> retrieveEventsForWorkspace(Workspace workspace, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(workspace, since);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventsJson.class);
    }

    @Override
    public List<CloudbreakEventsJson> retrieveEventsByStack(Long stackId) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventsJson.class);
    }
}
