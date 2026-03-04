package com.sequenceiq.cloudbreak.cm.healthcheck;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ClouderaManagerHostCertHealthCheck implements ClouderaManagerHostHealthCheck {

    public static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final Set<ApiHealthSummary> UNHEALTHY_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.CONCERNING,
            ApiHealthSummary.BAD
    );

    @Override
    public HealthCheckType getHealthCheckType() {
        return HealthCheckType.CERTIFICATE;
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost apiHost, List<ApiService> apiServices) {
        Optional<ApiHealthCheck> healthCheck = emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_AGENT_CERTIFICATE_EXPIRY.equals(health.getName()))
                .findFirst();
        if (healthCheck.isPresent()) {
            HealthCheckResult result = UNHEALTHY_SUMMARIES.contains(healthCheck.get().getSummary())
                    ? HealthCheckResult.UNHEALTHY
                    : HealthCheckResult.HEALTHY;
            Optional<String> reason = Optional.ofNullable(healthCheck.get().getSummary())
                    .map(apiSum -> "Certificate health in Cloudera Manager is " + apiSum.getValue());
            List<String> details = result == HealthCheckResult.UNHEALTHY ? List.of(healthCheck.get().getExplanation()) : List.of();
            return Optional.of(new HealthCheck(getHealthCheckType(), result, reason, details));
        }
        return Optional.empty();
    }
}
