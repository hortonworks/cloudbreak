package com.sequenceiq.cloudbreak.controller.validation.environment;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
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
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
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
    private CloudPlatformConnectors cloudPlatformConnectors;

    public ValidationResult validate(ClusterV4Request clusterRequest, Stack stack, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        String regionName = cloudPlatformConnectors.getDefault(platform(stack.cloudPlatform()))
                .displayNameToRegion(stack.getRegion());
        String displayName = cloudPlatformConnectors.getDefault(platform(stack.cloudPlatform()))
                .regionToDisplayName(stack.getRegion());
        if (environment != null && !CollectionUtils.isEmpty(environment.getRegions().getNames())
                && environment.getRegions()
                .getNames()
                .stream()
                .noneMatch(region -> region.equals(regionName) || region.equals(displayName))) {
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
                    .forEach(dbName -> resultBuilder.error(
                            String.format("Stack cannot use '%s' Database resource which doesn't exist in the same workspace.", dbName)));
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
        boolean platformAutoTls = isAutoTlsSupportedByCloudPlatform(stack);
        Optional<Boolean> requestedAutoTlsOptional = ofNullable(clusterRequest.getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls);
        boolean effectiveAutoTls = platformAutoTls;
        if (requestedAutoTlsOptional.isPresent()) {
            boolean requestedAutoTls = requestedAutoTlsOptional.get();
            if (!platformAutoTls || !requestedAutoTls) {
                if (platformAutoTls) {
                    effectiveAutoTls = false;
                } else if (requestedAutoTls) {
                    resultBuilder.error(String.format("AutoTLS is not supported by '%s' platform!", stack.getCloudPlatform()));
                }
            }
        }
        if (effectiveAutoTls) {
            validateKerberosConfig(stack, resultBuilder);
        }
    }

    private boolean isAutoTlsSupportedByCloudPlatform(Stack stack) {
        CloudConnector<Object> connector = cloudPlatformConnectors.get(platform(stack.cloudPlatform()), Variant.variant(stack.getPlatformVariant()));
        PlatformParameters platformParameters = connector.parameters();
        return platformParameters.isAutoTlsSupported();
    }

    private void validateKerberosConfig(Stack stack, ValidationResultBuilder resultBuilder) {
        Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        boolean freeipa = kerberosConfig.map(kc -> KerberosType.FREEIPA == kc.getType()).orElse(Boolean.FALSE);
        if (!freeipa) {
            resultBuilder.error("FreeIPA is not available in your environment!");
        }
    }
}
