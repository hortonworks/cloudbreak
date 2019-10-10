package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(StructuredEventEntity.class)
public class EventV4Controller implements EventV4Endpoint {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Inject
    private StructuredEventService structuredEventService;

    @Inject
    private StackService stackService;

    @Override
    public CloudbreakEventV4Responses list(Long workspaceId, Long since) {
        return new CloudbreakEventV4Responses(cloudbreakEventsFacade.retrieveEventsForWorkspace(workspaceId, since));
    }

    @Override
    public Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(Long workspaceId, String name, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return cloudbreakEventsFacade.retrieveEventsByStack(getStackIdIfAvailable(workspaceId, name), pageable);
    }

    private Long getStackIdIfAvailable(Long workspaceId, String name) {
        return Optional.ofNullable(stackService.getIdByNameInWorkspace(name, workspaceId)).orElseThrow(notFound("stack", name));
    }

    @Override
    public StructuredEventContainer structured(Long workspaceId, String name) {
        return structuredEventService.getStructuredEventsForStack(name, workspaceId);
    }

    @Override
    public Response download(Long workspaceId, String name) {
        StructuredEventContainer events = structuredEventService.getStructuredEventsForStack(name, workspaceId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return Response.ok(streamingOutput).header("content-disposition", "attachment; filename = struct-events.zip").build();
    }
}
