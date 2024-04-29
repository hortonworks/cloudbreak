package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;

@Component
public class DatalakeUseCaseMapper extends AbstractUseCaseMapper<Value, DatalakeUseCaseAware> {

    @Override
    protected Value unset() {
        return UNSET;
    }

    @Override
    protected Class<DatalakeUseCaseAware> useCaseAwareClass() {
        return DatalakeUseCaseAware.class;
    }
}
