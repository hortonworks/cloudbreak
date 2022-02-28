package com.sequenceiq.cloudbreak.usage.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;

/**
 * A usage reporter strategy class that logs usage events.
 */
@Service
public class LoggingUsageProcessingStrategy implements UsageProcessingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUsageProcessingStrategy.class);

    private static final Logger BINARY_EVENT_LOGGER = LoggerFactory.getLogger("CDP_BINARY_USAGE_EVENT");

    @Override
    public void processUsage(UsageProto.Event event) {
        LOGGER.info("Logging binary format for the following usage event: {}", event);
        String binaryUsageEvent = BaseEncoding.base64().encode(event.toByteArray());
        BINARY_EVENT_LOGGER.info(binaryUsageEvent);
    }
}
