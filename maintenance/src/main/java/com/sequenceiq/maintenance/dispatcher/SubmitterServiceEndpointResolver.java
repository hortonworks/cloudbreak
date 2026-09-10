package com.sequenceiq.maintenance.dispatcher;

import java.util.HashMap;
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

    private static final String CLOUDBREAK = "cloudbreak";

    private static final String DATALAKE = "datalake";

    private static final String FREEIPA = "freeipa";

    private final Map<String, String> submitterBaseUrls;

    @Inject
    public SubmitterServiceEndpointResolver(
            @Named(MaintenanceServiceEndpointConfig.CLOUDBREAK_SUBMITTER_BASE_URL) String cloudbreakBaseUrl,
            @Named(MaintenanceServiceEndpointConfig.DATALAKE_SUBMITTER_BASE_URL) String datalakeBaseUrl,
            @Named(MaintenanceServiceEndpointConfig.FREEIPA_SUBMITTER_BASE_URL) String freeipaBaseUrl) {
        this(buildUrlMap(cloudbreakBaseUrl, datalakeBaseUrl, freeipaBaseUrl));
    }

    SubmitterServiceEndpointResolver(Map<String, String> submitterBaseUrls) {
        this.submitterBaseUrls = Map.copyOf(submitterBaseUrls);
    }

    public Optional<String> resolveBaseUrl(String submitterService) {
        if (StringUtils.isBlank(submitterService)) {
            return Optional.empty();
        }
        return Optional.ofNullable(submitterBaseUrls.get(submitterService));
    }

    private static Map<String, String> buildUrlMap(String cloudbreakBaseUrl, String datalakeBaseUrl, String freeipaBaseUrl) {
        Map<String, String> urls = new HashMap<>();
        registerSubmitter(urls, CLOUDBREAK, cloudbreakBaseUrl);
        registerSubmitter(urls, DATALAKE, datalakeBaseUrl);
        registerSubmitter(urls, FREEIPA, freeipaBaseUrl);
        return urls;
    }

    private static void registerSubmitter(Map<String, String> urls, String submitterService, String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            urls.put(submitterService, baseUrl);
        }
    }
}
