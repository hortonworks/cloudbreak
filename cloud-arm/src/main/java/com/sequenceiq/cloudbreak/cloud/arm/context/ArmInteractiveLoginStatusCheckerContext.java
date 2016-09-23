package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class ArmInteractiveLoginStatusCheckerContext {

    private Boolean cancelled = false;
    private String deviceCode;
    private ExtendedCloudCredential extendedCloudCredential;

    public ArmInteractiveLoginStatusCheckerContext(String deviceCode, ExtendedCloudCredential extendedCloudCredential) {
        this.deviceCode = deviceCode;
        this.extendedCloudCredential = extendedCloudCredential;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }
}
