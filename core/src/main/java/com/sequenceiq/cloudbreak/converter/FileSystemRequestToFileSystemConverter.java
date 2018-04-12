package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class FileSystemRequestToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {
    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(source.getName());
        fs.setType(source.getType().name());
        fs.setDefaultFs(source.isDefaultFs());
        if (source.getProperties() != null) {
            fs.setProperties(source.getProperties());
        } else {
            fs.setProperties(new HashMap<>());
        }
        return fs;
    }
}
