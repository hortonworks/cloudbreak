package com.sequenceiq.cloudbreak.cloud.arm.poller.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class VirtualMachineCheckerContext extends ArmStatusCheckerContext {

    private String groupName;
    private String virtualMachine;
    private String status;

    public VirtualMachineCheckerContext(ArmCredentialView armCredentialView, String groupName, String virtualMachine, String status) {
        super(armCredentialView);
        this.groupName = groupName;
        this.virtualMachine = virtualMachine;
        this.status = status;
    }

    public VirtualMachineCheckerContext(ArmCredentialView armCredentialView, String groupName, String virtualMachine) {
        super(armCredentialView);
        this.groupName = groupName;
        this.virtualMachine = virtualMachine;
        this.status = "deleted";
    }

    public String getGroupName() {
        return groupName;
    }

    public String getVirtualMachine() {
        return virtualMachine;
    }

    public String getStatus() {
        return status;
    }
}
