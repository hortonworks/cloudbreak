package com.sequenceiq.cloudbreak.shell.converter

import java.util.Arrays

import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.shell.completion.OpenStackFacing

class OpenStackFacingConverter : AbstractConverter<OpenStackFacing>() {

    override fun supports(type: Class<*>, optionContext: String): Boolean {
        return OpenStackFacing::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        return getAllPossibleValues(completions, values)
    }

    companion object {

        private val values = Arrays.asList("admin", "public", "internal")
    }
}
