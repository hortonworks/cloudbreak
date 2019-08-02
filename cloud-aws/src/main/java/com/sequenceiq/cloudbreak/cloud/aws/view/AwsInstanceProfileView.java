package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;

public class AwsInstanceProfileView {

    private final Optional<SpiFileSystem> cloudFileSystem;

    public AwsInstanceProfileView(CloudStack stack) {
        cloudFileSystem = stack.getFileSystem();
    }

    public boolean isInstanceProfileAvailable() {
        return cloudFileSystem.isPresent() && (cloudFileSystem.get().getCloudFileSystems() instanceof CloudS3View);
    }

    public String getInstanceProfile() {
        return ((CloudS3View) cloudFileSystem.get().getCloudFileSystems()).getInstanceProfile();
    }

}
