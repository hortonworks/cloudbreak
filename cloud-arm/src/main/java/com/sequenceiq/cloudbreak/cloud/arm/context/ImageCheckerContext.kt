package com.sequenceiq.cloudbreak.cloud.arm.context

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView

class ImageCheckerContext(armCredentialView: ArmCredentialView, val groupName: String, val storageName: String, val containerName: String, val sourceBlob: String) : ArmStatusCheckerContext(armCredentialView)
