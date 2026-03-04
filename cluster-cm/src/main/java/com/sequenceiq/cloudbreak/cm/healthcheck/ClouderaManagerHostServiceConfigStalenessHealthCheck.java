package com.sequenceiq.cloudbreak.cm.healthcheck;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ClouderaManagerHostServiceConfigStalenessHealthCheck implements ClouderaManagerHostHealthCheck {

    private static final String DATA_CONTEXT_CONNECTOR = "DATA_CONTEXT_CONNECTOR";

    @Override
    public HealthCheckType getHealthCheckType() {
        return HealthCheckType.SERVICE_CONFIG_STALENESS;
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost apiHost, List<ApiService> apiServices) {
        List<String> staleServiceNames = apiServices.stream()
                .filter(apiService -> !DATA_CONTEXT_CONNECTOR.equals(apiService.getType()))
                .filter(this::isServiceStale)
                .map(ApiService::getDisplayName)
                .toList();
        if (staleServiceNames.isEmpty()) {
            return Optional.of(HealthCheck.healthy(getHealthCheckType()));
        } else {
            String reason = "The following services are running with stale configurations, please restart them to apply the pending updates";
            return Optional.of(HealthCheck.unhealthy(getHealthCheckType(), reason, staleServiceNames));
        }
    }

    private boolean isServiceStale(ApiService apiService) {
        return !apiService.getConfigStalenessStatus().equals(ApiConfigStalenessStatus.FRESH)
                || !apiService.getClientConfigStalenessStatus().equals(ApiConfigStalenessStatus.FRESH);
    }
}
