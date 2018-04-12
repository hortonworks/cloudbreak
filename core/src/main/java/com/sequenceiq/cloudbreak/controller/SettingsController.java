package com.sequenceiq.cloudbreak.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SettingsEndpoint;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;

@Component
public class SettingsController implements SettingsEndpoint {

    @Value("${cb.host.discovery.custom.domain:}")
    private String customDomain;

    @Override
    public Map<String, Map<String, Object>> getAllSettings() {
        Map<String, Map<String, Object>> settings = new HashMap<>();
        settings.put("recipe", bundleRecipeSettings());
        settings.put("database", bundleDatabaseConfigSettings());
        settings.put("domain", bundleDomainSettings());
        return settings;
    }

    private Map<String, Object> bundleDomainSettings() {
        Map<String, Object> domainSettings = new HashMap<>();
        domainSettings.put("defaultCustomDomain", customDomain);
        return domainSettings;
    }

    @Override
    public Map<String, Object> getRecipeSettings() {
        return bundleRecipeSettings();
    }

    private Map<String, Object> bundleRecipeSettings() {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getDatabaseConfigSettings() {
        return bundleDatabaseConfigSettings();
    }

    private Map<String, Object> bundleDatabaseConfigSettings() {
        Map<String, Object> dbConfig = new HashMap<>();
        dbConfig.put("vendor", DatabaseVendor.availableVendors());
        return dbConfig;
    }
}
