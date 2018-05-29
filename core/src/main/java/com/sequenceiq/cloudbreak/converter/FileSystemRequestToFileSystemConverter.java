package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import java.util.HashMap;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class FileSystemRequestToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(nameGenerator.generateName(FILESYSTEM));
        fs.setType(source.getType());
        fs.setDefaultFs(source.isDefaultFs());
        if (source.getProperties() != null) {
            fs.setProperties(source.getProperties());
        } else {
            fs.setProperties(new HashMap<>());
        }
        return fs;
    }
}
