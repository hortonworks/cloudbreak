package com.sequenceiq.environment.environment.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentAttachV1Request;
import com.sequenceiq.environment.proxy.ProxyConfig;

@Component
public class EnvironmentAttachValidator {

    public ValidationResult validate(EnvironmentAttachV1Request request, Set<ProxyConfig> proxiesToAttach) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
//        validateLdaps(request, ldapsToAttach, resultBuilder);
        validateProxies(request, proxiesToAttach, resultBuilder);
        return resultBuilder.build();
    }

//    private void validateLdaps(EnvironmentAttachV4Request request, Set<LdapConfig> ldapsToAttach, ValidationResultBuilder resultBuilder) {
//        if (ldapsToAttach.size() < request.getLdaps().size()) {
//            Set<String> attachableNames = ldapsToAttach.stream().map(LdapConfig::getName).collect(Collectors.toSet());
//            Set<String> requestedNames = new HashSet<>(request.getLdaps());
//            requestedNames.removeAll(attachableNames);
//            resultBuilder.error(String.format("LdapConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
//                    String.join(", ", requestedNames)));
//        }
//    }

    private void validateProxies(EnvironmentAttachV1Request request, Set<ProxyConfig> proxiesToAttach, ValidationResultBuilder resultBuilder) {
        if (proxiesToAttach.size() < request.getProxies().size()) {
            Set<String> attachableNames = proxiesToAttach.stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getProxies());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("ProxyConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    String.join(", ", requestedNames)));
        }
    }
}
