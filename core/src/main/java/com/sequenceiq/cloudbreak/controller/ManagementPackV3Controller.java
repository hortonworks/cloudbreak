package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(ManagementPack.class)
public class ManagementPackV3Controller extends NotificationController implements ManagementPackV3Endpoint {

    @Inject
    private ManagementPackService mpackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<ManagementPackResponse> listByWorkspace(Long workspaceId) {
        return mpackService.findAllByWorkspaceId(workspaceId).stream()
                .map(mpack -> conversionService.convert(mpack, ManagementPackResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ManagementPackResponse getByNameInWorkspace(Long workspaceId, String name) {
        ManagementPack managementPack = mpackService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(managementPack, ManagementPackResponse.class);
    }

    @Override
    public ManagementPackResponse createInWorkspace(Long workspaceId, @Valid ManagementPackRequest request) {
        ManagementPack managementPack = conversionService.convert(request, ManagementPack.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        managementPack = mpackService.create(managementPack, workspaceId, user);
        notify(ResourceEvent.MANAGEMENT_PACK_CREATED);
        return conversionService.convert(managementPack, ManagementPackResponse.class);
    }

    @Override
    public ManagementPackResponse deleteInWorkspace(Long workspaceId, String name) {
        ManagementPack deleted = mpackService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.MANAGEMENT_PACK_DELETED);
        return conversionService.convert(deleted, ManagementPackResponse.class);
    }
}
