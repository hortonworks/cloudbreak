package com.sequenceiq.cloudbreak.controller.validation.environment;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.cluster.DefaultAutoTlsFlagProvider;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class ClusterCreationEnvironmentValidator {
    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private DefaultAutoTlsFlagProvider defaultAutoTlsFlagProvider;

    public ValidationResult validate(ClusterV4Request clusterRequest, Stack stack, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (environment != null && !CollectionUtils.isEmpty(environment.getRegions().getNames())
                && environment.getRegions().getNames().stream().noneMatch(region -> region.equals(stack.getRegion()))) {
            resultBuilder.error(String.format("[%s] region is not enabled in [%s] environment. Enabled regions: [%s]", stack.getRegion(),
                    environment.getName(), environment.getRegions().getNames().stream().sorted().collect(Collectors.joining(","))));
        }
        Long workspaceId = stack.getWorkspace().getId();
        validateRdsConfigNames(clusterRequest.getDatabases(), resultBuilder, workspaceId);
        validateProxyConfig(clusterRequest.getProxyConfigCrn(), resultBuilder);
        validateAutoTls(clusterRequest, stack, resultBuilder);
        return resultBuilder.build();
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

    private void validateProxyConfig(String resourceCrn, ValidationResultBuilder resultBuilder) {
        if (StringUtils.isNotEmpty(resourceCrn)) {
            try {
                proxyConfigDtoService.getByCrn(resourceCrn);
            } catch (CloudbreakServiceException ex) {
                resultBuilder.error(String.format("The specified '%s' Proxy config resource couldn't be used: %s.", resourceCrn, ex.getMessage()));
            }
        }
    }

    private void validateAutoTls(ClusterV4Request clusterRequest, Stack stack, ValidationResultBuilder resultBuilder) {
        Boolean autoTls = Optional.ofNullable(clusterRequest.getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls)
                .orElse(defaultAutoTlsFlagProvider.defaultAutoTls(stack.getCloudPlatform()));
        if (autoTls) {
            Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
            boolean freeipa = kerberosConfig.map(kc -> KerberosType.FREEIPA == kc.getType()).orElse(Boolean.FALSE);
            if (!freeipa) {
                resultBuilder.error("AutoTls is only enabled for clusters with FreeIpa!");
            }
        }
    }
}
