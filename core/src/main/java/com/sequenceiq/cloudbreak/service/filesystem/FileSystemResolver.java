package com.sequenceiq.cloudbreak.service.filesystem;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.services.filesystem.BaseFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Service
public class FileSystemResolver {

    private static final String NOT_SUPPORTED_FS_PROVIDED = "Unable to decide file system, none of the supported file system type has provided!";

    public CloudStorageParametersV4 propagateConfiguration(CloudStorageV4Request source) {
        CloudStorageParametersV4 cloudStorageParameters;
        if (source.getAdls() != null) {
            cloudStorageParameters = source.getAdls();
        } else if (source.getGcs() != null) {
            cloudStorageParameters = source.getGcs();
        } else if (source.getWasb() != null) {
            cloudStorageParameters = source.getWasb();
        } else if (source.getS3() != null) {
            cloudStorageParameters = source.getS3();
        } else if (source.getAdlsGen2() != null) {
            cloudStorageParameters = source.getAdlsGen2();
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
