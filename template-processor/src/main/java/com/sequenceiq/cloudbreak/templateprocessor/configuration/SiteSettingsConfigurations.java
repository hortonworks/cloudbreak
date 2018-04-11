package com.sequenceiq.cloudbreak.templateprocessor.configuration;

import java.util.*;

public class SiteSettingsConfigurations implements Iterable<List<SiteConfiguration>> {
    private Map<String, List<SiteConfiguration>> config = new HashMap<>();

    private SiteSettingsConfigurations() {

    }

    public static SiteSettingsConfigurations getEmptyConfiguration() {
        return new SiteSettingsConfigurations();
    }

    public void addSiteSettings(SiteConfiguration c) {
        config.putIfAbsent(c.getName(), new ArrayList<>());
        config.get(c.getName()).add(c);
    }

    public void addSiteSettings(String name, Map<String, String> properties) {
        addSiteSettings(new SiteConfiguration(name, properties));
    }

    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Iterator<List<SiteConfiguration>> iterator() {
        return config.values().iterator();
    }
}
