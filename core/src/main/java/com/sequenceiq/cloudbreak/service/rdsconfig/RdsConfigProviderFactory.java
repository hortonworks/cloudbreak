package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;

@Component
public class RdsConfigProviderFactory {

    @Inject
    private HiveRdsConfigProvider hiveRdsConfigProvider;

    @Inject
    private RangerRdsConfigProvider rangerRdsConfigProvider;

    private final Set<RdsType> supportedRdsConfigProviders = EnumSet.of(RdsType.HIVE, RdsType.RANGER);

    private AbstractRdsConfigProvider getRdsConfigProviderForRdsType(RdsType type) {
        switch (type) {
            case HIVE:
                return hiveRdsConfigProvider;
            case RANGER:
                return rangerRdsConfigProvider;
            default:
                throw new UnsupportedOperationException(type.name() + " RdsConfigProvider is not supported!");
        }
    }

    public Set<AbstractRdsConfigProvider> getAllSupportedRdsConfigProviders() {
        return supportedRdsConfigProviders.stream().map(this::getRdsConfigProviderForRdsType).collect(Collectors.toSet());
    }
}
