package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(FileSystem.class)
public class FileSystemV4Controller implements FileSystemV4Endpoint {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public FileSystemParameterV4Responses getFileSystemParameters(Long workspaceId, String clusterDefinitionName, String clusterName,
            String accountName, String storageName, String fileSystemType, Boolean attachedCluster) {
        Set<ConfigQueryEntry> entries = clusterDefinitionService.queryFileSystemParameters(clusterDefinitionName, clusterName, storageName,
                fileSystemType, accountName, attachedCluster, workspaceId);
        return new FileSystemParameterV4Responses(converterUtil.convertAll(entries, FileSystemParameterV4Response.class));
    }
}
