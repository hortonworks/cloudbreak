package com.sequenceiq.cloudbreak.shell.converter

import org.springframework.shell.core.MethodTarget

import com.sequenceiq.cloudbreak.shell.completion.SecurityRules

class SecurityRulesConverter : AbstractConverter<AbstractCompletion>() {

    override fun supports(type: Class<Any>, optionContext: String): Boolean {
        return SecurityRules::class.java!!.isAssignableFrom(type)
    }

    override fun getAllPossibleValues(list: List<Any>, targetType: Class<Any>, existingData: String, optionContext: String, target: MethodTarget): Boolean {
        return false
    }
}
