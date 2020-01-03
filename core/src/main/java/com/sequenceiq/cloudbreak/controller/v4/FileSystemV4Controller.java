package com.sequenceiq.cloudbreak.controller.v4;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Set;

@Controller
@WorkspaceEntityType(FileSystem.class)
public class FileSystemV4Controller implements FileSystemV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public FileSystemParameterV4Responses getFileSystemParameters(
            Long workspaceId,
            String blueprintName,
            String clusterName,
            String accountName,
            String storageName,
            String fileSystemType,
            Boolean attachedCluster,
            Boolean secure) {
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(blueprintName, clusterName, storageName,
                fileSystemType, accountName, attachedCluster, secure, workspaceId);
        return new FileSystemParameterV4Responses(converterUtil.convertAll(entries, FileSystemParameterV4Response.class));
    }
}
