package com.sequenceiq.cloudbreak.controller;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Controller
@Transactional(TxType.NEVER)
public class CloudbreakEventController implements EventEndpoint {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Inject
    private StructuredEventService structuredEventService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsSince(Long since) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return cloudbreakEventsFacade.retrieveEventsForWorkspace(workspace, since);
    }

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsByStack(Long stackId) {
        return cloudbreakEventsFacade.retrieveEventsByStack(stackId);
    }

    @Override
    public StructuredEventContainer getStructuredEvents(Long stackId) {
        return getStructuredEventsForStack(stackId);
    }

    @Override
    public Response getStructuredEventsZip(Long stackId) {
        StructuredEventContainer events = getStructuredEventsForStack(stackId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return Response.ok(streamingOutput).header("content-disposition", "attachment; filename = struct-events.zip").build();
    }

    private StructuredEventContainer getStructuredEventsForStack(Long stackId) {
        return structuredEventService.getEventsForUserWithResourceId("stacks", stackId);
    }
}
