package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;

public class FileSystemValidationRequest extends CloudPlatformRequest<FileSystemValidationResult> {

    private final FileSystem fileSystem;

    public FileSystemValidationRequest(FileSystem fileSystem, CloudContext cloudContext) {
        super(cloudContext, null);
        this.fileSystem = fileSystem;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FileSystemValidationRequest{");
        sb.append("fileSystem=").append(fileSystem);
        sb.append('}');
        return sb.toString();
    }
}
