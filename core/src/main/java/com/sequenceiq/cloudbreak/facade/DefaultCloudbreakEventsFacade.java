package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.converter.StructuredNotificationEventToCloudbreakEventJsonConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventsFacade.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StructuredNotificationEventToCloudbreakEventJsonConverter eventJsonConverter;

    @Override
    public List<CloudbreakEventsJson> retrieveEventsForWorkspace(Workspace workspace, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(workspace, since);
        return converterUtil.convertAll(cloudbreakEvents, CloudbreakEventsJson.class);
    }

    @Override
    public Page<CloudbreakEventsJson> retrieveEventsByStack(Long stackId, Pageable pageable) {
        Page<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId, pageable);
        LOGGER.debug("Convert notification events for stack [{}]", stackId);
        Page<CloudbreakEventsJson> cloudbreakEventsJsons = eventJsonConverter.convertAllForSameStack(cloudbreakEvents);
        LOGGER.debug("Convert notification events for stack [{}] is done", stackId);
        return cloudbreakEventsJsons;
    }
}
