package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class FileSystemRequestToSpiFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, SpiFileSystem> {
    @Override
    public SpiFileSystem convert(FileSystemRequest source) {
        return new SpiFileSystem(source.getName(), source.getType().name(), source.isDefaultFs(), source.getProperties());
    }
}
