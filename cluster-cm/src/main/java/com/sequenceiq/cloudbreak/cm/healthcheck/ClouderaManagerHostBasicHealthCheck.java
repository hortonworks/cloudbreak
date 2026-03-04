package com.sequenceiq.cloudbreak.cm.healthcheck;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ClouderaManagerHostBasicHealthCheck implements ClouderaManagerHostHealthCheck {

    private static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    private static final String DEFAULT_STATUS_REASON = "Cloudera Manager reported bad health for this host.";

    private static final String MAINTENANCE_MODE = "This host is in maintenance mode.";

    private static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.DISABLED,
            ApiHealthSummary.NOT_AVAILABLE,
            ApiHealthSummary.HISTORY_NOT_AVAILABLE
    );

    @Override
    public HealthCheckType getHealthCheckType() {
        return HealthCheckType.HOST;
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost apiHost, List<ApiService> apiServices) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_SCM_HEALTH.equals(health.getName()))
                .filter(health -> !IGNORED_HEALTH_SUMMARIES.contains(health.getSummary()))
                .findFirst()
                .map(apiHealthCheck -> new HealthCheck(
                        getHealthCheckType(),
                        healthSummaryToHealthCheckResult(apiHealthCheck.getSummary(), apiHost.isMaintenanceMode()),
                        getHostHealthMessage(apiHealthCheck.getSummary(), apiHealthCheck.getExplanation(), apiHost.isMaintenanceMode()),
                        List.of()));
    }

    private static Optional<String> getHostHealthMessage(ApiHealthSummary healthSummary, String explanation, Boolean maintenanceMode) {
        if (Boolean.TRUE.equals(maintenanceMode)) {
            return Optional.of(MAINTENANCE_MODE);
        }
        if (healthSummaryToHealthCheckResult(healthSummary, false) == HealthCheckResult.UNHEALTHY) {
            if (StringUtils.isNotBlank(explanation)) {
                return Optional.of(explanation.endsWith(".") ? explanation : explanation + ".");
            } else {
                return Optional.of(DEFAULT_STATUS_REASON);
            }
        }
        return Optional.empty();
    }

    private static HealthCheckResult healthSummaryToHealthCheckResult(ApiHealthSummary healthSummary, Boolean maintenanceMode) {
        if (Boolean.TRUE.equals(maintenanceMode)) {
            return HealthCheckResult.UNHEALTHY;
        }
        switch (healthSummary) {
            case GOOD:
            case CONCERNING:
                return HealthCheckResult.HEALTHY;
            default:
                return HealthCheckResult.UNHEALTHY;
        }
    }
}
