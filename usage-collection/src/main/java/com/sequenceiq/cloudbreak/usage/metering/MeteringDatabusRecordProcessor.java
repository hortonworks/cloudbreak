package com.sequenceiq.cloudbreak.usage.metering;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.processor.AbstractDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConsumptionConfiguration;

@Component
public class MeteringDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MeteringConsumptionConfiguration> {

    public MeteringDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, MeteringConsumptionConfiguration configuration,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(sigmaDatabusConfig, configuration, regionAwareInternalCrnGeneratorFactory);
    }
}
