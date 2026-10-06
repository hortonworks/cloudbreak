package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.converter.CdpStructuredEventToCloudbreakEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.StructuredNotificationEventToCloudbreakEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventsFacade.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private StructuredNotificationEventToCloudbreakEventV4ResponseConverter eventConverter;

    @Inject
    private CdpStructuredEventToCloudbreakEventV4ResponseConverter cdpEventConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Override
    public List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Long workspaceId, Long since) {
        return cloudbreakEventService.cloudbreakEvents(workspaceId, since).stream()
                .map(e -> eventConverter.convert(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<CloudbreakEventV4Response> retrieveLastEventsByStack(Long stackId, StackType stackType, int size) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakLastEventsForStack(stackId, stackType.getResourceType(), size);
        LOGGER.debug("Convert notification events for stack [{}]", stackId);
        List<CloudbreakEventV4Response> cloudbreakEventsJsons = cloudbreakEvents.stream().map(eventConverter::convert).toList();
        LOGGER.debug("Convert notification events for stack [{}] is done", stackId);
        return cloudbreakEventsJsons;
    }

    @Override
    public Page<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId, StackType stackType, Pageable pageable) {
        LOGGER.debug("Convert notification events for stack [{}]", stackId);
        List<CloudbreakEventV4Response> coreEvents = cloudbreakEventService
                .cloudbreakEventsForStack(stackId, stackType.getResourceType(), pageable)
                .map(eventConverter::convert)
                .getContent();

        List<CloudbreakEventV4Response> combined = new ArrayList<>(coreEvents);
        combined.addAll(retrieveRedbeamsEventsIfExternalDatabase(stackId));
        combined.sort(Comparator.comparingLong(CloudbreakEventV4Response::getEventTimestamp).reversed());

        LOGGER.debug("Convert notification events for stack [{}] is done, total [{}] events", stackId, combined.size());
        return pageSlice(combined, pageable);
    }

    private List<CloudbreakEventV4Response> retrieveRedbeamsEventsIfExternalDatabase(Long stackId) {
        try {
            ClusterView cluster = stackDtoService.getClusterViewByStackId(stackId);
            if (cluster != null && cluster.hasExternalDatabase()) {
                return redbeamsClientService.getStructuredEventsByCrn(cluster.getDatabaseServerCrn()).stream()
                        .map(cdpEventConverter::convert)
                        .toList();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to retrieve redbeams structured events for stack [{}], falling back to core events only: {}",
                    stackId, e.getMessage(), e);
        }
        return List.of();
    }

    private Page<CloudbreakEventV4Response> pageSlice(List<CloudbreakEventV4Response> combined, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(combined, pageable, combined.size());
        }
        int from = (int) Math.min(pageable.getOffset(), combined.size());
        int to = (int) Math.min(pageable.getOffset() + pageable.getPageSize(), combined.size());
        return new PageImpl<>(combined.subList(from, to), pageable, combined.size());
    }

}
