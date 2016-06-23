package com.sequenceiq.cloudbreak.validation


import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

import org.apache.commons.codec.binary.Base64

import com.sequenceiq.cloudbreak.api.model.ExecutionType

class PluginValidator : ConstraintValidator<ValidPlugin, Map<String, ExecutionType>> {

    override fun initialize(validPlugin: ValidPlugin) {
    }

    override fun isValid(plugins: Map<String, ExecutionType>?, cxt: ConstraintValidatorContext): Boolean {
        if (plugins == null || plugins.isEmpty()) {
            return false
        }
        for (url in plugins.keys) {
            if (!url.matches(URL_PATTERN.toRegex())) {
                return false
            } else if (url.startsWith("consul://") && !url.matches(CONSUL_PATTERN.toRegex())) {
                return false
            } else if (url.startsWith("base64://") && !isValidBase64Plugin(url.replaceFirst("base64://".toRegex(), ""))) {
                return false
            }
        }
        return true
    }

    private fun isValidBase64Plugin(pluginContent: String): Boolean {
        if (!Base64.isBase64(pluginContent)) {
            return false
        }
        val content = String(Base64.decodeBase64(pluginContent))
        if (content.isEmpty()) {
            return false
        }

        var tomlFound = false
        var scriptFound = false
        for (line in content.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            if (line.startsWith("plugin.toml:")) {
                tomlFound = true
            } else if (line.matches(SCRIPT_PATTERN.toRegex())) {
                scriptFound = true
            }
        }

        return tomlFound && scriptFound
    }

    companion object {

        private val RECIPE_KEY_PREFIX = "consul-watch-plugin/"
        private val URL_PATTERN = "^(http|https|git|consul|base64)://.*"
        private val CONSUL_PATTERN = "^consul://$RECIPE_KEY_PREFIX([a-z][-a-z0-9]*[a-z0-9])"
        private val SCRIPT_PATTERN = "^(recipe-pre-install|recipe-post-install):.*"
    }
}