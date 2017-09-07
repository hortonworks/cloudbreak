package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.buildVolumePathString;
import static com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.getLogVolume;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class HadoopConfigurationService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    private final Map<String, ServiceConfig> serviceConfigs = new HashMap<>();

    private final Map<String, Map<String, String>> bpConfigs = new HashMap<>();

    @Value("#{'${cb.byos.dfs.data.dir}'.split('\\,')}")
    private List<String> byosDfsDataDirs;

    @PostConstruct
    public void init() throws IOException {
        String serviceConfigJson = FileReaderUtils.readFileFromClasspath("hdp/service-config.json");
        JsonNode services = JsonUtil.readTree(serviceConfigJson).get("services");
        for (JsonNode service : services) {
            String serviceName = service.get("name").asText();
            List<String> relatedServices = new ArrayList<>();
            JsonNode relatedServicesNode = service.get("related_services");
            for (JsonNode node : relatedServicesNode) {
                relatedServices.add(node.asText());
            }
            JsonNode configurations = service.get("configurations");
            Map<String, List<ConfigProperty>> globalConfig = new HashMap<>();
            Map<String, List<ConfigProperty>> hostConfig = new HashMap<>();
            for (JsonNode config : configurations) {
                String type = config.get("type").asText();
                List<ConfigProperty> global = toList(config.get("global"));
                if (!global.isEmpty()) {
                    globalConfig.put(type, global);
                }
                List<ConfigProperty> host = toList(config.get("host"));
                if (!host.isEmpty()) {
                    hostConfig.put(type, host);
                }
            }
            serviceConfigs.put(serviceName, new ServiceConfig(serviceName, relatedServices, globalConfig, hostConfig));
        }

        String bpConfigJson = FileReaderUtils.readFileFromClasspath("hdp/bp-config.json");
        JsonNode bps = JsonUtil.readTree(bpConfigJson).get("sites");
        for (JsonNode bp : bps) {
            String siteName = bp.get("name").asText();
            JsonNode configurations = bp.get("configurations");
            Map<String, String> keyVals = new HashMap<>();
            for (JsonNode config : configurations) {
                String key = config.get("key").asText();
                String value = config.get("value").asText();
                keyVals.put(key, value);
            }
            bpConfigs.put(siteName, keyVals);
        }
    }

    public Map<String, Map<String, String>> getGlobalConfiguration(Cluster cluster) throws IOException {
        Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
        Map<String, Map<String, String>> config = new HashMap<>();
        String blueprintText = cluster.getBlueprint().getBlueprintText();
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

    private HostGroup findHostGroupForNode(Set<HostGroup> hostGroups, JsonNode hostGroupNode) {
        for (HostGroup hostGroup : hostGroups) {
            if (hostGroup.getName().equals(hostGroupNode.path("name").asText())) {
                return hostGroup;
            }
        }
        throw new CloudbreakServiceException("Couldn't find a saved hostgroup for the hostgroup in the blueprint.");
    }

    public Map<String, Map<String, Map<String, String>>> getHostGroupConfiguration(Cluster cluster) {
        Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
        Map<String, Map<String, Map<String, String>>> hadoopConfig = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            Map<String, Map<String, String>> componentConfig = new HashMap<>();
            Integer volumeCount = -1;
            if (hostGroup.getConstraint().getInstanceGroup().getTemplate() != null) {
                volumeCount = hostGroup.getConstraint().getInstanceGroup().getTemplate().getVolumeCount();
            }
            if (configUpdateNeeded(hostGroup)) {
                for (String serviceName : serviceConfigs.keySet()) {
                    componentConfig.putAll(getProperties(serviceName, false, volumeCount, hostGroup, cluster.getBlueprint().getBlueprintText()));
                }
                hadoopConfig.put(hostGroup.getName(), componentConfig);
            }
        }
        return hadoopConfig;
    }

    private boolean configUpdateNeeded(HostGroup hostGroup) {
        return hostGroup.getConstraint().getInstanceGroup() != null || (byosDfsDataDirs != null && !byosDfsDataDirs.isEmpty());
    }

    private List<ConfigProperty> toList(JsonNode nodes) {
        List<ConfigProperty> list = new ArrayList<>();
        for (JsonNode node : nodes) {
            list.add(new ConfigProperty(node.get("name").asText(), node.get("directory").asText(), node.get("prefix").asText()));
        }
        return list;
    }

    private Map<String, Map<String, String>> getProperties(String name, boolean global, Integer volumeCount, HostGroup hostGroup, String blueprint) {
        Map<String, Map<String, String>> result = new HashMap<>();
        String serviceName = getServiceName(name);
        if (serviceName != null) {
            ServiceConfig serviceConfig = serviceConfigs.get(serviceName);
            Set<String> hostComponents = blueprintProcessor.getComponentsInHostGroup(blueprint, hostGroup.getName());
            List<String> relatedServices = serviceConfig.getRelatedServices();
            if (hostComponents.stream().anyMatch(relatedServices::contains)) {
                Map<String, List<ConfigProperty>> config = global ? serviceConfig.getGlobalConfig() : serviceConfig.getHostGroupConfig();
                for (Entry<String, List<ConfigProperty>> entry : config.entrySet()) {
                    Map<String, String> properties = new HashMap<>();
                    for (ConfigProperty property : entry.getValue()) {
                        String directory = serviceName.toLowerCase() + (property.getDirectory().isEmpty() ? "" : '/' + property.getDirectory());
                        String value = getValue(global, volumeCount, property, directory);
                        if (value != null) {
                            properties.put(property.getName(), value);
                        }
                    }
                    if (!properties.isEmpty()) {
                        result.put(entry.getKey(), properties);
                    }
                }
            }
        }
        return result;
    }

    private String getValue(boolean global, Integer volumeCount, ConfigProperty property, String directory) {
        String value = null;
        if (volumeCount == null && global) {
            value = property.getPrefix() + getLogVolume(directory);
        } else if (volumeCount != null && volumeCount > 0) {
            value = global ? property.getPrefix() + getLogVolume(directory) : buildVolumePathString(volumeCount, directory);
        } else if (byosDataDirIsSet()) {
            value = global ? property.getPrefix() + byosDfsDataDirs.get(0) + '/' + directory : buildVolumePathString(byosDfsDataDirs, directory);
        }
        return value;
    }

    private boolean byosDataDirIsSet() {
        return byosDfsDataDirs != null && !byosDfsDataDirs.isEmpty() && byosDfsDataDirs.get(0) != null && !byosDfsDataDirs.get(0).isEmpty();
    }

    private String getServiceName(String componentName) {
        for (String serviceName : serviceConfigs.keySet()) {
            if (componentName.toLowerCase().startsWith(serviceName.toLowerCase())) {
                return serviceName;
            }
        }
        return null;
    }

}
