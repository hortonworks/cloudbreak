package com.sequenceiq.environment.environment.validation.validators;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.proxy.ProxyConfig;

@Component
public class EnvironmentAttachValidator {

    public ValidationResult validate(EnvironmentAttachRequest request, Set<ProxyConfig> proxiesToAttach) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateProxies(request, proxiesToAttach, resultBuilder);
        return resultBuilder.build();
    }

    private void validateProxies(EnvironmentAttachRequest request, Set<ProxyConfig> proxiesToAttach, ValidationResultBuilder resultBuilder) {
        if (proxiesToAttach.size() < request.getProxies().size()) {
            Set<String> attachableNames = proxiesToAttach.stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedNames = new HashSet<>(request.getProxies());
            requestedNames.removeAll(attachableNames);
            resultBuilder.error(String.format("ProxyConfigs [%s] cannot be found in the workspace, therefore cannot be attached.",
                    String.join(", ", requestedNames)));
        }
    }
}
