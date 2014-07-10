package com.sequenceiq.periscope.service.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;

public class AmbariConfigurationService implements ConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationService.class);
    private static final String RETRY_COUNT = "1";
    private static final List<String> CONFIG_LIST = Arrays.asList(
            ConfigParam.MR_FRAMEWORK_NAME,
            ConfigParam.YARN_RM_ADDRESS,
            ConfigParam.YARN_RESOURCEMANAGER_SCHEDULER_ADDRESS,
            ConfigParam.YARN_SCHEDULER_ADDRESS,
            ConfigParam.FS_DEFAULT_NAME
    );
    private final AmbariClient ambariClient;

    public AmbariConfigurationService(AmbariClient ambariClient) {
        this.ambariClient = ambariClient;
    }

    @Override
    public Configuration getConfiguration() {
        Configuration configuration = new Configuration(false);
        for (Map.Entry<String, Map<String, String>> serviceEntry : ambariClient.getServiceConfigMap().entrySet()) {
            LOGGER.debug("Processing service: {}", serviceEntry.getKey());
            for (Map.Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    configuration.set(configEntry.getKey(), configEntry.getValue());
                    LOGGER.debug("Adding entry: {}", configEntry);
                }
            }
        }
        decorateConfiguration(configuration);
        return configuration;
    }

    private void decorateConfiguration(Configuration configuration) {
        configuration.set(ConfigParam.RM_CONN_MAX_WAIT_MS, RETRY_COUNT);
        configuration.set(ConfigParam.RM_CONN_RETRY_INTERVAL_MS, RETRY_COUNT);
    }
}
