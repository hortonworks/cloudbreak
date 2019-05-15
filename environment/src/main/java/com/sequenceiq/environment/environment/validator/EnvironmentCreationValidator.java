package com.sequenceiq.environment.environment.validator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.validator.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.proxy.ProxyConfig;

@Component
public class EnvironmentCreationValidator {

    @Inject
    private EnvironmentRegionValidator environmentRegionValidator;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    public ValidationResult validate(Environment environment, EnvironmentV1Request request, CloudRegions cloudRegions) {
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateProxyConfigs(environment, request, resultBuilder);
        environmentRegionValidator.validateRegions(request.getRegions(), cloudRegions, cloudPlatform, resultBuilder);
        environmentRegionValidator.validateLocation(request.getLocation(), request.getRegions(), environment, resultBuilder);
        validateNetwork(request, cloudPlatform, resultBuilder);
        return resultBuilder.build();
    }

    private void validateProxyConfigs(Environment subject, EnvironmentV1Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getProxyConfigs().size() < request.getProxies().size()) {
            Set<String> foundProxyConfigs = subject.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedProxyConfigs = new HashSet<>(request.getProxies());
            requestedProxyConfigs.removeAll(foundProxyConfigs);
            resultBuilder.error(String.format("The following Proxy config(s) could not be found in the workspace: [%s]",
                    String.join(", ", requestedProxyConfigs)));
        }
    }

    private void validateNetwork(EnvironmentV1Request request, String cloudPlatform, ValidationResultBuilder resultBuilder) {
        EnvironmentNetworkV1Request networkRequest = request.getNetwork();
        if (networkRequest != null) {
            EnvironmentNetworkValidator environmentNetworkValidator = environmentNetworkValidatorsByCloudPlatform.get(CloudPlatform.valueOf(cloudPlatform));
            if (environmentNetworkValidator != null) {
                environmentNetworkValidator.validate(networkRequest, resultBuilder);
            } else {
                resultBuilder.error(String.format("Environment specific network is not supported for cloud platform: '%s'!", cloudPlatform));
            }
        }
    }
}