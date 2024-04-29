package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;

@Component
public class EnvironmentUseCaseMapper extends AbstractUseCaseMapper<Value, EnvironmentUseCaseAware> {

    @Override
    protected Value unset() {
        return UNSET;
    }

    @Override
    protected Class<EnvironmentUseCaseAware> useCaseAwareClass() {
        return EnvironmentUseCaseAware.class;
    }
}