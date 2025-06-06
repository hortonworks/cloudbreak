package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(StructuredEventEntity.class)
public class EventV4Controller implements EventV4Endpoint {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Inject
    private LegacyStructuredEventService legacyStructuredEventService;

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public CloudbreakEventV4Responses list(Long since, @AccountId String accountId) {
        return new CloudbreakEventV4Responses(cloudbreakEventsFacade.retrieveEventsForWorkspace(workspaceService.getForCurrentUser().getId(), since));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(String name, Integer page, Integer size, @AccountId String accountId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        StackView stackView = getStackViewByNameIfAvailable(name);
        return cloudbreakEventsFacade.retrieveEventsByStack(stackView.getId(), stackView.getType(), pageable);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public List<CloudbreakEventV4Response> getPagedCloudbreakEventListByStack(String name, Integer page, Integer size, @AccountId String accountId) {
        return getCloudbreakEventsByStack(name, page, size, accountId).getContent();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public List<CloudbreakEventV4Response> getPagedCloudbreakEventListByCrn(@ResourceCrn String crn, Integer page, Integer size, boolean onlyAlive) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        StackView stackView;
        if (onlyAlive) {
            stackView = getStackViewByCrnIfAvailable(crn);
        } else {
            stackView = getStackViewByCrn(crn);
        }
        return cloudbreakEventsFacade.retrieveEventsByStack(stackView.getId(), stackView.getType(), pageable).getContent();
    }

    private StackView getStackViewByNameIfAvailable(String name) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(stackService.getViewByNameInWorkspace(name, workspaceId)).orElseThrow(notFound("stack", name));
    }

    private StackView getStackViewByCrnIfAvailable(String crn) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(stackService.getNotTerminatedViewByCrnInWorkspace(crn, workspaceId)).orElseThrow(notFound("stack", crn));
    }

    private StackView getStackViewByCrn(String crn) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(stackService.getViewByCrnInWorkspace(crn, workspaceId)).orElseThrow(notFound("stack", crn));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StructuredEventContainer structured(String name, @AccountId String accountId) {
        return legacyStructuredEventService.getStructuredEventsForStack(name, workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StructuredEventContainer structuredByCrn(@ResourceCrn String crn, boolean onlyAlive) {
        return legacyStructuredEventService.getStructuredEventsForStackByCrn(crn, workspaceService.getForCurrentUser().getId(), onlyAlive);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Response download(String name, @AccountId String accountId) {
        StructuredEventContainer events = legacyStructuredEventService.getStructuredEventsForStack(name, workspaceService.getForCurrentUser().getId());
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
