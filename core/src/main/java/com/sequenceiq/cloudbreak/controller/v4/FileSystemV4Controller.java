package com.sequenceiq.cloudbreak.controller.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemParametersV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParametersV4Response;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(FileSystem.class)
public class FileSystemV4Controller implements FileSystemV4Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public FileSystemParametersV4Response getFileSystemParameters(Long workspaceId, FileSystemParametersV4Filter fileSystemParametersV4Filter) {
        Workspace workspace = getWorkspace(workspaceId);
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                fileSystemParametersV4Filter.getBlueprintName(),
                fileSystemParametersV4Filter.getClusterName(),
                fileSystemParametersV4Filter.getStorageName(),
                fileSystemParametersV4Filter.getFileSystemType(),
                fileSystemParametersV4Filter.getAccountName(),
                fileSystemParametersV4Filter.isAttachedCluster(),
                workspace);
        List<FileSystemParameterV4Response> result = new ArrayList<>();
        for (ConfigQueryEntry configQueryEntry : entries) {
            result.add(conversionService.convert(configQueryEntry, FileSystemParameterV4Response.class));
        }
        FileSystemParametersV4Response parametersQueryResponse = new FileSystemParametersV4Response();
        return parametersQueryResponse;
    }

    private Workspace getWorkspace(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return workspaceService.get(workspaceId, user);
    }
}
