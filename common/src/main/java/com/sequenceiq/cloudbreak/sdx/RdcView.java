package com.sequenceiq.cloudbreak.sdx;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.template.TemplateEndpoint;
import com.sequenceiq.cloudbreak.template.TemplateRoleConfig;
import com.sequenceiq.cloudbreak.template.TemplateServiceConfig;

public class RdcView {

    private final String stackCrn;

    private final Set<TemplateEndpoint> endpoints;

    private final Set<TemplateServiceConfig> serviceConfigs;

    private final Set<TemplateRoleConfig> roleConfigs;

    private String remoteDataContext;

    public RdcView(
            String stackCrn,
            String remoteDataContext,
            Set<TemplateEndpoint> endpoints,
            Set<TemplateServiceConfig> serviceConfigs,
            Set<TemplateRoleConfig> roleConfigs) {
        this.stackCrn = Objects.requireNonNull(stackCrn);
        this.remoteDataContext = remoteDataContext;
        this.endpoints = Objects.requireNonNullElse(endpoints, new HashSet<>());
        this.serviceConfigs = Objects.requireNonNullElse(serviceConfigs, new HashSet<>());
        this.roleConfigs = Objects.requireNonNullElse(roleConfigs, new HashSet<>());
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public Optional<String> getRemoteDataContext() {
        return Optional.ofNullable(remoteDataContext);
    }

    public Set<String> getEndpoints(String service, String type) {
        return endpoints.stream()
                .filter(endpoint -> Objects.equals(endpoint.service(), service) && Objects.equals(endpoint.type(), type))
                .map(TemplateEndpoint::endpoint)
                .collect(Collectors.toSet());
    }

    public void extendEndpoints(Set<TemplateEndpoint> endpoints) {
        this.endpoints.addAll(endpoints);
    }

    public Map<String, String> getServiceConfigs(String service) {
        return serviceConfigs.stream()
                .filter(serviceConfig -> Objects.equals(serviceConfig.service(), service))
                .collect(Collectors.toMap(TemplateServiceConfig::key, TemplateServiceConfig::value));
    }

    public String getServiceConfig(String service, String configKey) {
        return getServiceConfigs(service).get(configKey);
    }

    public void extendServiceConfigs(Set<TemplateServiceConfig> serviceConfigs) {
        this.serviceConfigs.addAll(serviceConfigs);
    }

    public Map<String, String> getRoleConfigs(String service, String role) {
        return roleConfigs.stream()
                .filter(roleConfig -> Objects.equals(roleConfig.service(), service) && Objects.equals(roleConfig.role(), role))
                .collect(Collectors.toMap(TemplateRoleConfig::key, TemplateRoleConfig::value));
    }

    public void extendRoleConfigs(Set<TemplateRoleConfig> roleConfigs) {
        this.roleConfigs.addAll(roleConfigs);
    }

    public void updateRemoteDataContext(String remoteDataContext) {
        this.remoteDataContext = remoteDataContext;
    }
}
