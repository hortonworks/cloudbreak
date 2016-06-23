package com.sequenceiq.cloudbreak.shell.converter

import java.util.Arrays

import org.springframework.shell.core.Completion
import org.springframework.shell.core.MethodTarget

import com.google.common.base.Function
import com.google.common.collect.Collections2
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType

class SssdSchemaTypeConverter : AbstractConverter<SssdSchemaType>() {

    init {
        values = Collections2.transform<SssdSchemaType, String>(Arrays.asList<SssdSchemaType>(*com.sequenceiq.cloudbreak.api.model.SssdSchemaType.values()),
                Function<com.sequenceiq.cloudbreak.api.model.SssdSchemaType, kotlin.String> { input -> input!!.name })
    }

    override fun supports(type: Class<*>, optionContext: String): Boolean {
        return SssdSchemaType::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(completions: MutableList<Completion>, targetType: Class<*>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        return getAllPossibleValues(completions, values)
    }

    companion object {

        private var values: Collection<String>? = null
    }
}
