package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.buildVolumePathString;
import static com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils.getFirstVolume;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class HadoopConfigurationService {

    @Inject
    private HostGroupRepository hostGroupRepository;
    private Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws IOException {
        String serviceConfigJson = FileReaderUtils.readFileFromClasspath("hdp/service-config.json");
        JsonNode services = objectMapper.readTree(serviceConfigJson).get("services");
        for (JsonNode service : services) {
            String serviceName = service.get("name").asText();
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
            serviceConfigs.put(serviceName, new ServiceConfig(serviceName, globalConfig, hostConfig));
        }
    }

    public Map<String, Map<String, String>> getGlobalConfiguration(Cluster cluster) throws IOException {
        Map<String, Map<String, String>> config = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode blueprintNode = mapper.readTree(cluster.getBlueprint().getBlueprintText());
        JsonNode hostGroups = blueprintNode.path("host_groups");
        for (JsonNode hostGroup : hostGroups) {
            JsonNode components = hostGroup.path("components");
            for (JsonNode component : components) {
                String name = component.path("name").asText();
                config.putAll(getProperties(name, true, null));
            }
        }
        return config;
    }

    public Map<String, Map<String, Map<String, String>>> getHostGroupConfiguration(Cluster cluster) {
        Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
        Map<String, Map<String, Map<String, String>>> hadoopConfig = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            int volumeCount = hostGroup.getInstanceGroup().getTemplate().getVolumeCount();
            Map<String, Map<String, String>> componentConfig = new HashMap<>();
            for (String serviceName : serviceConfigs.keySet()) {
                componentConfig.putAll(getProperties(serviceName, false, volumeCount));
            }
            hadoopConfig.put(hostGroup.getName(), componentConfig);
        }
        return hadoopConfig;
    }

    private List<ConfigProperty> toList(JsonNode nodes) {
        List<ConfigProperty> list = new ArrayList<>();
        for (JsonNode node : nodes) {
            list.add(new ConfigProperty(node.get("name").asText(), node.get("directory").asText(), node.get("prefix").asText()));
        }
        return list;
    }

    private Map<String, Map<String, String>> getProperties(String name, boolean global, Integer volumeCount) {
        Map<String, Map<String, String>> result = new HashMap<>();
        String serviceName = getServiceName(name);
        if (serviceName != null) {
            ServiceConfig serviceConfig = serviceConfigs.get(serviceName);
            Map<String, List<ConfigProperty>> config = global ? serviceConfig.getGlobalConfig() : serviceConfig.getHostGroupConfig();
            for (String siteConfig : config.keySet()) {
                Map<String, String> properties = new HashMap<>();
                for (ConfigProperty property : config.get(siteConfig)) {
                    String directory = serviceName.toLowerCase() + (property.getDirectory().isEmpty() ? "" : "/" + property.getDirectory());
                    String value = global ? property.getPrefix() + getFirstVolume(directory) : buildVolumePathString(volumeCount, directory);
                    properties.put(property.getName(), value);
                }
                result.put(siteConfig, properties);
            }
        }
        return result;
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
