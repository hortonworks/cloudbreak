package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.api.model.v2.FileSystemV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class FileSystemV2RequestToFileSystemRequestConverter extends AbstractConversionServiceAwareConverter<FileSystemV2Request, FileSystemRequest> {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Override
    public FileSystemRequest convert(FileSystemV2Request source) {
        FileSystemRequest request = new FileSystemRequest();
        request.setDefaultFs(false);
        request.setName(nameGenerator.generateName(FILESYSTEM));
        FileSystemParameters fileSystem = FileSystemResolver.decideFileSystemFromFileSystemV2Request(source);
        request.setType(fileSystem.getType());
        request.setProperties(fileSystem.getAsMap());
        return request;
    }

}
