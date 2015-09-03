package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public abstract class ArmStatusCheckerContext {
    private ArmCredentialView armCredentialView;

    public ArmStatusCheckerContext(ArmCredentialView armCredentialView) {
        this.armCredentialView = armCredentialView;
    }

    public ArmCredentialView getArmCredentialView() {
        return armCredentialView;
    }
}
