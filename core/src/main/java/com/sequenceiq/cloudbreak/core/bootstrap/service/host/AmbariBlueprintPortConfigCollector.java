package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Component
@ConfigurationProperties
public class AmbariBlueprintPortConfigCollector {

    // injected by Spring using the setter, but setting a default value here in case config were missing
    private List<PortConfig> blueprintServicePorts = new ArrayList<>();

    public Map<String, Integer> getServicePorts(ClusterDefinition clusterDefinition) {
        Map<String, Integer> collectedPorts = new HashMap<>();
        collectConfiguredPorts(clusterDefinition, collectedPorts);
        addDefaultPorts(collectedPorts);
        return collectedPorts;
    }

    private void collectConfiguredPorts(ClusterDefinition clusterDefinition, Map<String, Integer> collectedPorts) {
        String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
        Map<String, Map<String, String>> configurations = new AmbariBlueprintTextProcessor(clusterDefinitionText).getConfigurationEntries();
        blueprintServicePorts.forEach(portConfig -> {
            Optional<Integer> configuredPort = getConfiguredPortForService(portConfig, configurations);
            ExposedService exposedService = ExposedService.valueOf(portConfig.getService());
            configuredPort.ifPresent(integer -> collectedPorts.put(exposedService.getKnoxService(), integer));
        });
    }

    private Optional<Integer> getConfiguredPortForService(PortConfig portConfig, Map<String, Map<String, String>> configurations) {
        if (configurations.containsKey(portConfig.getConfigName())) {
            Map<String, String> configuration = configurations.get(portConfig.getConfigName());
            if (portConfig.isPortKeySet()) {
                return Optional.ofNullable(configuration.get(portConfig.getPortKey()))
                        .map(portValue -> getPortFromPortString(portConfig.getService(), portValue));
            } else {
                return Optional.ofNullable(configuration.get(portConfig.getHostKey()))
                        .map(hostValue -> getPortFromHost(portConfig.getService(), hostValue));
            }
        }
        return Optional.empty();
    }

    private Integer getPortFromPortString(String service, String portValue) {
        try {
            return Integer.valueOf(portValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("In the blueprint '%s' service has invalid port config. "
                    + "Port value is: '%s'.", service, portValue), e);
        }
    }

    private Integer getPortFromHost(String service, String host) {
        try {
            String portString = StringUtils.substringAfterLast(host, ":");
            return Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("In the blueprint '%s' service has invalid host config. "
                    + "Host value is: '%s'.", service, host), e);
        }
    }

    private void addDefaultPorts(Map<String, Integer> collectedPorts) {
        Arrays.stream(ExposedService.values()).forEach(exposedService -> {
            if (StringUtils.isNotEmpty(exposedService.getKnoxService())) {
                if (!collectedPorts.containsKey(exposedService.getKnoxService())) {
                    collectedPorts.put(exposedService.getKnoxService(), exposedService.getDefaultPort());
                }
            }
        });
    }

    public void setBlueprintServicePorts(List<PortConfig> blueprintServicePorts) {
        this.blueprintServicePorts = blueprintServicePorts;
    }

    public List<PortConfig> getBlueprintServicePorts() {
        return blueprintServicePorts;
    }

    public static class PortConfig {

        private String service;

        private String configName;

        private String portKey;

        private String hostKey;

        public PortConfig() {
        }

        public PortConfig(String service, String configName, String portKey, String hostKey) {
            this.service = service;
            this.configName = configName;
            this.portKey = portKey;
            this.hostKey = hostKey;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getConfigName() {
            return configName;
        }

        public void setConfigName(String configName) {
            this.configName = configName;
        }

        public String getPortKey() {
            return portKey;
        }

        public void setPortKey(String portKey) {
            this.portKey = portKey;
        }

        public String getHostKey() {
            return hostKey;
        }

        public void setHostKey(String hostKey) {
            this.hostKey = hostKey;
        }

        public boolean isPortKeySet() {
            return StringUtils.isNotEmpty(portKey);
        }
    }
}
