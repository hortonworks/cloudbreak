package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.environment.network.EnvironmentNetworkValidator;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentCreationValidator {

    @Inject
    private EnvironmentRegionValidator environmentRegionValidator;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    public ValidationResult validate(Environment environment, EnvironmentV4Request request, CloudRegions cloudRegions) {
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateProxyConfigs(environment, request, resultBuilder);
        environmentRegionValidator.validateRegions(request.getRegions(), cloudRegions, cloudPlatform, resultBuilder);
        environmentRegionValidator.validateLocation(request.getLocation(), request.getRegions(), environment, resultBuilder);
        validateNetwork(request, cloudPlatform, resultBuilder);
        return resultBuilder.build();
    }

    private void validateProxyConfigs(Environment subject, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getProxyConfigs().size() < request.getProxies().size()) {
            Set<String> foundProxyConfigs = subject.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedProxyConfigs = new HashSet<>(request.getProxies());
            requestedProxyConfigs.removeAll(foundProxyConfigs);
            resultBuilder.error(String.format("The following Proxy config(s) could not be found in the workspace: [%s]",
                    String.join(", ", requestedProxyConfigs)));
        }
    }

    private void validateNetwork(EnvironmentV4Request request, String cloudPlatform, ValidationResultBuilder resultBuilder) {
        EnvironmentNetworkV4Request networkV4Request = request.getNetwork();
        if (networkV4Request != null) {
            EnvironmentNetworkValidator environmentNetworkValidator = environmentNetworkValidatorsByCloudPlatform.get(CloudPlatform.valueOf(cloudPlatform));
            if (environmentNetworkValidator != null) {
                environmentNetworkValidator.validate(networkV4Request, resultBuilder);
            } else {
                resultBuilder.error(String.format("Environment specific network is not supported for cloud platform: '%s'!", cloudPlatform));
            }
        }
    }
}