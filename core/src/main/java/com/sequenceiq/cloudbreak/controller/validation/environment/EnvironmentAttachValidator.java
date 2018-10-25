package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class EnvironmentAttachValidator {

    public ValidationResult validate(EnvironmentAttachRequest request,
            Set<LdapConfig> ldapsToAttach, Set<ProxyConfig> proxiesToAttach, Set<RDSConfig> rdssToAttach) {

        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateLdaps(request, ldapsToAttach, resultBuilder);
        validateProxies(request, proxiesToAttach, resultBuilder);
        validateRdss(request, rdssToAttach, resultBuilder);
        return resultBuilder.build();
    }

    private void validateLdaps(EnvironmentAttachRequest request, Set<LdapConfig> ldapsToAttach, ValidationResultBuilder resultBuilder) {
        if (ldapsToAttach.size() < request.getLdapConfigs().size()) {
            Set<String> attachableNames = ldapsToAttach.stream().map(LdapConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getLdapConfigs());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("LdapConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    requestedNames.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateProxies(EnvironmentAttachRequest request, Set<ProxyConfig> proxiesToAttach, ValidationResultBuilder resultBuilder) {
        if (proxiesToAttach.size() < request.getProxyConfigs().size()) {
            Set<String> attachableNames = proxiesToAttach.stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getProxyConfigs());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("ProxyConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    requestedNames.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateRdss(EnvironmentAttachRequest request, Set<RDSConfig> rdssToAttach, ValidationResultBuilder resultBuilder) {
        if (rdssToAttach.size() < request.getRdsConfigs().size()) {
            Set<String> attachableNames = rdssToAttach.stream().map(RDSConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getRdsConfigs());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("RdsConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    requestedNames.stream().collect(Collectors.joining(", "))));
        }
    }
}
