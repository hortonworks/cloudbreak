package com.sequenceiq.cloudbreak.shell.converter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class InstanceGroupTemplateNameConverter : AbstractConverter<InstanceGroupTemplateName>() {

    @Autowired
    private val context: ShellContext? = null


    override fun supports(type: Class<*>, s: String): Boolean {
        return InstanceGroupTemplateName::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        try {
            return getAllPossibleValues(completions, context!!.activeTemplateNames)
        } catch (e: Exception) {
            return false
        }

    }
}
