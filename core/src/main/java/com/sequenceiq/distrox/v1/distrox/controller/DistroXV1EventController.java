package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1EventEndpoint;

@Controller
public class DistroXV1EventController implements DistroXV1EventEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXV1EventController.class);

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<CDPStructuredEvent> getAuditEvents(@ResourceCrn String resourceCrn, Integer page, Integer size) {
        LOGGER.info("Get Data Hub audit events for {}, page {}, size {}", resourceCrn, page, size);
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        StackView stackView = Optional.ofNullable(stackService.getViewByCrnInWorkspace(resourceCrn, workspaceId)).orElseThrow(notFound("stack", resourceCrn));
        MDCBuilder.buildMdcContext(stackView);
        List<CDPStructuredEvent> ret;
        if (page == null || page.equals(0)) {
            // Temporary workaroud to use a quicker repository call, without paging
            ret = cloudbreakEventsFacade.retrieveLastEventsByStack(stackView.getId(), stackView.getType(), size)
                    .stream()
                    .map(eventResponse -> convert(eventResponse, resourceCrn))
                    .collect(Collectors.toList());
        } else {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
            ret = cloudbreakEventsFacade.retrieveEventsByStack(stackView.getId(), stackView.getType(), pageRequest)
                    .toList()
                    .stream()
                    .map(eventResponse -> convert(eventResponse, resourceCrn))
                    .collect(Collectors.toList());
        }
        return ret;
    }

    private CDPStructuredEvent convert(CloudbreakEventV4Response cloudbreakEventV4Response, String resourceCrn) {
        CDPStructuredNotificationEvent cdpStructuredNotificationEvent = new CDPStructuredNotificationEvent();
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails();
        cdpOperationDetails.setTimestamp(cloudbreakEventV4Response.getEventTimestamp());
        cdpOperationDetails.setEventType(StructuredEventType.NOTIFICATION);
        cdpOperationDetails.setResourceName(cloudbreakEventV4Response.getClusterName());
        cdpOperationDetails.setResourceId(cloudbreakEventV4Response.getClusterId());
        cdpOperationDetails.setResourceCrn(resourceCrn);
        cdpOperationDetails.setResourceEvent(cloudbreakEventV4Response.getEventType());
        cdpOperationDetails.setResourceType(CloudbreakEventService.DATAHUB_RESOURCE_TYPE);

        cdpStructuredNotificationEvent.setOperation(cdpOperationDetails);
        cdpStructuredNotificationEvent.setStatusReason(cloudbreakEventV4Response.getEventMessage());

        return cdpStructuredNotificationEvent;
    }
}
