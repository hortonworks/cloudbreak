package com.sequenceiq.cloudbreak.cm.healthcheck;

import java.util.List;
import java.util.Optional;

import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public interface ClouderaManagerHostHealthCheck {

    HealthCheckType getHealthCheckType();

    Optional<HealthCheck> getHealthCheck(Optional<String> runtimeVersion, ApiHost host, List<ApiService> apiServices);
}
