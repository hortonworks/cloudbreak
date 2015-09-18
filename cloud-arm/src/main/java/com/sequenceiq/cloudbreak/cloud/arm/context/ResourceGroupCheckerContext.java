package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class ResourceGroupCheckerContext extends ArmStatusCheckerContext {

    private String groupName;

    public ResourceGroupCheckerContext(ArmCredentialView armCredentialView, String groupName) {
        super(armCredentialView);
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }
}
