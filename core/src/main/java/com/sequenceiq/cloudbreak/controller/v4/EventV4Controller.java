package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

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

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsResourceAuthorizationService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
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
    private StackViewService stackViewService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @Override
    // TODO
    @DisableCheckPermissions
    public CloudbreakEventV4Responses list(Long since) {
        return new CloudbreakEventV4Responses(cloudbreakEventsFacade.retrieveEventsForWorkspace(threadLocalService.getRequestedWorkspaceId(), since));
    }

    @Override
    @CustomPermissionCheck
    public Page<CloudbreakEventV4Response> getCloudbreakEventsByStack(String name, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        StackView stackView = getStackViewIfAvailable(name);
        checkPermissionBasedOnStackType(stackView);
        return cloudbreakEventsFacade.retrieveEventsByStack(stackView.getId(), stackView.getType(), pageable);
    }

    private void checkPermissionBasedOnStackType(StackView stackView) {
        if (StackType.DATALAKE.equals(stackView.getType())) {
            umsResourceAuthorizationService.checkRightOfUserOnResource(ThreadBasedUserCrnProvider.getUserCrn(),
                    AuthorizationResourceAction.DESCRIBE_DATALAKE, stackView.getResourceCrn());
        } else {
            umsResourceAuthorizationService.checkRightOfUserOnResource(ThreadBasedUserCrnProvider.getUserCrn(),
                    AuthorizationResourceAction.DESCRIBE_DATAHUB, stackView.getResourceCrn());
        }
    }

    private StackView getStackViewIfAvailable(String name) {
        Long workspaceId = threadLocalService.getRequestedWorkspaceId();
        return stackViewService.findByName(name, workspaceId).orElseThrow(notFound("stack", name));
    }

    @Override
    @CustomPermissionCheck
    public StructuredEventContainer structured(String name) {
        checkPermissionBasedOnStackType(getStackViewIfAvailable(name));
        return legacyStructuredEventService.getStructuredEventsForStack(name, threadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CustomPermissionCheck
    public Response download(String name) {
        checkPermissionBasedOnStackType(getStackViewIfAvailable(name));
        StructuredEventContainer events = legacyStructuredEventService.getStructuredEventsForStack(name, threadLocalService.getRequestedWorkspaceId());
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
