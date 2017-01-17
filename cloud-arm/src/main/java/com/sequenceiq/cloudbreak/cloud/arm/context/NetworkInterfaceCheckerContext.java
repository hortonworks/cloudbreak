package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class NetworkInterfaceCheckerContext extends GroupNameContext {

    private String networkName;

    public NetworkInterfaceCheckerContext(ArmCredentialView armCredentialView, String groupName, String networkName) {
        super(armCredentialView, groupName);
        this.networkName = networkName;
    }

    public String getNetworkName() {
        return networkName;
    }
}
