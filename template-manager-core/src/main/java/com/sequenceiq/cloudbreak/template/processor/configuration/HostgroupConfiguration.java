package com.sequenceiq.cloudbreak.template.processor.configuration;

import java.util.HashMap;
import java.util.Map;

public class HostgroupConfiguration {
    private final String name;

    private final SiteConfigurations siteConfigs;

    public HostgroupConfiguration(String name, Map<String, Map<String, String>> config) {
        this.name = name;
        siteConfigs = SiteConfigurations.fromMap(config);
    }

    public static HostgroupConfiguration getEmptyConfiguration(String name) {
        return new HostgroupConfiguration(name, new HashMap<>());
    }

    public String getName() {
        return name;
    }

    public void addSiteConfiguration(SiteConfiguration c) {
        siteConfigs.addSiteConfiguration(c);
    }

    public SiteConfigurations getSiteConfigs() {
        return siteConfigs;
    }
}
