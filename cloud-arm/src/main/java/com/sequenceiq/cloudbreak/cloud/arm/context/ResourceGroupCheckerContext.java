package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class ResourceGroupCheckerContext extends GroupNameContext {

    public ResourceGroupCheckerContext(ArmCredentialView armCredentialView, String groupName) {
        super(armCredentialView, groupName);
    }
}
