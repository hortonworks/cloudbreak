package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

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

        validateConfigByName(
                clusterRequest.getLdapName(),
                workspaceId,
                resultBuilder,
                ldapConfigService::getByNameForWorkspaceId,
                LdapConfig.class.getSimpleName());

        validateConfigByName(
                clusterRequest.getProxyName(),
                workspaceId,
                resultBuilder,
                proxyConfigService::getByNameForWorkspaceId,
                ProxyConfig.class.getSimpleName());

        validateConfigByName(
                clusterRequest.getKerberosName(),
                workspaceId,
                resultBuilder,
                kerberosConfigService::getByNameForWorkspaceId,
                KerberosConfig.class.getSimpleName());
        validateRdsConfigNames(clusterRequest.getDatabases(), resultBuilder, workspaceId);
        return resultBuilder.build();
    }

    public ValidationResult validate(RegisterDatalakeV4Request registerDatalakeRequest, Environment environment) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        Long workspaceId = environment.getWorkspace().getId();
        if (!CollectionUtils.isEmpty(environment.getDatalakeResources())) {
            resultBuilder.error("Only one external datalake can be registered to an environment!");
        }

        validateConfigByName(
                registerDatalakeRequest.getLdapName(),
                workspaceId,
                resultBuilder,
                ldapConfigService::getByNameForWorkspaceId,
                LdapConfig.class.getSimpleName());

        validateConfigByName(
                registerDatalakeRequest.getKerberosName(),
                workspaceId,
                resultBuilder,
                kerberosConfigService::getByNameForWorkspaceId,
                KerberosConfig.class.getSimpleName());

        validateRdsConfigNames(registerDatalakeRequest.getDatabaseNames(), resultBuilder, workspaceId);
        return resultBuilder.build();
    }

    private void validateConfigByName(
            String configName, Long workspaceId,
            ValidationResultBuilder resultBuilder,
            BiFunction<String, Long, ? extends WorkspaceAwareResource> repositoryCall,
            String resourceTypeName) {

        if (StringUtils.isNoneEmpty(configName)) {
            try {
                repositoryCall.apply(configName, workspaceId);
            } catch (NotFoundException nfe) {
                resultBuilder.error(String.format("Stack cannot use '%s' %s resource which doesn't exist in the same workspace.", configName, resourceTypeName));
            }
        }
    }

    private void validateRdsConfigNames(Set<String> rdsConfigNames, ValidationResultBuilder resultBuilder, Long workspaceId) {
        if (!rdsConfigNames.isEmpty()) {
            Set<String> foundDatabaseNames = rdsConfigService.getByNamesForWorkspaceId(rdsConfigNames, workspaceId).stream()
                    .map(RDSConfig::getName)
                    .collect(Collectors.toSet());

            rdsConfigNames
                    .stream()
                    .filter(dbName -> !foundDatabaseNames.contains(dbName))
                    .forEach(dbName -> {
                        resultBuilder.error(String.format("Stack cannot use '%s' Database resource which doesn't exist in the same workspace.", dbName));
                    });
        }
    }
}
