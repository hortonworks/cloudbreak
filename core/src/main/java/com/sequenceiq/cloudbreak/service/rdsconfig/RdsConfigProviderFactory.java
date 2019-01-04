package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class RdsConfigProviderFactory {

    @Inject
    private Set<AbstractRdsConfigProvider> rdsConfigProviders;

    public Set<AbstractRdsConfigProvider> getAllSupportedRdsConfigProviders() {
        return rdsConfigProviders;
    }
}
