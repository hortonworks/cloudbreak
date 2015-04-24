package com.sequenceiq.cloudbreak.service.cluster;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;

@Service
public class AmbariConfigurationService {

    private static final List<String> CONFIG_LIST = new ArrayList<>(ConfigParam.values().length);
    private static final String AZURE_ADDRESS_SUFFIX = "cloudapp.net";

    static {
        for (ConfigParam param : ConfigParam.values()) {
            CONFIG_LIST.add(param.key());
        }
    }

    public Map<String, String> getConfiguration(AmbariClient ambariClient, String hostGroup) throws ConnectException {
        Map<String, String> configuration = new HashMap<>();
        Set<Map.Entry<String, Map<String, String>>> serviceConfigs = ambariClient.getServiceConfigMapByHostGroup(hostGroup).entrySet();
        for (Map.Entry<String, Map<String, String>> serviceEntry : serviceConfigs) {
            for (Map.Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    configuration.put(configEntry.getKey(), replaceHostName(ambariClient, configEntry));
                }
            }
        }
        return configuration;
    }

    private String replaceHostName(AmbariClient ambariClient, Map.Entry<String, String> entry) {
        String result = entry.getValue();
        if (entry.getKey().startsWith("yarn.resourcemanager")) {
            int portStartIndex = result.indexOf(":");
            String internalAddress = result.substring(0, portStartIndex);
            String publicAddress = ambariClient.resolveInternalHostName(internalAddress);
            if (internalAddress.equals(publicAddress)) {
                if (internalAddress.contains(AZURE_ADDRESS_SUFFIX)) {
                    publicAddress = internalAddress.substring(0, internalAddress.indexOf(".") + 1) + AZURE_ADDRESS_SUFFIX;
                }
            }
            result = publicAddress + result.substring(portStartIndex);
        }
        return result;
    }

}
