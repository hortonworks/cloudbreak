package com.sequenceiq.cloudbreak.controller.validation.environment;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class ClusterCreationEnvironmentValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationEnvironmentValidator.class);

    @Value("${datalake.validateAvailability}")
    private boolean validateDatalakeAvailability;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SdxClientService sdxClientService;

    public void validate(Stack stack, DetailedEnvironmentResponse environment, boolean distroxRequest,
        ValidationResult.ValidationResultBuilder validationBuilder) {
        String regionName = cloudPlatformConnectors.getDefault(platform(stack.cloudPlatform()))
                .displayNameToRegion(stack.getRegion());
        String displayName = cloudPlatformConnectors.getDefault(platform(stack.cloudPlatform()))
                .regionToDisplayName(stack.getRegion());
        if (environment != null) {
            if (!CollectionUtils.isEmpty(environment.getRegions().getNames())
                    && environment.getRegions()
                    .getNames()
                    .stream()
                    .noneMatch(region -> region.equals(regionName) || region.equals(displayName))) {
                validationBuilder.error(String.format("[%s] region is not enabled in [%s] environment. Enabled regions: [%s]", stack.getRegion(),
                        environment.getName(), environment.getRegions().getNames().stream().sorted().collect(Collectors.joining(","))));
            }
        }
        validateDatalakeConfig(stack, validationBuilder, distroxRequest);
    }

    public void validateRdsConfigNames(Set<String> rdsConfigNames, ValidationResultBuilder resultBuilder, Long workspaceId) {
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

    public void validateProxyConfig(String resourceCrn, ValidationResultBuilder resultBuilder) {
        if (StringUtils.isNotEmpty(resourceCrn)) {
            try {
                proxyConfigDtoService.getByCrn(resourceCrn);
            } catch (CloudbreakServiceException ex) {
                resultBuilder.error(String.format("The specified '%s' Proxy config resource couldn't be used: %s.", resourceCrn, ex.getMessage()));
            }
        }
    }

    public void validateAutoTls(
            ClusterV4Request clusterRequest,
            Stack stack,
            ValidationResultBuilder resultBuilder,
            String parentEnvironmentCloudPlatform) {
        boolean platformAutoTls = isAutoTlsSupportedByCloudPlatform(stack, parentEnvironmentCloudPlatform);
        Optional<Boolean> requestedAutoTlsOptional = ofNullable(clusterRequest.getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls);
        if (requestedAutoTlsOptional.isPresent()) {
            boolean requestedAutoTls = requestedAutoTlsOptional.get();
            if (!platformAutoTls && requestedAutoTls) {
                resultBuilder.error(String.format("AutoTLS is not supported by '%s' platform!", stack.getCloudPlatform()));
            }
        }
    }

    private boolean isAutoTlsSupportedByCloudPlatform(Stack stack, String parentEnvironmentCloudPlatform) {
        String cloudPlatform = Optional.ofNullable(parentEnvironmentCloudPlatform).orElse(stack.getCloudPlatform());
        CloudConnector<Object> connector = cloudPlatformConnectors.get(platform(cloudPlatform), Variant.variant(stack.getPlatformVariant()));
        PlatformParameters platformParameters = connector.parameters();
        return platformParameters.isAutoTlsSupported();
    }

    public boolean hasFreeIpaKerberosConfig(Stack stack) {
        return kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName())
                .map(kc -> KerberosType.FREEIPA == kc.getType())
                .orElse(Boolean.FALSE);
    }

    private void validateDatalakeConfig(Stack stack, ValidationResultBuilder resultBuilder, boolean distroxRequest) {
        if (CloudPlatform.MOCK.name().equalsIgnoreCase(stack.cloudPlatform())) {
            LOGGER.info("No Data Lake validation for MOCK provider");
        } else if (validateDatalakeAvailability && distroxRequest) {
            List<SdxClusterResponse> datalakes = sdxClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            if (datalakes.isEmpty()) {
                resultBuilder.error("Data Lake is not available in your environment!");
            }
        }
    }
}
