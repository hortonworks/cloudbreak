package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Component
public class FileSystemToFileSystemRequestConverter extends AbstractConversionServiceAwareConverter<FileSystem, FileSystemRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToFileSystemRequestConverter.class);

    @Override
    public FileSystemRequest convert(FileSystem source) {
        FileSystemRequest fileSystemRequest = new FileSystemRequest();
        fileSystemRequest.setName(source.getName());
        fileSystemRequest.setProperties(source.getProperties());
        fileSystemRequest.setType(FileSystemType.valueOf(source.getType()));
        return fileSystemRequest;
    }

}
