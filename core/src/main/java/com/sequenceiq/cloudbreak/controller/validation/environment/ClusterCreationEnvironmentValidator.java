package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class ClusterCreationEnvironmentValidator {
    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    public ValidationResult validate(ClusterV4Request clusterRequest, Stack stack) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        EnvironmentView stackEnv = stack.getEnvironment();
        if (stackEnv != null && !CollectionUtils.isEmpty(stackEnv.getRegionSet())
                && stackEnv.getRegionSet().stream().noneMatch(region -> region.getName().equals(stack.getRegion()))) {
            resultBuilder.error(String.format("[%s] region is not enabled in [%s] environment. Enabled environments: [%s]", stack.getRegion(),
                    stackEnv.getName(), stackEnv.getRegionSet().stream().map(Region::getName).sorted().collect(Collectors.joining(","))));
        }
        Long workspaceId = stack.getWorkspace().getId();
        validateLdapConfig(workspaceId, clusterRequest, stackEnv, resultBuilder);
        validateProxyConfig(workspaceId, clusterRequest, stackEnv, resultBuilder);
        validateRdsConfigs(workspaceId, clusterRequest, stackEnv, resultBuilder);
        return resultBuilder.build();
    }

    public ValidationResult validate(RegisterDatalakeV4Request registerDatalakeRequest, Environment environment) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        Long workspaceId = environment.getWorkspace().getId();
        String environmentName = environment.getName();
        if (!CollectionUtils.isEmpty(environment.getDatalakeResources())) {
            resultBuilder.error("Only one external datalake can be registered to an environment!");
        }
        validateEnvironmentAwareResource(ldapConfigService.getByNameForWorkspaceId(registerDatalakeRequest.getLdapName(), workspaceId),
                environmentName, resultBuilder);
        for (String rdsConfigName : registerDatalakeRequest.getDatabaseNames()) {
            validateEnvironmentAwareResource(rdsConfigService.getByNameForWorkspaceId(rdsConfigName, workspaceId), environmentName, resultBuilder);
        }
        if (StringUtils.isNoneEmpty(registerDatalakeRequest.getKerberosName())) {
            validateEnvironmentAwareResource(kerberosConfigService.getByNameForWorkspaceId(registerDatalakeRequest.getKerberosName(), workspaceId),
                    environmentName, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void validateLdapConfig(Long workspaceId, ClusterV4Request request, EnvironmentView stackEnv, ValidationResultBuilder resultBuilder) {
        if (request.getLdapName() != null) {
            validateEnvironmentAwareResource(ldapConfigService.getByNameForWorkspaceId(request.getLdapName(), workspaceId), stackEnv, resultBuilder);
        }
    }

    private void validateProxyConfig(Long workspaceId, ClusterV4Request request, EnvironmentView stackEnv, ValidationResultBuilder resultBuilder) {
        if (StringUtils.isNotBlank(request.getProxyName())) {
            validateEnvironmentAwareResource(proxyConfigService.getByNameForWorkspaceId(request.getProxyName(), workspaceId), stackEnv, resultBuilder);
        }
    }

    private void validateRdsConfigs(Long workspaceId, ClusterV4Request request, EnvironmentView stackEnv, ValidationResultBuilder resultBuilder) {
        if (request.getDatabases() != null) {
            for (String rdsConfigName : request.getDatabases()) {
                validateEnvironmentAwareResource(rdsConfigService.getByNameForWorkspaceId(rdsConfigName, workspaceId), stackEnv, resultBuilder);
            }
        }
    }

    private void validateEnvironments(String resourceName, String resourceType, Set<String> environments, EnvironmentView stackEnv,
            ValidationResultBuilder resultBuilder) {
        if (stackEnv == null) {
            if (!CollectionUtils.isEmpty(environments)) {
                resultBuilder.error(String.format("Stack without environment cannot use %s %s resource which attached to an environment.",
                        resourceName, resourceType));
            }
        } else {
            if (!CollectionUtils.isEmpty(environments)
                    && environments.stream().noneMatch(resEnv -> resEnv.equals(stackEnv.getName()))) {
                resultBuilder.error(String.format("Stack cannot use %s %s resource which is not attached to %s environment and not global.",
                        resourceName, resourceType, stackEnv.getName()));
            }
        }
    }

    private <T extends EnvironmentAwareResource> void validateEnvironmentAwareResource(T resource,
            EnvironmentView stackEnv, ValidationResultBuilder resultBuilder) {
        if (stackEnv == null) {
            if (!CollectionUtils.isEmpty(resource.getEnvironments())) {
                resultBuilder.error(String.format("Stack without environment cannot use %s %s resource which attached to an environment.",
                        resource.getName(), resource.getClass().getSimpleName()));
            }
        } else {
            validateEnvironmentAwareResource(resource, stackEnv.getName(), resultBuilder);
        }
    }

    private <T extends EnvironmentAwareResource> void validateEnvironmentAwareResource(T resource,
            String environemntName, ValidationResultBuilder resultBuilder) {
        if (!CollectionUtils.isEmpty(resource.getEnvironments())
                && resource.getEnvironments().stream().noneMatch(resEnv -> resEnv.getName().equals(environemntName))) {
            resultBuilder.error(String.format("Stack cannot use %s %s resource which is not attached to %s environment and not global.",
                    resource.getName(), resource.getClass().getSimpleName(), environemntName));
        }
    }
}
