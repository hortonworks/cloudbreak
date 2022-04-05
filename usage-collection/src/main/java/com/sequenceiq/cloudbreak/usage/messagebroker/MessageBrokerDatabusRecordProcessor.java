package com.sequenceiq.cloudbreak.usage.messagebroker;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.processor.AbstractDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.telemetry.messagebroker.MessageBrokerConfiguration;

import io.opentracing.Tracer;

@Component
public class MessageBrokerDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MessageBrokerConfiguration> {

    private static final String HEADER_USAGE_EVENTS_ORIGIN = "usage-events-origin";

    private static final String HEADER_USAGE_EVENTS_PROCESSOR = "usage-events-processor";

    private final Map<String, String> optionalUsageHeaders;

    public MessageBrokerDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, MessageBrokerConfiguration databusStreamConfiguration,
        @Value("${telemetry.usage.messagebroker.workers:1}") int numberOfWorkers,
        @Value("${telemetry.usage.messagebroker.queueSizeLimit:2000}") int queueSizeLimit, Tracer tracer,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(sigmaDatabusConfig, databusStreamConfiguration, numberOfWorkers, queueSizeLimit, tracer, regionAwareInternalCrnGeneratorFactory);
        optionalUsageHeaders = ImmutableMap.of(HEADER_USAGE_EVENTS_ORIGIN, databusStreamConfiguration.getOrigin(),
                HEADER_USAGE_EVENTS_PROCESSOR, databusStreamConfiguration.getProcessor());
    }

    public Map<String, String> getOptionalUsageHeaders() {
        return optionalUsageHeaders;
    }
}
