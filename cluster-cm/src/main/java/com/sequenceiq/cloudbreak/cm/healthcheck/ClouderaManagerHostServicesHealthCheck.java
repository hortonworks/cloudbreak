package com.sequenceiq.cloudbreak.cm.healthcheck;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class ClouderaManagerHostServicesHealthCheck implements ClouderaManagerHostHealthCheck {

    @Override
    public HealthCheckType getHealthCheckType() {
        return HealthCheckType.SERVICES;
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost host, List<ApiService> apiServices) {
        if (CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion)) {
            Set<String> servicesWithBadHealth = collectServicesWithBadHealthOnHost(host);
            if (servicesWithBadHealth.isEmpty()) {
                return Optional.of(HealthCheck.healthy(getHealthCheckType()));
            } else {
                String statusReason = String.format("The following services are in bad health: %s.", Joiner.on(", ").join(servicesWithBadHealth));
                return Optional.of(HealthCheck.unhealthy(getHealthCheckType(), statusReason));
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
}
