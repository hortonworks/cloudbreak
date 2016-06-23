package com.sequenceiq.cloudbreak.cloud.arm.context

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView

class ResourceGroupCheckerContext(armCredentialView: ArmCredentialView, val groupName: String) : ArmStatusCheckerContext(armCredentialView)
