package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class PreFlightCheckValidationService {

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private EnvironmentClientService environmentClientService;

    public boolean preFlightCheckSupported(Long stackId, String environmentCrm) {
        return anyDatabusFeatureEnabled(stackId) || !proxyConfigUsed(environmentCrm);
    }

    private boolean proxyConfigUsed(String environmentCrn) {
        return environmentClientService.getByCrn(environmentCrn).getProxyConfig() != null;
    }

    private boolean anyDatabusFeatureEnabled(Long stackId) {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        return telemetry.isAnyDataBusBasedFeatureEnabled();
    }
}
