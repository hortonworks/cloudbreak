package com.sequenceiq.cloudbreak.service.filesystem;

import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;

@Service
public class FileSystemResolver {

    private static final String NOT_SUPPORTED_FS_PROVIDED = "Unable to determine file system type, or unsupported file system type provided!";

    public FileSystemAwareCloudStorage resolveFileSystem(CloudStorageBase cloudStorageRequest) {
        StorageIdentityBase storageIdentityRequest = cloudStorageRequest.getIdentities().get(0);
        FileSystemAwareCloudStorage cloudStorageParameters;
        if (storageIdentityRequest.getAdls() != null) {
            cloudStorageParameters = storageIdentityRequest.getAdls();
        } else if (storageIdentityRequest.getGcs() != null) {
            cloudStorageParameters = storageIdentityRequest.getGcs();
        } else if (storageIdentityRequest.getWasb() != null) {
            cloudStorageParameters = storageIdentityRequest.getWasb();
        } else if (storageIdentityRequest.getS3() != null) {
            cloudStorageParameters = storageIdentityRequest.getS3();
        } else if (storageIdentityRequest.getAdlsGen2() != null) {
            cloudStorageParameters = storageIdentityRequest.getAdlsGen2();
        } else {
            throw new BadRequestException(NOT_SUPPORTED_FS_PROVIDED);
        }
        return cloudStorageParameters;
    }
}
