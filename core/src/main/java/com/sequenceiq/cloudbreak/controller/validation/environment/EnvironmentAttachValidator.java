package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class EnvironmentAttachValidator {

    public ValidationResult validate(EnvironmentAttachV4Request request,
            Set<LdapConfig> ldapsToAttach, Set<ProxyConfig> proxiesToAttach, Set<RDSConfig> rdssToAttach) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateLdaps(request, ldapsToAttach, resultBuilder);
        validateProxies(request, proxiesToAttach, resultBuilder);
        validateRdss(request, rdssToAttach, resultBuilder);
        return resultBuilder.build();
    }

    private void validateLdaps(EnvironmentAttachV4Request request, Set<LdapConfig> ldapsToAttach, ValidationResultBuilder resultBuilder) {
        if (ldapsToAttach.size() < request.getLdaps().size()) {
            Set<String> attachableNames = ldapsToAttach.stream().map(LdapConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getLdaps());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("LdapConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    String.join(", ", requestedNames)));
        }
    }

    private void validateProxies(EnvironmentAttachV4Request request, Set<ProxyConfig> proxiesToAttach, ValidationResultBuilder resultBuilder) {
        if (proxiesToAttach.size() < request.getProxies().size()) {
            Set<String> attachableNames = proxiesToAttach.stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getProxies());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("ProxyConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    String.join(", ", requestedNames)));
        }
    }

    private void validateRdss(EnvironmentAttachV4Request request, Set<RDSConfig> rdssToAttach, ValidationResultBuilder resultBuilder) {
        if (rdssToAttach.size() < request.getDatabases().size()) {
            Set<String> attachableNames = rdssToAttach.stream().map(RDSConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getDatabases());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("RdsConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    String.join(", ", requestedNames)));
        }
    }
}
