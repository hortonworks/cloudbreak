package com.sequenceiq.cloudbreak.cloud.arm.context

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView

class VirtualMachineCheckerContext : ArmStatusCheckerContext {

    var groupName: String? = null
        private set
    var virtualMachine: String? = null
        private set
    var status: String? = null
        private set

    constructor(armCredentialView: ArmCredentialView, groupName: String, virtualMachine: String, status: String) : super(armCredentialView) {
        this.groupName = groupName
        this.virtualMachine = virtualMachine
        this.status = status
    }

    constructor(armCredentialView: ArmCredentialView, groupName: String, virtualMachine: String) : super(armCredentialView) {
        this.groupName = groupName
        this.virtualMachine = virtualMachine
        this.status = "deleted"
    }
}
