package com.sequenceiq.cloudbreak.template.processor.configuration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HostgroupConfigurations implements Iterable<HostgroupConfiguration> {
    private Map<String, HostgroupConfiguration> config = new HashMap<>();

    private HostgroupConfigurations() {
    }

    public static HostgroupConfigurations getEmptyConfiguration() {
        return new HostgroupConfigurations();
    }

    public static HostgroupConfigurations fromMap(Map<String, Map<String, Map<String, String>>> configuration) {
        HostgroupConfigurations result = new HostgroupConfigurations();
        for (Map.Entry<String, Map<String, Map<String, String>>> conf : configuration.entrySet()) {
            result.addHostgroupConfiguration(new HostgroupConfiguration(conf.getKey(), conf.getValue()));
        }
        return result;
    }

    public static HostgroupConfigurations fromConfigEntryMap(Map<HostgroupEntry, List<ClusterDefinitionConfigurationEntry>> configuration) {
        HostgroupConfigurations result = new HostgroupConfigurations();
        for (Map.Entry<HostgroupEntry, List<ClusterDefinitionConfigurationEntry>> conf : configuration.entrySet()) {
            Map<String, Map<String, String>> config = new HashMap<>();
            for (ClusterDefinitionConfigurationEntry clusterDefinitionConfigurationEntry : conf.getValue()) {
                if (!config.keySet().contains(clusterDefinitionConfigurationEntry.getConfigFile())) {
                    config.put(clusterDefinitionConfigurationEntry.getConfigFile(), new HashMap<>());
                }
                config.get(clusterDefinitionConfigurationEntry.getConfigFile()).put(clusterDefinitionConfigurationEntry.getKey(),
                        clusterDefinitionConfigurationEntry.getValue());
            }
            result.addHostgroupConfiguration(new HostgroupConfiguration(conf.getKey().getHostGroup(), config));
        }
        return result;
    }

    public void addHostgroupConfiguration(HostgroupConfiguration c) {
        config.put(c.getName(), c);
    }

    public HostgroupConfigurations getFilteredConfigs(Set<String> globalConfigs, boolean forced) {
        HostgroupConfigurations result = HostgroupConfigurations.getEmptyConfiguration();
        if (forced) {
            result = this;
        } else {
            for (HostgroupConfiguration newConfig : config.values()) {
                String hostgroup = newConfig.getName();
                HostgroupConfiguration filteredHostgroupConfig = HostgroupConfiguration.getEmptyConfiguration(hostgroup);
                result.addHostgroupConfiguration(filteredHostgroupConfig);

                for (SiteConfiguration siteConfig : newConfig.getSiteConfigs()) {
                    SiteConfiguration filteredSiteConfig = SiteConfiguration.getEmptyConfiguration(siteConfig.getName());
                    filteredHostgroupConfig.addSiteConfiguration(filteredSiteConfig);
                    for (Map.Entry<String, String> siteProp : siteConfig.getProperties().entrySet()) {
                        if (!globalConfigs.contains(siteProp.getKey())) {
                            filteredSiteConfig.getProperties().put(siteProp.getKey(), siteProp.getValue());
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Iterator<HostgroupConfiguration> iterator() {
        return config.values().iterator();
    }
}
