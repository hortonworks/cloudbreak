package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request.ManagementPackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(ManagementPack.class)
public class ManagementPackV4Controller extends NotificationController implements ManagementPackV4Endpoint {

    @Inject
    private ManagementPackService mpackService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ManagementPackV4Responses listByWorkspace(Long workspaceId) {
        return new ManagementPackV4Responses(converterUtil.convertAllAsSet(mpackService.findAllByWorkspaceId(workspaceId), ManagementPackV4Response.class));
    }

    @Override
    public ManagementPackV4Response getByNameInWorkspace(Long workspaceId, String name) {
        ManagementPack managementPack = mpackService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(managementPack, ManagementPackV4Response.class);
    }

    @Override
    public ManagementPackV4Response createInWorkspace(Long workspaceId, @Valid ManagementPackV4Request request) {
        ManagementPack managementPack = converterUtil.convert(request, ManagementPack.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        managementPack = mpackService.create(managementPack, workspaceId, user);
        ManagementPackV4Response response = converterUtil.convert(managementPack, ManagementPackV4Response.class);
        notify(response, NotificationEventType.CREATE_SUCCESS, WorkspaceResource.MPACK, workspaceId);
        return response;
    }

    @Override
    public ManagementPackV4Response deleteInWorkspace(Long workspaceId, String name) {
        ManagementPack deleted = mpackService.deleteByNameFromWorkspace(name, workspaceId);
        ManagementPackV4Response response = converterUtil.convert(deleted, ManagementPackV4Response.class);
        notify(response, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.MPACK, workspaceId);
        return response;
    }

    @Override
    public ManagementPackV4Responses deleteMultipleInWorkspace(Long workspaceId, Set<String> names) {
        Set<ManagementPack> response = mpackService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(response, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.MPACK, workspaceId);
        return new ManagementPackV4Responses(converterUtil.convertAllAsSet(response, ManagementPackV4Response.class));
    }
}
