package com.sequenceiq.distrox.v1.distrox.controller;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXEventV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Responses;
import com.sequenceiq.distrox.v1.distrox.service.DistroXEventService;

@Controller
public class DistroXEventV1Controller implements DistroXEventV1Endpoint {

    @Inject
    private DistroXEventService distroXEventService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public DistroXEventV1Responses list(Long since) {
        return new DistroXEventV1Responses(distroXEventService.list(since));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public Page<DistroXEventV1Response> getEventsByStack(@ResourceCrn String crn, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return distroXEventService.getEventsByStack(crn, pageable);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public StructuredEventContainer structured(@ResourceCrn String crn) {
        return distroXEventService.structured(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public Response download(@ResourceCrn String crn) {
        return Response.ok(distroXEventService.download(crn))
                .header("content-disposition", "attachment; filename = struct-events.zip").build();
    }
}
