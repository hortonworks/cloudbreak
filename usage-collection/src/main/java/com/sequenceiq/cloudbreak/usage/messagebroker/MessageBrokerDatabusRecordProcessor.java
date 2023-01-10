package com.sequenceiq.cloudbreak.usage.messagebroker;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.processor.AbstractDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.telemetry.messagebroker.MessageBrokerConfiguration;

@Component
public class MessageBrokerDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MessageBrokerConfiguration> {

    private static final String HEADER_USAGE_EVENTS_ORIGIN = "usage-events-origin";

    private static final String HEADER_USAGE_EVENTS_PROCESSOR = "usage-events-processor";

    private final Map<String, String> optionalUsageHeaders;

    public MessageBrokerDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, MessageBrokerConfiguration databusStreamConfiguration,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(sigmaDatabusConfig, databusStreamConfiguration, regionAwareInternalCrnGeneratorFactory);
        optionalUsageHeaders = ImmutableMap.of(HEADER_USAGE_EVENTS_ORIGIN, databusStreamConfiguration.getOrigin(),
                HEADER_USAGE_EVENTS_PROCESSOR, databusStreamConfiguration.getProcessor());
    }

    public Map<String, String> getOptionalUsageHeaders() {
        return optionalUsageHeaders;
    }
}
