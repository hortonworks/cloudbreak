package com.sequenceiq.cloudbreak.shell.converter

import java.util.Arrays

import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.google.common.base.Function
import com.google.common.collect.Collections2
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.shell.completion.PluginExecutionType

class PluginExecutionTypeConverter : AbstractConverter<PluginExecutionType>() {

    init {
        values = Collections2.transform(Arrays.asList(*ExecutionType.values())
        ) { input -> input!!.name }
    }

    override fun supports(type: Class<*>, optionContext: String): Boolean {
        return PluginExecutionType::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        return getAllPossibleValues(completions, values)
    }

    companion object {

        private var values: Collection<String>? = null
    }
}
