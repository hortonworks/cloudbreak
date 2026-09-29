package com.sequenceiq.it.cloudbreak.preflight;

import java.time.Duration;

import jakarta.inject.Inject;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.it.cloudbreak.config.VaultProperties;

@Component
public class VaultAvailabilityCheck implements PreflightCheck {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Inject
    private VaultProperties vaultProperties;

    @Inject
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory;

    @Override
    public PreflightCheckResult check() {
        String healthUrl = vaultProperties.getHealthUrl();
        try {
            RestTemplate restTemplate = reachabilityRestTemplateFactory.create(TIMEOUT);
            HttpStatusCode status = restTemplate.getForEntity(healthUrl, String.class).getStatusCode();
            if (!status.is2xxSuccessful()) {
                return PreflightCheckResult.fail(getName(), "Vault is not healthy at " + healthUrl + ": HTTP " + status.value()
                        + " (501: not initialized, 503: sealed)");
            }
            return PreflightCheckResult.pass(getName(), "Vault healthy at " + healthUrl);
        } catch (RestClientException e) {
            return PreflightCheckResult.fail(getName(), "Vault unreachable at " + healthUrl + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "vault-availability";
    }
}
