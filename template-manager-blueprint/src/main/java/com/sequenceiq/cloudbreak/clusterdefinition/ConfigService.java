package com.sequenceiq.cloudbreak.clusterdefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.ConfigUtils;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.HadoopConfigurationUtils;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Service
public class ConfigService {

    @Inject
    private ConfigUtils configUtils;

    @Inject
    private HadoopConfigurationUtils hadoopConfigurationUtils;

    private Map<String, ServiceConfig> serviceConfigs;

    private Map<String, Map<String, String>> bpConfigs;

    @PostConstruct
    public void init() throws IOException {
        serviceConfigs = collectServiceConfigsFromJson();
        bpConfigs = collectBlueprintConfigsFromJson();
    }

    protected Map<String, Map<String, String>> collectBlueprintConfigsFromJson() throws IOException {
        JsonNode blueprints = configUtils.readConfigJson("hdp/bp-config.json", "sites");

        Map<String, Map<String, String>> bpConfigs = new HashMap<>();
        for (JsonNode bp : blueprints) {
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

        return bpConfigs;
    }

    protected Map<String, ServiceConfig> collectServiceConfigsFromJson() throws IOException {
        JsonNode services = configUtils.readConfigJson("hdp/service-config.json", "services");

        Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
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
        return serviceConfigs;
    }

    public Map<String, Map<String, Map<String, String>>> getHostGroupConfiguration(AmbariBlueprintTextProcessor blueprintProcessor,
            Collection<HostgroupView> hostGroups) {
        Map<String, Map<String, Map<String, String>>> hadoopConfig = new HashMap<>();
        hostGroups.stream()
                .filter(configUtils::isConfigUpdateNeeded)
                .forEach(hostGroup -> hadoopConfig.put(hostGroup.getName(), getHadoopConfigs(blueprintProcessor, hostGroup)));
        return hadoopConfig;
    }

    public Map<String, Map<String, String>> getComponentsByHostGroup(AmbariBlueprintTextProcessor blueprintProcessor, Collection<HostgroupView> hostGroups) {
        Map<String, Map<String, String>> config = new HashMap<>();

        Map<String, Set<String>> componentsByHostGroup = blueprintProcessor.getComponentsByHostGroup();
        componentsByHostGroup.forEach((hostGoupName, value) -> {
            HostgroupView hostGroup = hadoopConfigurationUtils.findHostGroupForNode(hostGroups, hostGoupName);
            int volumeCount = configUtils.isConfigUpdateNeeded(hostGroup) ? 0 : -1;
            value.stream()
                    .map(componentName -> configUtils.getServiceConfig(componentName, serviceConfigs))
                    .filter(Objects::nonNull)
                    .forEach(serviceConfig -> {
                        Set<String> hostComponents = blueprintProcessor.getComponentsInHostGroup(hostGoupName);
                        config.putAll(configUtils.getProperties(serviceConfig, true, volumeCount, hostComponents));
                    });
        });

        return config;
    }

    public void collectBlueprintConfigIfNeed(Map<String, Map<String, String>> config) {
        bpConfigs.forEach((key, value) -> {
            if (config.containsKey(key)) {
                value.forEach((containsKey, containsValue) -> config.get(key).put(containsKey, containsValue));
            } else {
                config.put(key, value);
            }
        });
    }

    private Map<String, Map<String, String>> getHadoopConfigs(AmbariBlueprintTextProcessor blueprintProcessor, HostgroupView hostGroup) {
        int volumeCount = Objects.isNull(hostGroup.getVolumeCount()) ? -1 : hostGroup.getVolumeCount();

        Map<String, Map<String, String>> hadoopConfig = new HashMap<>();
        for (ServiceConfig serviceConfig : serviceConfigs.values()) {
            Set<String> hostComponents = blueprintProcessor.getComponentsInHostGroup(hostGroup.getName());
            hadoopConfig.putAll(configUtils.getProperties(serviceConfig, false, volumeCount, hostComponents));
        }
        return hadoopConfig;
    }

    private List<ConfigProperty> toList(Iterable<JsonNode> nodes) {
        List<ConfigProperty> list = new ArrayList<>();
        for (JsonNode node : nodes) {
            list.add(new ConfigProperty(node.get("name").asText(), node.get("directory").asText(), node.get("prefix").asText()));
        }
        return list;
    }

}
