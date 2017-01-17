package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class ImageCheckerContext extends GroupNameContext {

    private String storageName;

    private String containerName;

    private String sourceBlob;

    public ImageCheckerContext(ArmCredentialView armCredentialView, String groupName, String storageName, String containerName, String sourceBlob) {
        super(armCredentialView, groupName);
        this.storageName = storageName;
        this.containerName = containerName;
        this.sourceBlob = sourceBlob;
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
