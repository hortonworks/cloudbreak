package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class ImageCheckerContext extends ArmStatusCheckerContext {

    private String groupName;
    private String storageName;
    private String containerName;
    private String sourceBlob;

    public ImageCheckerContext(ArmCredentialView armCredentialView, String groupName, String storageName, String containerName, String sourceBlob) {
        super(armCredentialView);
        this.groupName = groupName;
        this.storageName = storageName;
        this.containerName = containerName;
        this.sourceBlob = sourceBlob;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStorageName() {
        return storageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getSourceBlob() {
        return sourceBlob;
    }
}
