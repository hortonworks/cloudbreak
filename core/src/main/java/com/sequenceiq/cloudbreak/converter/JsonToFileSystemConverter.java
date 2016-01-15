package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Component
public class JsonToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {
    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(source.getName());
        fs.setType(source.getType().name());
        fs.setDefaultFs(source.isDefaultFs());
        if (source.getProperties() != null) {
            fs.setProperties(source.getProperties());
        } else {
            fs.setProperties(new HashMap<String, String>());
        }
        return fs;
    }
}
