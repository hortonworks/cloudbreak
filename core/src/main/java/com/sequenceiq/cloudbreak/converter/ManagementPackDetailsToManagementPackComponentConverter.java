package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ManagementPackDetailsToManagementPackComponentConverter
        extends AbstractConversionServiceAwareConverter<ManagementPackDetails, ManagementPackComponent> {
    @Inject
    private ManagementPackService managementPackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ManagementPackComponent convert(ManagementPackDetails source) {
        ManagementPackComponent mpack = new ManagementPackComponent();
        if (StringUtils.isNoneEmpty(source.getName())) {
            User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            ManagementPack dmpack = managementPackService.getByNameForWorkspace(source.getName(), workspace);
            mpack.setName(source.getName());
            mpack.setMpackUrl(dmpack.getMpackUrl());
            mpack.setStackDefault(false);
            mpack.setPreInstalled(false);
            mpack.setForce(dmpack.isForce());
            mpack.setPurge(dmpack.isPurge());
            if (StringUtils.isNoneEmpty(dmpack.getPurgeList())) {
                mpack.setPurgeList(Arrays.asList(dmpack.getPurgeList().split(",")));
            }
        } else {
            throw new BadRequestException("Mpack name cannot be empty!");
        }
        return mpack;
    }
}
