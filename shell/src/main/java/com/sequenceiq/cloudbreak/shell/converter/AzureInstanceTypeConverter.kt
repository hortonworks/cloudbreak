package com.sequenceiq.cloudbreak.shell.converter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class AzureInstanceTypeConverter : AbstractConverter<AzureInstanceType>() {
    @Autowired
    private val context: ShellContext? = null

    override fun supports(type: Class<*>, optionContext: String): Boolean {
        return AzureInstanceType::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        try {
            return getAllPossibleValues(completions, context!!.getInstanceTypeNamesByPlatform("AZURE_RM"))
        } catch (e: Exception) {
            return false
        }

    }
}
