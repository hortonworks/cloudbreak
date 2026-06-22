package com.sequenceiq.cloudbreak.cm.healthcheck;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ClouderaManagerHostServicesHealthCheck implements ClouderaManagerHostHealthCheck {

    private static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    @Override
    public HealthCheckType getHealthCheckType() {
        return HealthCheckType.SERVICES;
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost host, List<ApiService> apiServices) {
        if (CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion)) {
            Set<String> servicesWithBadHealth = collectServicesWithBadHealthOnHost(host);
            Set<String> stoppedServices = collectStoppedServicesOnHost(host);
            List<String> reasons = new ArrayList<>();
            if (!servicesWithBadHealth.isEmpty()) {
                reasons.add(String.format("The following services are in bad health: %s.", Joiner.on(", ").join(servicesWithBadHealth)));
            }
            if (!stoppedServices.isEmpty()) {
                reasons.add(String.format("The following services are stopped: %s.", Joiner.on(", ").join(stoppedServices)));
            }
            if (reasons.isEmpty()) {
                return Optional.of(HealthCheck.healthy(getHealthCheckType()));
            } else {
                return Optional.of(HealthCheck.unhealthy(getHealthCheckType(), String.join(" ", reasons)));
            }
        }
        return Optional.empty();
    }

    private Set<String> collectServicesWithBadHealthOnHost(ApiHost host) {
        return emptyIfNull(host.getRoleRefs()).stream()
                .filter(roleRef -> ApiHealthSummary.BAD.equals(roleRef.getHealthSummary()))
                .map(ApiRoleRef::getServiceName)
                .collect(Collectors.toSet());
    }

    private Set<String> collectStoppedServicesOnHost(ApiHost host) {
        if (!isHostAlive(host) || Boolean.TRUE.equals(host.isMaintenanceMode())) {
            return Set.of();
        }
        return emptyIfNull(host.getRoleRefs()).stream()
                .filter(roleRef -> roleRef.getRoleStatus() != null)
                .filter(roleRef -> roleRef.getRoleStatus() == ApiRoleState.STOPPED
                        || roleRef.getRoleStatus() == ApiRoleState.STOPPING)
                .map(ApiRoleRef::getServiceName)
                .collect(Collectors.toSet());
    }

    private boolean isHostAlive(ApiHost host) {
        return emptyIfNull(host.getHealthChecks()).stream()
                .filter(hc -> HOST_SCM_HEALTH.equals(hc.getName()))
                .map(ApiHealthCheck::getSummary)
                .findFirst()
                .map(summary -> summary == ApiHealthSummary.GOOD || summary == ApiHealthSummary.CONCERNING)
                .orElse(false);
    }
}
