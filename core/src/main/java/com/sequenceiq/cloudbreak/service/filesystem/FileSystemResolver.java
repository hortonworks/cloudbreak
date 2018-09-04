package com.sequenceiq.cloudbreak.service.filesystem;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.BaseFileSystem;

@Service
public class FileSystemResolver {

    private static final String NOT_SUPPORTED_FS_PROVIDED = "Unable to decide file system, none of the supported file system type has provided!";

    public CloudStorageParameters propagateConfiguration(CloudStorageRequest source) {
        CloudStorageParameters cloudStorageParameters;
        if (source.getAdls() != null) {
            cloudStorageParameters = source.getAdls();
        } else if (source.getGcs() != null) {
            cloudStorageParameters = source.getGcs();
        } else if (source.getWasb() != null) {
            cloudStorageParameters = source.getWasb();
        } else if (source.getS3() != null) {
            cloudStorageParameters = source.getS3();
        } else if (source.getAbfs() != null) {
            cloudStorageParameters = source.getAbfs();
        } else {
            throw new BadRequestException(NOT_SUPPORTED_FS_PROVIDED);
        }
        return cloudStorageParameters;
    }

    public BaseFileSystem propagateConfiguration(FileSystem source) {
        BaseFileSystem fileSystem;
        try {
            fileSystem = source.getConfigurations().get(BaseFileSystem.class);
        } catch (IOException e) {
            throw new BadRequestException(NOT_SUPPORTED_FS_PROVIDED);
        }
        return fileSystem;
    }

}
