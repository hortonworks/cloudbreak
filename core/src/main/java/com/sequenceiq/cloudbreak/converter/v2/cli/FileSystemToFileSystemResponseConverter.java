package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Component
public class FileSystemToFileSystemResponseConverter extends AbstractConversionServiceAwareConverter<FileSystem, FileSystemResponse> {

    @Override
    public FileSystemResponse convert(FileSystem source) {
        FileSystemResponse response = new FileSystemResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setType(FileSystemType.valueOf(source.getType()));
        response.setDefaultFs(source.isDefaultFs());
        response.setProperties(source.getProperties());
        return response;
    }

}
