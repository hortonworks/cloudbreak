package com.sequenceiq.cloudbreak.controller.validation.environment;

import static java.util.Optional.ofNullable;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class ClusterCreationEnvironmentValidator {
    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    public ValidationResult validate(ClusterV4Request clusterRequest, Stack stack, User user, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (environment != null && !CollectionUtils.isEmpty(environment.getRegions().getRegions())
                && environment.getRegions().getRegions().stream().noneMatch(region -> region.equals(stack.getRegion()))) {
            resultBuilder.error(String.format("[%s] region is not enabled in [%s] environment. Enabled regions: [%s]", stack.getRegion(),
                    environment.getName(), environment.getRegions().getRegions().stream().sorted().collect(Collectors.joining(","))));
        }
        Long workspaceId = stack.getWorkspace().getId();

        validateConfigByName(
                clusterRequest.getLdapName(),
                workspaceId,
                resultBuilder,
                ldapConfigService::getByNameForWorkspaceId,
                LdapConfig.class.getSimpleName());

        validateConfigByName(
                clusterRequest.getKerberosName(),
                workspaceId,
                resultBuilder,
                kerberosConfigService::getByNameForWorkspaceId,
                KerberosConfig.class.getSimpleName());
        validateRdsConfigNames(clusterRequest.getDatabases(), resultBuilder, workspaceId);
        validateProxyConfig(clusterRequest.getProxyConfigCrn(), stack.getWorkspace().getTenant().getName(), resultBuilder, user.getUserCrn());
        return resultBuilder.build();
    }

    private void validateConfigByName(
            String configName, Long workspaceId,
            ValidationResultBuilder resultBuilder,
            BiFunction<String, Long, ? extends WorkspaceAwareResource> repositoryCall,
            String resourceTypeName) {

        if (StringUtils.isNotEmpty(configName)) {
            try {
                repositoryCall.apply(configName, workspaceId);
            } catch (NotFoundException nfe) {
                resultBuilder.error(String.format("Stack cannot use '%s' %s resource which doesn't exist in the same workspace.", configName, resourceTypeName));
            }
        }
    }

    private void validateRdsConfigNames(Set<String> rdsConfigNames, ValidationResultBuilder resultBuilder, Long workspaceId) {
        if (!ofNullable(rdsConfigNames).orElse(Set.of()).isEmpty()) {
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

    private void validateProxyConfig(String resourceCrn, String accountId, ValidationResultBuilder resultBuilder, String userCrn) {
        if (StringUtils.isNotEmpty(resourceCrn)) {
            try {
                proxyConfigDtoService.get(resourceCrn, accountId, userCrn);
            } catch (CloudbreakServiceException ex) {
                resultBuilder.error(String.format("The specified '%s' Proxy config resource couldn't be used: %s.", resourceCrn, ex.getMessage()));
            }
        }
    }
}
