package com.sequenceiq.cloudbreak.cloud.arm.context

import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView

class StorageCheckerContext(armCredentialView: ArmCredentialView, val groupName: String, val storageName: String,
                            val expectedStatus: StorageStatus) : ArmStatusCheckerContext(armCredentialView)
