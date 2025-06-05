package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

@Component
public class PreFlightCheckValidationService {

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private EnvironmentService environmentClientService;

    public boolean preFlightCheckSupported(Long stackId, String environmentCrm) {
        return !proxyConfigUsed(environmentCrm);
    }

    private boolean proxyConfigUsed(String environmentCrn) {
        return environmentClientService.getByCrn(environmentCrn).getProxyConfig() != null;
    }
}
