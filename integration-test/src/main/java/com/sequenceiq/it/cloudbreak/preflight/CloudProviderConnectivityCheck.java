package com.sequenceiq.it.cloudbreak.preflight;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CloudProviderConnectivityCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CloudProviderConnectivityCheck.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Inject
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory;

    @Override
    public PreflightCheckResult check() {
        List<String> hostnames = endpointResolver.resolveActiveProviderHostnames();
        if (hostnames.isEmpty()) {
            return PreflightCheckResult.skip(getName(), "no external endpoints to check for cloud provider " + endpointResolver.activeCloudPlatform());
        }
        List<String> unreachable = new ArrayList<>();
        RestTemplate restTemplate = reachabilityRestTemplateFactory.create(TIMEOUT);
        for (String hostname : hostnames) {
            String url = "https://" + hostname + "/";
            try {
                restTemplate.execute(url, HttpMethod.HEAD, null, response -> null);
            } catch (RestClientException e) {
                LOG.warn("Cloud provider endpoint {} is unreachable", url, e);
                Throwable cause = e.getMostSpecificCause();
                unreachable.add(hostname + " (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
            }
        }
        if (unreachable.isEmpty()) {
            return PreflightCheckResult.pass(getName(), "reached " + hostnames);
        }
        return PreflightCheckResult.fail(getName(), "could not reach: " + unreachable);
    }

    @Override
    public String getName() {
        return "cloud-provider-connectivity";
    }
}
