package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class NetworkInterfaceCheckerContext extends ArmStatusCheckerContext {

    private String groupName;
    private String networkName;

    public NetworkInterfaceCheckerContext(ArmCredentialView armCredentialView, String groupName, String networkName) {
        super(armCredentialView);
        this.groupName = groupName;
        this.networkName = networkName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getNetworkName() {
        return networkName;
    }

}
