package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemParametersV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@WorkspaceEntityType(FileSystem.class)
public class FileSystemV4Controller implements FileSystemV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public FileSystemParameterV4Responses getFileSystemParameters(Long workspaceId, FileSystemParametersV4Filter fileSystemParametersV4Filter) {
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                fileSystemParametersV4Filter.getBlueprintName(),
                fileSystemParametersV4Filter.getClusterName(),
                fileSystemParametersV4Filter.getStorageName(),
                fileSystemParametersV4Filter.getFileSystemType(),
                fileSystemParametersV4Filter.getAccountName(),
                fileSystemParametersV4Filter.isAttachedCluster(),
                workspaceId);
        return new FileSystemParameterV4Responses(converterUtil.convertAll(entries, FileSystemParameterV4Response.class));
    }
}
