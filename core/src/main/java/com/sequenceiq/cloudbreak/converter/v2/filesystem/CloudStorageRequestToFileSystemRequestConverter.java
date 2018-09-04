package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class CloudStorageRequestToFileSystemRequestConverter extends AbstractConversionServiceAwareConverter<CloudStorageRequest, FileSystemRequest> {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Inject
    private FileSystemResolver fileSystemResolver;

    @Override
    public FileSystemRequest convert(CloudStorageRequest source) {
        FileSystemRequest request = new FileSystemRequest();
        request.setDefaultFs(false);
        request.setName(nameGenerator.generateName(FILESYSTEM));
        request.setAdls(source.getAdls());
        request.setGcs(source.getGcs());
        request.setWasb(source.getWasb());
        request.setS3(source.getS3());
        request.setAbfs(source.getAbfs());
        CloudStorageParameters fileSystem = fileSystemResolver.propagateConfiguration(source);
        request.setLocations(source.getLocations());
        request.setType(fileSystem.getType().name());
        return request;
    }

}
