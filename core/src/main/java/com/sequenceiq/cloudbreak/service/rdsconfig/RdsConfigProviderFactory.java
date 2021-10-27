package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

@Component
public class RdsConfigProviderFactory {

    @Inject
    private Set<AbstractRdsConfigProvider> rdsConfigProviders;

    public AbstractRdsConfigProvider getRdsConfigProviderForRdsType(DatabaseType type) {
        return rdsConfigProviders.stream().filter(rdsConfigProvider -> rdsConfigProvider.getRdsType() == type).findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(type.name() + " RdsConfigProvider is not supported!"));
    }

    public Set<AbstractRdsConfigProvider> getAllSupportedRdsConfigProviders() {
        return rdsConfigProviders;
    }
}
