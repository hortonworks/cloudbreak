package com.sequenceiq.periscope.service.configuration;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.model.HostResolution;

public class AmbariConfigurationService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(AmbariConfigurationService.class);
    private static final List<String> CONFIG_LIST = new ArrayList<>(ConfigParam.values().length);
    private static final String AZURE_ADDRESS = "cloudapp.net";
    private static final String RETRY_COUNT = "1";

    static {
        for (ConfigParam param : ConfigParam.values()) {
            CONFIG_LIST.add(param.key());
        }
    }

    private AmbariConfigurationService() {
    }

    public static Configuration getConfiguration(long id, AmbariClient ambariClient, HostResolution resolution) throws ConnectException {
        Configuration configuration = new Configuration(false);
        Set<Map.Entry<String, Map<String, String>>> serviceConfigs = ambariClient.getServiceConfigMap().entrySet();
        for (Map.Entry<String, Map<String, String>> serviceEntry : serviceConfigs) {
            LOGGER.debug(id, "Processing service: {}", serviceEntry.getKey());
            for (Map.Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    if (resolution == HostResolution.PUBLIC) {
                        configuration.set(configEntry.getKey(), replaceHostName(ambariClient, configEntry));
                    } else {
                        configuration.set(configEntry.getKey(), configEntry.getValue());
                    }
                    LOGGER.debug(id, "Adding entry: {}", configEntry);
                }
            }
        }
        if (serviceConfigs.isEmpty()) {
            throw new ConnectException(ambariClient.getAmbari().getUri().toString());
        }
        decorateConfiguration(configuration);
        return configuration;
    }

    private static String replaceHostName(AmbariClient ambariClient, Map.Entry<String, String> entry) {
        String result = entry.getValue();
        if (entry.getKey().startsWith("yarn.resourcemanager")) {
            int portStartIndex = result.indexOf(":");
            String internalAddress = result.substring(0, portStartIndex);
            String publicAddress = ambariClient.resolveInternalHostName(internalAddress);
            if (internalAddress.equals(publicAddress)) {
                if (internalAddress.contains(AZURE_ADDRESS)) {
                    publicAddress = internalAddress.substring(0, internalAddress.indexOf(".") + 1) + AZURE_ADDRESS;
                }
            }
            result = publicAddress + result.substring(portStartIndex);
        }
        return result;
    }

    private static void decorateConfiguration(Configuration configuration) {
        configuration.set(ConfigParam.RM_CONN_MAX_WAIT_MS.key(), RETRY_COUNT);
        configuration.set(ConfigParam.RM_CONN_RETRY_INTERVAL_MS.key(), RETRY_COUNT);
    }
}
