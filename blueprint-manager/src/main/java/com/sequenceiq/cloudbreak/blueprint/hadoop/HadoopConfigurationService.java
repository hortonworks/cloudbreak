package com.sequenceiq.cloudbreak.blueprint.hadoop;

import static com.sequenceiq.cloudbreak.blueprint.HostgroupEntry.hostgroupEntry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.blueprint.HdfClusterLocator;
import com.sequenceiq.cloudbreak.blueprint.HostgroupEntry;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.domain.HostGroup;

@Service
public class HadoopConfigurationService implements BlueprintComponentConfigProvider {

    @Inject
    private HdfClusterLocator hdfClusterLocator;

    @Inject
    private ConfigService configService;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) throws IOException {
        Map<String, Map<String, Map<String, String>>> hostGroupConfig = configService.getHostGroupConfiguration(blueprintText, source.getHostGroups());
        blueprintText = source.getAmbariClient().extendBlueprintHostGroupConfiguration(blueprintText, hostGroupConfig);
        return blueprintText;
    }

    @Override
    public Map<HostgroupEntry, List<BlueprintConfigurationEntry>> getHostgroupConfigurationEntries(BlueprintPreparationObject source, String blueprintText)
            throws IOException {
        Map<String, Map<String, Map<String, String>>> hostGroupConfiguration = getHostGroupConfiguration(blueprintText, source.getHostGroups());
        Map<HostgroupEntry, List<BlueprintConfigurationEntry>> blueprintConfigurationEntries = new HashMap<>();

        for (Entry<String, Map<String, Map<String, String>>> entry : hostGroupConfiguration.entrySet()) {

            List<BlueprintConfigurationEntry> blueprintConfigurationEntriesList = new ArrayList<>();
            for (Entry<String, Map<String, String>> stringMapEntry : entry.getValue().entrySet()) {
                for (Entry<String, String> innerEntry : stringMapEntry.getValue().entrySet()) {
                    blueprintConfigurationEntriesList.add(new BlueprintConfigurationEntry(stringMapEntry.getKey(), innerEntry.getKey(), innerEntry.getValue()));
                }
            }
            blueprintConfigurationEntries.put(hostgroupEntry(entry.getKey()), blueprintConfigurationEntriesList);
        }
        return blueprintConfigurationEntries;
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return !hdfClusterLocator.hdfCluster(source.getStackRepoDetails());
    }

    @Override
    public boolean ldapConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        return source.getLdapView().isPresent();
    }

    @Override
    public List<BlueprintConfigurationEntry> ldapConfigs(BlueprintPreparationObject source, String blueprintText) {
        String configFile = "core-site";

        LdapView ldapView = source.getLdapView().get();
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping", "org.apache.hadoop.security.LdapGroupsMapping"));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.url", ldapView.getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.bind.user", ldapView.getBindDn()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.bind.password", ldapView.getBindPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.userbase", ldapView.getUserSearchBase()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.search.filter.user",
                String.format("(&(objectClass=%s)(%s={0}))", ldapView.getUserObjectClass(), ldapView.getUserNameAttribute())));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.groupbase", ldapView.getGroupSearchBase()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.search.filter.group",
                String.format("(objectClass=%s)", ldapView.getGroupObjectClass())));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.search.attr.group.name",
                ldapView.getGroupNameAttribute()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.security.group.mapping.ldap.search.attr.member",
                ldapView.getGroupMemberAttribute()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hadoop.user.group.static.mapping.overrides",
                "hive=hive,hadoop;hdfs=hdfs,hadoop;beacon=hadoop,hdfs,beacon;dpprofiler=hadoop"));

        return configs;
    }


    private Map<String, Map<String, String>> getGlobalConfiguration(String blueprintText, Set<HostGroup> hostGroups) throws IOException {
        Map<String, Map<String, String>> config = new HashMap<>();
        JsonNode blueprintNode = JsonUtil.readTree(blueprintText);
        JsonNode hostGroupsBp = blueprintNode.path("host_groups");
        for (JsonNode hostGroupNode : hostGroupsBp) {
            HostGroup hostGroup = findHostGroupForNode(hostGroups, hostGroupNode);
            JsonNode components = hostGroupNode.path("components");
            for (JsonNode component : components) {
                String name = component.path("name").asText();
                Integer volumeCount = -1;
                if (hostGroup.getConstraint().getInstanceGroup() != null) {
                    volumeCount = null;
                }
                config.putAll(getProperties(name, true, volumeCount, hostGroup, blueprintText));
            }
        }
        for (Entry<String, Map<String, String>> entry : bpConfigs.entrySet()) {
            if (config.containsKey(entry.getKey())) {
                for (Entry<String, String> inEntry : entry.getValue().entrySet()) {
                    config.get(entry.getKey()).put(inEntry.getKey(), inEntry.getValue());
                }
            } else {
                config.put(entry.getKey(), entry.getValue());
            }
        }
        return config;
    }
}
