package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Component
public class JsonToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {
    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(source.getName());
        fs.setType(source.getType());
        fs.setDefaultFs(source.isDefaultFs());
        fs.setProperties(source.getProperties());
        return fs;
    }
}
