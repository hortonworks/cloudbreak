package com.sequenceiq.cloudbreak.converter.v4.filesystems;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;

@Component
public class ConfigQueryEntryToFileSystemParameterV4ResponseConverter {

    public FileSystemParameterV4Response convert(ConfigQueryEntry source) {
        FileSystemParameterV4Response fileSystemParameterV4Response = new FileSystemParameterV4Response();
        fileSystemParameterV4Response.setType(source.getType().name());
        fileSystemParameterV4Response.setDefaultPath(source.getDefaultPath());
        fileSystemParameterV4Response.setDescription(source.getDescription());
        fileSystemParameterV4Response.setPropertyName(source.getPropertyName());
        fileSystemParameterV4Response.setRelatedServices(source.getRelatedServices());
        fileSystemParameterV4Response.setRelatedMissingServices(source.getRelatedMissingServices());
        fileSystemParameterV4Response.setPropertyFile(source.getPropertyFile());
        fileSystemParameterV4Response.setProtocol(source.getProtocol());
        fileSystemParameterV4Response.setPropertyDisplayName(source.getPropertyDisplayName());
        fileSystemParameterV4Response.setSecure(source.isSecure());
        return fileSystemParameterV4Response;
    }
}
