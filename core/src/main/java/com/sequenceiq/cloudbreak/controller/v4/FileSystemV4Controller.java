package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.converter.v4.filesystems.ConfigQueryEntryToFileSystemParameterV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;

@Controller
@WorkspaceEntityType(FileSystem.class)
public class FileSystemV4Controller implements FileSystemV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private ConfigQueryEntryToFileSystemParameterV4ResponseConverter configQueryEntryToFileSystemParameterV4ResponseConverter;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public FileSystemParameterV4Responses getFileSystemParameters(
            Long workspaceId,
            @ResourceName String blueprintName,
            String clusterName,
            String accountName,
            String storageName,
            String fileSystemType,
            Boolean attachedCluster,
            Boolean secure) {
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(blueprintName, clusterName, storageName,
                fileSystemType, accountName, attachedCluster, secure, threadLocalService.getRequestedWorkspaceId());
        return new FileSystemParameterV4Responses(entries.stream()
                .map(e -> configQueryEntryToFileSystemParameterV4ResponseConverter.convert(e))
                .collect(Collectors.toList())
        );
    }

    @Override
    @InternalOnly
    public FileSystemParameterV4Responses getFileSystemParametersInternal(Long workspaceId,
            String blueprintName,
            String clusterName,
            String accountName,
            String storageName,
            String fileSystemType,
            Boolean attachedCluster,
            Boolean secure,
            @AccountId String accountId) {
        return getFileSystemParameters(workspaceId, blueprintName, clusterName,
                accountName, storageName, fileSystemType, attachedCluster, secure);
    }
}
