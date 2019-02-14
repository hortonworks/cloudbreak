package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import static org.apache.commons.lang3.tuple.ImmutablePair.of;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.clusterdefinition.ConfigProperty;
import com.sequenceiq.cloudbreak.clusterdefinition.ServiceConfig;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class ConfigUtils {

    @Inject
    private HadoopConfigurationUtils hadoopConfigurationUtils;

    public JsonNode readConfigJson(String jsonPath, String configName) throws IOException {
        String serviceConfigJson = FileReaderUtils.readFileFromClasspath(jsonPath);
        return JsonUtil.readTree(serviceConfigJson).get(configName);
    }

    public boolean isConfigUpdateNeeded(HostgroupView hostGroup) {
        return hostGroup.isInstanceGroupConfigured();
    }

    public Map<String, Map<String, String>> getProperties(ServiceConfig serviceConfig, boolean global, int volumeCount, Collection<String> hostComponents) {
        Map<String, Map<String, String>> result = new HashMap<>();

        getConfigProperties(serviceConfig, hostComponents, global).forEach((key, configProperties) -> {
            Map<String, String> collected = configProperties.stream()
                    .map(configProperty -> of(configProperty.getName(), hadoopConfigurationUtils.getValue(configProperty,
                            serviceConfig.getServiceName(), global, volumeCount)))
                    .filter(pair -> Objects.nonNull(pair.right))
                    .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));

            if (!collected.isEmpty()) {
                result.put(key, collected);
            }
        });

        return result;
    }

    private Map<String, List<ConfigProperty>> getConfigProperties(ServiceConfig serviceConfig, Collection<String> hostComponents, boolean global) {
        if (hostComponents.stream().anyMatch(serviceConfig.getRelatedServices()::contains)) {
            return global ? serviceConfig.getGlobalConfig() : serviceConfig.getHostGroupConfig();
        } else {
            return Collections.emptyMap();
        }
    }

    public ServiceConfig getServiceConfig(String serviceName, Map<String, ServiceConfig> serviceConfigs) {
        Optional<Entry<String, ServiceConfig>> serviceConfigEntry = serviceConfigs.entrySet().stream()
                .filter(entry -> serviceName.toLowerCase().startsWith(entry.getKey().toLowerCase()))
                .findFirst();

        return serviceConfigEntry.isPresent() ? serviceConfigEntry.get().getValue() : null;
    }

}
