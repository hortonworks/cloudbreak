package com.sequenceiq.cloudbreak.controller

import java.util.HashMap

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.SettingsEndpoint
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.SssdProviderType
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType

@Component
class SettingsController : SettingsEndpoint {

    override fun getAllSettings(): Map<String, Map<String, Any>> {
        val settings = HashMap<String, Map<String, Any>>()
        settings.put("sssdConfig", bundleSssdConfigSettings())
        settings.put("recipe", bundleRecipeSettings())
        return settings
    }

    override fun getRecipeSettings(): Map<String, Any> {
        return bundleRecipeSettings()
    }

    private fun bundleRecipeSettings(): Map<String, Any> {
        val recipe = HashMap<String, Any>()
        recipe.put("executionTypes", ExecutionType.values())
        return recipe
    }

    override fun getSssdConfigSettings(): Map<String, Any> {
        return bundleSssdConfigSettings()
    }

    private fun bundleSssdConfigSettings(): Map<String, Any> {
        val sssdConfig = HashMap<String, Any>()
        sssdConfig.put("providerTypes", SssdProviderType.values())
        sssdConfig.put("schemaTypes", SssdSchemaType.values())
        sssdConfig.put("tlsReqcertTypes", SssdTlsReqcertType.values())
        return sssdConfig
    }
}
