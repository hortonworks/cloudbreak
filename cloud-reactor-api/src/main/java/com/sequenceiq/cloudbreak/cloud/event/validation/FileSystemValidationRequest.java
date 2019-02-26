package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;

public class FileSystemValidationRequest extends CloudPlatformRequest<FileSystemValidationResult<? extends FileSystemValidationRequest>> {

    private final SpiFileSystem spiFileSystem;

    private final CloudCredential credential;

    public FileSystemValidationRequest(SpiFileSystem spiFileSystem, CloudCredential credential, CloudContext cloudContext) {
        super(cloudContext, null);
        this.spiFileSystem = spiFileSystem;
        this.credential = credential;
    }

    public SpiFileSystem getSpiFileSystem() {
        return spiFileSystem;
    }

    public CloudCredential getCredential() {
        return credential;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FileSystemValidationRequest{");
        sb.append("spiFileSystem=").append(spiFileSystem);
        sb.append('}');
        return sb.toString();
    }
}
