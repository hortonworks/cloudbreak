package com.sequenceiq.cloudbreak.template.processor.configuration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SiteConfigurations implements Iterable<SiteConfiguration> {
    private final Map<String, SiteConfiguration> config = new HashMap<>();

    private SiteConfigurations() {

    }

    public static SiteConfigurations getEmptyConfiguration() {
        return new SiteConfigurations();
    }

    public static SiteConfigurations fromMap(Map<String, Map<String, String>> configuration) {
        SiteConfigurations result = new SiteConfigurations();
        for (Entry<String, Map<String, String>> conf : configuration.entrySet()) {
            result.addSiteConfiguration(new SiteConfiguration(conf.getKey(), conf.getValue()));
        }
        return result;
    }

    public void addSiteConfiguration(SiteConfiguration c) {
        if (config.containsKey(c.getName())) {
            config.get(c.getName()).getProperties().putAll(c.getProperties());
        } else {
            config.put(c.getName(), c);
        }
    }

    public void addSiteConfiguration(String name, Map<String, String> properties) {
        addSiteConfiguration(new SiteConfiguration(name, properties));
    }

    public void addConfiguration(String site, String key, String value) {
        SiteConfiguration c = config.putIfAbsent(site, SiteConfiguration.getEmptyConfiguration(site));
        c.add(key, value);
    }

    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Iterator<SiteConfiguration> iterator() {
        return config.values().iterator();
    }
}
