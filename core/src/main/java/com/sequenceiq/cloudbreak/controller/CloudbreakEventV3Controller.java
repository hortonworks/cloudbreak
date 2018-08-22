package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.EventV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
@Transactional(TxType.NEVER)
public class CloudbreakEventV3Controller implements EventV3Endpoint {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Inject
    private StructuredEventService structuredEventService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private StackService stackService;

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsSince(Long organizationId, Long since) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(organizationId, user);
        return cloudbreakEventsFacade.retrieveEventsForOrganiztion(organization, since);
    }

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsByStack(Long organizationId, String name) {
        return cloudbreakEventsFacade.retrieveEventsByStack(getStackIfAvailable(organizationId, name).getId());
    }

    @Override
    public StructuredEventContainer getStructuredEvents(Long organizationId, String name) {
        return getStructuredEventsForStack(name, organizationId);
    }

    @Override
    public Response getStructuredEventsZip(Long organizationId, String name) {
        StructuredEventContainer events = getStructuredEventsForStack(name, organizationId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return Response.ok(streamingOutput).header("content-disposition", "attachment; filename = struct-events.zip").build();
    }

    private StructuredEventContainer getStructuredEventsForStack(String name, Long organizationId) {
        return structuredEventService.getEventsForUserWithResourceId("stacks", getStackIfAvailable(organizationId, name).getId());
    }

    private Stack getStackIfAvailable(Long organizationId, String name) {
        return Optional.ofNullable(stackService.getByNameInOrg(name, organizationId)).orElseThrow(notFound("stack", name));
    }
}
