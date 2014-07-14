package com.sequenceiq.periscope.service.configuration;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;

public class AmbariConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationService.class);
    private static final List<String> CONFIG_LIST = new ArrayList<>(ConfigParam.values().length);
    private static final String RETRY_COUNT = "1";

    static {
        for (ConfigParam param : ConfigParam.values()) {
            CONFIG_LIST.add(param.key());
        }
    }

    private AmbariConfigurationService() {
    }

    public static Configuration getConfiguration(AmbariClient ambariClient) throws ConnectException {
        Configuration configuration = new Configuration(false);
        Set<Map.Entry<String, Map<String, String>>> serviceConfigs = ambariClient.getServiceConfigMap().entrySet();
        for (Map.Entry<String, Map<String, String>> serviceEntry : serviceConfigs) {
            LOGGER.debug("Processing service: {}", serviceEntry.getKey());
            for (Map.Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    configuration.set(configEntry.getKey(), configEntry.getValue());
                    LOGGER.debug("Adding entry: {}", configEntry);
                }
            }
        }
        if (serviceConfigs.isEmpty()) {
            throw new ConnectException(ambariClient.getAmbari().getUri().toString());
        }
        decorateConfiguration(configuration);
        return configuration;
    }

    private static void decorateConfiguration(Configuration configuration) {
        configuration.set(ConfigParam.RM_CONN_MAX_WAIT_MS.key(), RETRY_COUNT);
        configuration.set(ConfigParam.RM_CONN_RETRY_INTERVAL_MS.key(), RETRY_COUNT);
    }
}
