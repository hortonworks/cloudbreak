package com.sequenceiq.cloudbreak.controller;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class CloudbreakEventController implements EventEndpoint {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private StructuredEventService structuredEventService;

    public List<CloudbreakEventsJson> get(Long since) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return cloudbreakEventsFacade.retrieveEvents(user.getUserId(), since);
    }

    @Override
    public List<CloudbreakEventsJson> getByStack(Long stackId) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return cloudbreakEventsFacade.retrieveEventsByStack(user.getUserId(), stackId);
    }

    @Override
    public List<StructuredEvent> getStructuredEvents(Long stackId) {
        return getStructuredEventsForStack(stackId);
    }

    @Override
    public Response getStructuredEventsZip(Long stackId) {
        List<StructuredEvent> events = getStructuredEventsForStack(stackId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return Response.ok(streamingOutput).header("content-disposition", "attachment; filename = struct-events.zip").build();
    }

    private List<StructuredEvent> getStructuredEventsForStack(Long stackId) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        Map<String, Long> resourceIds = Maps.newHashMap();
        resourceIds.put("STACK", stackId);
        resourceIds.put("stacks", stackId);
        return structuredEventService.getEventsForUser(identityUser.getUserId(), Arrays.asList("REST", "FLOW", "NOTIFICATION"), resourceIds);
    }
}
