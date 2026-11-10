package com.sequenceiq.cloudbreak.cm.util;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.stream.Stream;

import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHost;

public final class ClouderaManagerHostHealthUtil {

    public static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    private ClouderaManagerHostHealthUtil() {
    }

    public static Stream<ApiHealthCheck> getHostScmHealthChecks(ApiHost host) {
        return emptyIfNull(host.getHealthChecks()).stream()
                .filter(healthCheck -> HOST_SCM_HEALTH.equals(healthCheck.getName()));
    }
}
