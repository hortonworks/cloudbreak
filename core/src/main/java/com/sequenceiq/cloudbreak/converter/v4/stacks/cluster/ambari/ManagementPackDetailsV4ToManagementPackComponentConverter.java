package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ManagementPackDetailsV4ToManagementPackComponentConverter
        extends AbstractConversionServiceAwareConverter<ManagementPackDetailsV4Request, ManagementPackComponent> {
    @Inject
    private ManagementPackService managementPackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ManagementPackComponent convert(ManagementPackDetailsV4Request source) {
        ManagementPackComponent mpack = new ManagementPackComponent();
        if (StringUtils.isNotEmpty(source.getName())) {
            User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            ManagementPack dmpack = managementPackService.getByNameForWorkspace(source.getName(), workspace);
            mpack.setName(source.getName());
            mpack.setMpackUrl(dmpack.getMpackUrl());
            mpack.setStackDefault(false);
            mpack.setPreInstalled(false);
            mpack.setForce(dmpack.isForce());
            mpack.setPurge(dmpack.isPurge());
            if (StringUtils.isNotEmpty(dmpack.getPurgeList())) {
                mpack.setPurgeList(Arrays.asList(dmpack.getPurgeList().split(",")));
            }
        } else {
            throw new BadRequestException("Mpack name cannot be empty!");
        }
        return mpack;
    }
}
