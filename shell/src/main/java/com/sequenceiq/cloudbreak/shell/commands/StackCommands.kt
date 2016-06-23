package com.sequenceiq.cloudbreak.shell.commands

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone
import com.sequenceiq.cloudbreak.shell.completion.StackRegion

interface StackCommands {

    fun create(name: String,
               region: StackRegion,
               availabilityZone: StackAvailabilityZone,
               publicInAccount: Boolean?,
               onFailureAction: OnFailureAction,
               adjustmentType: AdjustmentType,
               threshold: Long?,
               relocateDocker: Boolean?,
               wait: Boolean?,
               platformVariant: PlatformVariant,
               orchestrator: String,
               platform: String,
               ambariVersion: String,
               hdpVersion: String,
               params: Map<String, String>): String

    fun createStackAvailable(platform: String): Boolean
}
