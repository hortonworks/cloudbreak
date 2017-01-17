package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

abstract class GroupNameContext extends ArmStatusCheckerContext {

    private final String groupName;

    GroupNameContext(ArmCredentialView armCredentialView, String groupName) {
        super(armCredentialView);
        this.groupName = groupName;
    }

    public final String getGroupName() {
        return groupName;
    }
}
