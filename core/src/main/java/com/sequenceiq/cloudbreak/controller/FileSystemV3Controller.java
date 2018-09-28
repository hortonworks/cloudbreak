package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.FileSystemV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueriesResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParametersQueryRequest;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
public class FileSystemV3Controller implements FileSystemV3Endpoint, WorkspaceAwareResourceController<FileSystem> {

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
    public StructuredParameterQueriesResponse getFileSystemParameters(Long workspaceId, StructuredParametersQueryRequest structuredParametersQueryRequest) {
        Workspace workspace = getWorkspace(workspaceId);
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                structuredParametersQueryRequest.getBlueprintName(),
                structuredParametersQueryRequest.getClusterName(),
                structuredParametersQueryRequest.getStorageName(),
                structuredParametersQueryRequest.getFileSystemType(),
                structuredParametersQueryRequest.getAccountName(),
                structuredParametersQueryRequest.isAttachedCluster(),
                workspace);
        List<StructuredParameterQueryResponse> result = new ArrayList<>();
        for (ConfigQueryEntry configQueryEntry : entries) {
            result.add(conversionService.convert(configQueryEntry, StructuredParameterQueryResponse.class));
        }
        StructuredParameterQueriesResponse parametersQueryResponse = new StructuredParameterQueriesResponse();
        parametersQueryResponse.setEntries(result);
        return parametersQueryResponse;
    }

    private Workspace getWorkspace(Long workspaceId) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        return workspaceService.get(workspaceId, user);
    }

    @Override
    public Class<? extends WorkspaceResourceRepository<FileSystem, ?>> getWorkspaceAwareResourceRepository() {
        return FileSystemRepository.class;
    }
}
