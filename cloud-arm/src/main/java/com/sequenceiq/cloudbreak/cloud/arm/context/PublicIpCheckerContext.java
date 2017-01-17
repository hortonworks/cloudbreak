package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class PublicIpCheckerContext extends GroupNameContext {

    private String addressName;

    public PublicIpCheckerContext(ArmCredentialView armCredentialView, String groupName, String addressName) {
        super(armCredentialView, groupName);
        this.addressName = addressName;
    }

    public String getAddressName() {
        return addressName;
    }

}
