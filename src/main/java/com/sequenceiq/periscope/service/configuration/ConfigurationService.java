package com.sequenceiq.periscope.service.configuration;

import org.apache.hadoop.conf.Configuration;

public interface ConfigurationService {

    /**
     * Retrieves the configuration from a hadoop cluster.
     *
     * @return configuration object
     */
    Configuration getConfiguration();
}
