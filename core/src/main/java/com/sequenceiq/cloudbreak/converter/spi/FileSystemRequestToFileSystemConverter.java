package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class FileSystemRequestToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {
    @Override
    public FileSystem convert(FileSystemRequest source) {
        return new FileSystem(source.getName(), source.getType().name(), source.isDefaultFs(), source.getProperties());
    }
}
