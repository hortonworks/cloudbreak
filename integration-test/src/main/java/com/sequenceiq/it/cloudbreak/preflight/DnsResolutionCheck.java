package com.sequenceiq.it.cloudbreak.preflight;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.config.CcmProperties;

@Component
public class DnsResolutionCheck implements PreflightCheck {

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Inject
    private CcmProperties ccmProperties;

    @Override
    public PreflightCheckResult check() {
        List<String> hostnamesToResolve = new ArrayList<>(endpointResolver.resolveActiveProviderHostnames());
        if (StringUtils.isNotBlank(ccmProperties.getHost())) {
            hostnamesToResolve.add(ccmProperties.getHost());
        }
        if (hostnamesToResolve.isEmpty()) {
            return PreflightCheckResult.skip(getName(), "no hostnames to resolve");
        }
        List<String> unresolved = new ArrayList<>();
        for (String hostname : hostnamesToResolve) {
            try {
                InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                unresolved.add(hostname);
            }
        }
        if (unresolved.isEmpty()) {
            return PreflightCheckResult.pass(getName(), "resolved: " + hostnamesToResolve);
        }
        return PreflightCheckResult.fail(getName(), "DNS resolution failed for: " + unresolved);
    }

    @Override
    public String getName() {
        return "dns-resolution";
    }
}
