package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class VirtualMachineCheckerContext extends GroupNameContext {

    private String virtualMachine;

    private String status;

    public VirtualMachineCheckerContext(ArmCredentialView armCredentialView, String groupName, String virtualMachine, String status) {
        super(armCredentialView, groupName);
        this.virtualMachine = virtualMachine;
        this.status = status;
    }

    public VirtualMachineCheckerContext(ArmCredentialView armCredentialView, String groupName, String virtualMachine) {
        super(armCredentialView, groupName);
        this.virtualMachine = virtualMachine;
        this.status = "deleted";
    }

    public String getVirtualMachine() {
        return virtualMachine;
    }

    public String getStatus() {
        return status;
    }
}
