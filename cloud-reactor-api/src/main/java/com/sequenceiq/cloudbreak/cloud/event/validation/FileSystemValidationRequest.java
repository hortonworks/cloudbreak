package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;

public class FileSystemValidationRequest extends CloudPlatformRequest<FileSystemValidationResult> {

    private final FileSystem fileSystem;

    private final CloudCredential credential;

    public FileSystemValidationRequest(FileSystem fileSystem, CloudCredential credential, CloudContext cloudContext) {
        super(cloudContext, null);
        this.fileSystem = fileSystem;
        this.credential = credential;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public CloudCredential getCredential() {
        return credential;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FileSystemValidationRequest{");
        sb.append("fileSystem=").append(fileSystem);
        sb.append('}');
        return sb.toString();
    }
}
