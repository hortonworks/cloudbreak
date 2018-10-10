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

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.EventV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(StructuredEventEntity.class)
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
    private WorkspaceService workspaceService;

    @Inject
    private StackService stackService;

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsSince(Long workspaceId, Long since) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        return cloudbreakEventsFacade.retrieveEventsForWorkspace(workspace, since);
    }

    @Override
    public List<CloudbreakEventsJson> getCloudbreakEventsByStack(Long workspaceId, String name) {
        return cloudbreakEventsFacade.retrieveEventsByStack(getStackIfAvailable(workspaceId, name).getId());
    }

    @Override
    public StructuredEventContainer getStructuredEvents(Long workspaceId, String name) {
        return getStructuredEventsForStack(name, workspaceId);
    }

    @Override
    public Response getStructuredEventsZip(Long workspaceId, String name) {
        StructuredEventContainer events = getStructuredEventsForStack(name, workspaceId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return Response.ok(streamingOutput).header("content-disposition", "attachment; filename = struct-events.zip").build();
    }

    private StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId) {
        return structuredEventService.getEventsForUserWithResourceId("stacks", getStackIfAvailable(workspaceId, name).getId());
    }

    private Stack getStackIfAvailable(Long workspaceId, String name) {
        return Optional.ofNullable(stackService.getByNameInWorkspace(name, workspaceId)).orElseThrow(notFound("stack", name));
    }
}
