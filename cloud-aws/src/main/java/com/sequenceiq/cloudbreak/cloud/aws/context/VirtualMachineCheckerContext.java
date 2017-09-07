package com.sequenceiq.cloudbreak.cloud.aws.context;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

public class VirtualMachineCheckerContext extends AwsStatusCheckerContext {

    private final String groupName;

    private final String virtualMachine;

    private final String status;

    public VirtualMachineCheckerContext(AwsCredentialView awsCredentialView, String groupName, String virtualMachine, String status) {
        super(awsCredentialView);
        this.groupName = groupName;
        this.virtualMachine = virtualMachine;
        this.status = status;
    }

    public VirtualMachineCheckerContext(AwsCredentialView awsCredentialView, String groupName, String virtualMachine) {
        super(awsCredentialView);
        this.groupName = groupName;
        this.virtualMachine = virtualMachine;
        status = "deleted";
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
