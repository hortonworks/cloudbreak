package com.sequenceiq.maintenance.dispatcher;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.maintenance.configuration.MaintenanceServiceEndpointConfig;

/**
 * Maps {@link com.sequenceiq.maintenance.domain.MaintenanceWindowTask#getSubmitterService() submitter_service}
 * values to resolved base URLs from {@link MaintenanceServiceEndpointConfig}.
 * <p>
 * Canonical names are {@code cloudbreak}, {@code datalake}, and {@code freeipa}.
 */
@Component
public class SubmitterServiceEndpointResolver {

    private final Map<String, String> submitterBaseUrls;

    @Inject
    public SubmitterServiceEndpointResolver(
            @Named(MaintenanceServiceEndpointConfig.SUBMITTER_BASE_URLS) Map<String, String> submitterBaseUrls) {
        this.submitterBaseUrls = Map.copyOf(submitterBaseUrls);
    }

    public Optional<String> resolveBaseUrl(String submitterService) {
        if (StringUtils.isBlank(submitterService)) {
            return Optional.empty();
        }
        return Optional.ofNullable(submitterBaseUrls.get(submitterService));
    }
}
