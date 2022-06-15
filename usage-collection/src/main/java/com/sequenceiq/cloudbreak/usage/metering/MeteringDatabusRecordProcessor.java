package com.sequenceiq.cloudbreak.usage.metering;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.processor.AbstractDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConsumptionConfiguration;

import io.opentracing.Tracer;

@Component
public class MeteringDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MeteringConsumptionConfiguration> {

    public MeteringDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, MeteringConsumptionConfiguration configuration,
            @Value("${telemetry.usage.messagebroker.workers:1}") int numberOfWorkers,
            @Value("${telemetry.usage.messagebroker.queueSizeLimit:2000}") int queueSizeLimit, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory) {
        super(sigmaDatabusConfig, configuration, numberOfWorkers, queueSizeLimit, tracer, internalCrnGeneratorFactory);
    }
}
