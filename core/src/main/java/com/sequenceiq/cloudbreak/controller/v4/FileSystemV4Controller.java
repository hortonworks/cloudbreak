package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
    private ConverterUtil converterUtil;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

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
        return new FileSystemParameterV4Responses(converterUtil.convertAll(entries, FileSystemParameterV4Response.class));
    }
}
