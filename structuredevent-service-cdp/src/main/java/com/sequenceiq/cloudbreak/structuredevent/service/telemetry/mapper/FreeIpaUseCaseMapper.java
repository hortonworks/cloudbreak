package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;

@Component
public class FreeIpaUseCaseMapper extends AbstractUseCaseMapper<CDPFreeIPAStatus.Value, FreeIpaUseCaseAware> {

    @Override
    protected Value unset() {
        return UNSET;
    }

    @Override
    protected Class<FreeIpaUseCaseAware> useCaseAwareClass() {
        return FreeIpaUseCaseAware.class;
    }
}
