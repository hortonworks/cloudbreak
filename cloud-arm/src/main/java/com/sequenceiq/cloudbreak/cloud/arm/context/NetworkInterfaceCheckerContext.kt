package com.sequenceiq.cloudbreak.cloud.arm.context

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView

class NetworkInterfaceCheckerContext(armCredentialView: ArmCredentialView, val groupName: String, val networkName: String) : ArmStatusCheckerContext(armCredentialView)
