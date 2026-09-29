package com.sequenceiq.it.cloudbreak.preflight;

import java.time.Duration;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;

@Component
public class FreeipaCatalogAvailabilityCheck implements PreflightCheck {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory;

    @Override
    public PreflightCheckResult check() {
        String catalogUrl = commonCloudProperties.getFreeipaImageCatalogUrl();
        if (StringUtils.isBlank(catalogUrl)) {
            return PreflightCheckResult.skip(getName(), "no FreeIPA image catalog url configured");
        }
        try {
            RestTemplate restTemplate = reachabilityRestTemplateFactory.create(TIMEOUT);
            HttpStatusCode status = restTemplate.getForEntity(catalogUrl, String.class).getStatusCode();
            if (!status.is2xxSuccessful()) {
                return PreflightCheckResult.fail(getName(), "FreeIPA catalog not available at " + catalogUrl + ": HTTP " + status.value());
            }
            return PreflightCheckResult.pass(getName(), "FreeIPA catalog reachable at " + catalogUrl);
        } catch (RestClientException e) {
            return PreflightCheckResult.fail(getName(), "FreeIPA catalog unreachable at " + catalogUrl + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "freeipa-catalog-availability";
    }
}
