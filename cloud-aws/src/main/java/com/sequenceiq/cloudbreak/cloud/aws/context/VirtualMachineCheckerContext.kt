package com.sequenceiq.cloudbreak.cloud.aws.context

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView

class VirtualMachineCheckerContext : AwsStatusCheckerContext {

    var groupName: String? = null
        private set
    var virtualMachine: String? = null
        private set
    var status: String? = null
        private set

    constructor(awsCredentialView: AwsCredentialView, groupName: String, virtualMachine: String, status: String) : super(awsCredentialView) {
        this.groupName = groupName
        this.virtualMachine = virtualMachine
        this.status = status
    }

    constructor(awsCredentialView: AwsCredentialView, groupName: String, virtualMachine: String) : super(awsCredentialView) {
        this.groupName = groupName
        this.virtualMachine = virtualMachine
        this.status = "deleted"
    }
}
