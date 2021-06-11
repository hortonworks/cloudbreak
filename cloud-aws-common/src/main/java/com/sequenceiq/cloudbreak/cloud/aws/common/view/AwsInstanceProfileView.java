package com.sequenceiq.cloudbreak.cloud.aws.common.view;

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
        return getInstanceProfile() != null;
    }

    public String getInstanceProfile() {
        String instanceProfile = null;
        if (cloudFileSystem.isPresent() && (cloudFileSystem.get().getCloudFileSystems() instanceof CloudS3View)) {
            instanceProfile = ((CloudS3View) cloudFileSystem.get().getCloudFileSystems()).getInstanceProfile();
        }
        return instanceProfile;
    }
}
