package com.sequenceiq.cloudbreak.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.SettingsEndpoint;
import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.SssdProviderType;
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType;
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType;

@Component
public class SettingsController implements SettingsEndpoint {

    @Override
    public Map<String, Map<String, Object>> getAllSettings() {
        Map<String, Map<String, Object>> settings = new HashMap<>();
        settings.put("sssdConfig", bundleSssdConfigSettings());
        settings.put("recipe", bundleRecipeSettings());
        return settings;
    }

    @Override
    public Map<String, Object> getRecipeSettings() {
        return bundleRecipeSettings();
    }

    private Map<String, Object> bundleRecipeSettings() {
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("executionTypes", ExecutionType.values());
        return recipe;
    }

    @Override
    public Map<String, Object> getSssdConfigSettings() {
        return bundleSssdConfigSettings();
    }

    private Map<String, Object> bundleSssdConfigSettings() {
        Map<String, Object> sssdConfig = new HashMap<>();
        sssdConfig.put("providerTypes", SssdProviderType.values());
        sssdConfig.put("schemaTypes", SssdSchemaType.values());
        sssdConfig.put("tlsReqcertTypes", SssdTlsReqcertType.values());
        return sssdConfig;
    }
}
