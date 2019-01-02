package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentCreationValidator {

    @Inject
    private EnvironmentRegionValidator environmentRegionValidator;

    public ValidationResult validate(Environment environment, EnvironmentV4Request request, CloudRegions cloudRegions) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        validateLdapConfigs(environment, request, resultBuilder);
        validateProxyConfigs(environment, request, resultBuilder);
        validateRdsConfigs(environment, request, resultBuilder);
        validateKubernetesConfigs(environment, request, resultBuilder);
        environmentRegionValidator.validateRegions(request.getRegions(), cloudRegions, environment.getCloudPlatform(), resultBuilder);
        environmentRegionValidator.validateLocation(request.getLocation(), request.getRegions(), environment, resultBuilder);
        validateKerberosConfigs(environment, request, resultBuilder);
        return resultBuilder.build();
    }

    private void validateKerberosConfigs(Environment environment, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (environment.getKerberosConfigs().size() < request.getKerberoses().size()) {
            Set<String> foundKerberosConfigs = environment.getKerberosConfigs().stream()
                    .map(KerberosConfig::getName).collect(Collectors.toSet());
            Set<String> requestedKerberosConfigs = new HashSet<>(request.getKerberoses());
            requestedKerberosConfigs.removeAll(foundKerberosConfigs);
            resultBuilder.error(String.format("The following Kerberos config(s) could not be found in the workspace: [%s]",
                    requestedKerberosConfigs.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateLdapConfigs(Environment subject, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getLdapConfigs().size() < request.getLdaps().size()) {
            Set<String> foundLdaps = subject.getLdapConfigs().stream().map(LdapConfig::getName).collect(Collectors.toSet());
            Set<String> requestedLdaps = new HashSet<>(request.getLdaps());
            requestedLdaps.removeAll(foundLdaps);
            resultBuilder.error(String.format("The following LDAP config(s) could not be found in the workspace: [%s]",
                    requestedLdaps.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateProxyConfigs(Environment subject, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getProxyConfigs().size() < request.getProxies().size()) {
            Set<String> foundProxyConfigs = subject.getProxyConfigs().stream().map(ProxyConfig::getName).collect(Collectors.toSet());
            Set<String> requestedProxyConfigs = new HashSet<>(request.getProxies());
            requestedProxyConfigs.removeAll(foundProxyConfigs);
            resultBuilder.error(String.format("The following Proxy config(s) could not be found in the workspace: [%s]",
                    requestedProxyConfigs.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateRdsConfigs(Environment subject, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getRdsConfigs().size() < request.getDatabases().size()) {
            Set<String> foundRdsConfigs = subject.getRdsConfigs().stream().map(RDSConfig::getName).collect(Collectors.toSet());
            Set<String> requestedRdsConfigs = new HashSet<>(request.getDatabases());
            requestedRdsConfigs.removeAll(foundRdsConfigs);
            resultBuilder.error(String.format("The following RDS config(s) could not be found in the workspace: [%s]",
                    requestedRdsConfigs.stream().collect(Collectors.joining(", "))));
        }
    }

    private void validateKubernetesConfigs(Environment subject, EnvironmentV4Request request, ValidationResultBuilder resultBuilder) {
        if (subject.getKubernetesConfigs().size() < request.getKubernetes().size()) {
            Set<String> foundKubernetesConfigs = subject.getKubernetesConfigs().stream().map(KubernetesConfig::getName).collect(Collectors.toSet());
            Set<String> requestedKubernetesConfigs = new HashSet<>(request.getKubernetes());
            requestedKubernetesConfigs.removeAll(foundKubernetesConfigs);
            resultBuilder.error(String.format("The following Kubernetes config(s) could not be found in the workspace: [%s]",
                    requestedKubernetesConfigs.stream().collect(Collectors.joining(", "))));
        }
    }
}