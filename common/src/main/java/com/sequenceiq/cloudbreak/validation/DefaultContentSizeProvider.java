package com.sequenceiq.cloudbreak.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DefaultContentSizeProvider implements ContentSizeProvider {

    private static final String ENV_MAX_SIZE_IN_BYTES = "cb.validation.max.content.size";

    private static final Integer DEFAULT_MAX_SIZE_IN_BYTES = 6 * 1024 * 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContentSizeProvider.class);

    private int maxContentSizeInBytes;

    public DefaultContentSizeProvider(Environment environment) {
        this.maxContentSizeInBytes = initMaxContentSizeInBytes(environment);
    }

    private int initMaxContentSizeInBytes(Environment environment) {
        if (!environment.containsProperty(ENV_MAX_SIZE_IN_BYTES)) {
            LOGGER.debug("{} property is not set. Defaulting its value to 6 MB.", ENV_MAX_SIZE_IN_BYTES);
        }
        return Integer.valueOf(environment.getProperty(ENV_MAX_SIZE_IN_BYTES, DEFAULT_MAX_SIZE_IN_BYTES.toString()));
    }

    @Override
    public int getMaxSizeInBytes() {
        return maxContentSizeInBytes;
    }
}
