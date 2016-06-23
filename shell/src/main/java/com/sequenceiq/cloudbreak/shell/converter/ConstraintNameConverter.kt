package com.sequenceiq.cloudbreak.shell.converter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.shell.completion.ConstraintName
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class ConstraintNameConverter : AbstractConverter<ConstraintName>() {

    @Autowired
    private val shellContext: ShellContext? = null

    override fun supports(type: Class<*>, s: String): Boolean {
        return ConstraintName::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        try {
            return getAllPossibleValues(completions, shellContext!!.constraints)
        } catch (e: Exception) {
            return false
        }

    }
}
