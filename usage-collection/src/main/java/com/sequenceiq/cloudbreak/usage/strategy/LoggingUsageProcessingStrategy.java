package com.sequenceiq.cloudbreak.usage.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

/**
 * A usage reporter strategy class that logs usage events.
 */
@Service
public class LoggingUsageProcessingStrategy implements UsageProcessingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUsageProcessingStrategy.class);

    private static final Logger BINARY_EVENT_LOGGER = LoggerFactory.getLogger("CDP_BINARY_USAGE_EVENT");

    private static final String USAGE_EVENT_MDC_NAME = "binaryUsageEvent";

    @Override
    public void processUsage(UsageProto.Event event) {
        LOGGER.info("Logging binary format for the following usage event: {}", event);
        String binaryUsageEvent = BaseEncoding.base64().encode(event.toByteArray());
        MDCBuilder.addMdcField(USAGE_EVENT_MDC_NAME, binaryUsageEvent);
        BINARY_EVENT_LOGGER.info(binaryUsageEvent);
        MDCBuilder.removeMdcField(USAGE_EVENT_MDC_NAME);
    }
}
