package com.sequenceiq.cloudbreak.service.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.services.ServiceAndHostService;

@Service
public class AmbariConfigurationService {

    private static final Collection<String> CONFIG_LIST = new ArrayList<>(ConfigParam.values().length);

    private static final String AZURE_ADDRESS_SUFFIX = "cloudapp.net";

    static {
        for (ConfigParam param : ConfigParam.values()) {
            CONFIG_LIST.add(param.key());
        }
    }

    public Map<String, String> getConfiguration(ServiceAndHostService ambariClient, String hostGroup) {
        Map<String, String> configuration = new HashMap<>();
        Set<Entry<String, Map<String, String>>> serviceConfigs = ambariClient.getServiceConfigMapByHostGroup(hostGroup).entrySet();
        for (Entry<String, Map<String, String>> serviceEntry : serviceConfigs) {
            for (Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    configuration.put(configEntry.getKey(), replaceHostName(ambariClient, configEntry));
                }
            }
        }
        return configuration;
    }

    private String replaceHostName(ServiceAndHostService ambariClient, Entry<String, String> entry) {
        String result = entry.getValue();
        if (entry.getKey().startsWith("yarn.resourcemanager")) {
            int portStartIndex = result.indexOf(':');
            String internalAddress = result.substring(0, portStartIndex);
            String publicAddress = ambariClient.resolveInternalHostName(internalAddress);
            if (internalAddress.equals(publicAddress)) {
                if (internalAddress.contains(AZURE_ADDRESS_SUFFIX)) {
                    publicAddress = internalAddress.substring(0, internalAddress.indexOf('.') + 1) + AZURE_ADDRESS_SUFFIX;
                }
            }
            result = publicAddress + result.substring(portStartIndex);
        }
        return result;
    }

}
