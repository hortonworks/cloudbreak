package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;

import reactor.fn.tuple.Tuple2;

@Component
public class EnvironmentCreationValidator implements Validator<Tuple2<Environment, EnvironmentRequest>> {

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    public ValidationResult validate(Tuple2<Environment, EnvironmentRequest> subject) {
        Environment environment = subject.t1;
        EnvironmentRequest request = subject.t2;
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateLdapConfigs(environment, request, resultBuilder);
        validateProxyConfigs(environment, request, resultBuilder);
        validateRdsConfigs(environment, request, resultBuilder);
        validateRegions(request.getRegions(), environment.getCredential(), resultBuilder);
        return resultBuilder.build();
    }

    private void validateLdapConfigs(Environment subject, EnvironmentRequest request, ValidationResultBuilder resultBuilder) {
        if (subject.getLdapConfigs().size() < request.getLdapConfigs().size()) {
            Set<String> foundLdaps = subject.getLdapConfigs().stream().map(LdapConfig::getName).collect(Collectors.toSet());
            Set<String> requestedLdaps = new HashSet<>(request.getLdapConfigs());
            requestedLdaps.removeAll(foundLdaps);
            resultBuilder.error(String.format("The following LDAP config(s) could not be found in the workspace: [%s]",
                    requestedLdaps.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateProxyConfigs(Environment subject, EnvironmentRequest request, ValidationResultBuilder resultBuilder) {
        if (subject.getProxyConfigs().size() < request.getProxyConfigs().size()) {
            Set<String> foundProxyConfigs = subject.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedProxyConfigs = new HashSet<>(request.getProxyConfigs());
            requestedProxyConfigs.removeAll(foundProxyConfigs);
            resultBuilder.error(String.format("The following Proxy config(s) could not be found in the workspace: [%s]",
                    requestedProxyConfigs.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateRdsConfigs(Environment subject, EnvironmentRequest request, ValidationResultBuilder resultBuilder) {
        if (subject.getRdsConfigs().size() < request.getRdsConfigs().size()) {
            Set<String> foundRdsConfigs = subject.getRdsConfigs().stream().map(RDSConfig::getName).collect(Collectors.toSet());
            Set<String> requestedRdsConfigs = new HashSet<>(request.getRdsConfigs());
            requestedRdsConfigs.removeAll(foundRdsConfigs);
            resultBuilder.error(String.format("The following RDS config(s) could not be found in the workspace: [%s]",
                    requestedRdsConfigs.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateRegions(Set<String> requestedRegions, Credential credential, ValidationResultBuilder resultBuilder) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(credential);
        platformResourceRequest.setCloudPlatform(credential.cloudPlatform());
        // TODO: no idea where to get it from: platformResourceRequest.setPlatformVariant();
        CloudRegions regions = platformParameterService.getRegionsByCredential(platformResourceRequest);
        Set<String> existingRegions = regions.getCloudRegions().keySet()
                .stream().map(Region::getRegionName).collect(Collectors.toSet());
        requestedRegions = new HashSet<>(requestedRegions);
        requestedRegions.removeAll(existingRegions);
        if (!requestedRegions.isEmpty()) {
            resultBuilder.error(String.format("The following regions does not exist in your cloud provider: [%s]. "
                            + "Existing regions are: [%s]",
                    requestedRegions.stream().collect(Collectors.joining(", ")),
                    existingRegions.stream().collect(Collectors.joining(", "))
            ));
        }
    }
}
