package com.sequenceiq.distrox.v1.distrox.facade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.StructuredNotificationEventToDistroXV1EventResponseConverter;

@Service
public class DefaultDistroxEventsFacade implements DistroxEventsFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDistroxEventsFacade.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackService stackService;

    @Inject
    private StructuredNotificationEventToDistroXV1EventResponseConverter eventConverter;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Override
    public List<DistroXEventV1Response> retrieveEvents(Long since) {
        List<Long> datahubIds = stackService.getAsAuthorizationResources(threadLocalService.getRequestedWorkspaceId(), WORKLOAD)
            .stream().map(ResourceWithId::getId).collect(Collectors.toList());
        List<StructuredNotificationEvent> filtered = cloudbreakEventService.cloudbreakEvents(threadLocalService.getRequestedWorkspaceId(), since)
                .stream().filter(event -> datahubIds.contains(event.getNotificationDetails().getStackId())).collect(Collectors.toList());
        return converterUtil.convertAll(filtered, DistroXEventV1Response.class);
    }

    @Override
    public Page<DistroXEventV1Response> retrieveEventsByStack(String stackCrn, Pageable pageable) {
        StackView stackView = stackService.getViewByCrnInWorkspace(stackCrn, threadLocalService.getRequestedWorkspaceId());
        if (!WORKLOAD.equals(stackView.getType())) {
            throw new BadRequestException(String.format("Stack with crn %s is not a datahub cluster!", stackCrn));
        }
        Page<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackView.getId(),
                WORKLOAD.getResourceType(), pageable);
        LOGGER.debug("Convert notification events for stack [{}]", stackCrn);
        Page<DistroXEventV1Response> cloudbreakEventsJsons = cloudbreakEvents.map(eventConverter::convert);
        LOGGER.debug("Convert notification events for stack [{}] is done", stackCrn);
        return cloudbreakEventsJsons;
    }
}
