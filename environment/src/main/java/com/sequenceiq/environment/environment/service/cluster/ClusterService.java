package com.sequenceiq.environment.environment.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterServiceConfigurationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ServiceConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterServiceConfigurationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ClusterServiceConfigurationResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoBase;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    private final StackV4Endpoint stackV4Endpoint;

    private final FreeIpaService freeIpaService;

    public ClusterService(StackV4Endpoint stackV4Endpoint, FreeIpaService freeIpaService) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.freeIpaService = freeIpaService;
    }

    public List<String> getClustersNamesByEncryptionProfile(String encryptionProfileCrn) {
        return stackV4Endpoint.getClustersNamesByEncryptionProfile(0L, encryptionProfileCrn);
    }

    public void updateTrustedRealmsOnClusters(Optional<EnvironmentDto> environmentDto) {
        EnvironmentType environmentType = environmentDto.map(EnvironmentDtoBase::getEnvironmentType).orElse(EnvironmentType.PUBLIC_CLOUD);

        String envCrn = environmentDto.map(EnvironmentDtoBase::getResourceCrn).orElse(null);

        ClusterServiceConfigurationRequest configRequest = new ClusterServiceConfigurationRequest();
        configRequest.setServiceName("core_settings");
        configRequest.setConfigName("trusted_realms");

        UpdateClusterServiceConfigurationRequest updateClusterServiceConfigurationRequest = new UpdateClusterServiceConfigurationRequest();
        ServiceConfiguration trustedRealmsConfiguration = new ServiceConfiguration();
        trustedRealmsConfiguration.setServiceName("core_settings");
        trustedRealmsConfiguration.setConfigName("trusted_realms");
        String realm = freeIpaService.describe(envCrn).map(describeFreeIpaResponse ->
                        describeFreeIpaResponse.getTrust().getRealm().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new CloudbreakServiceException("Failed to get realm from FreeIPA for environment " + envCrn));
        trustedRealmsConfiguration.setValue(realm);
        updateClusterServiceConfigurationRequest.setServiceConfigurations(List.of(trustedRealmsConfiguration));

        List<String> stackCrns = getStackCrnsForConfigUpdate(envCrn, environmentType);
        stackCrns.forEach(crn -> {
            ClusterServiceConfigurationResponse configResponse = stackV4Endpoint.getClusterServiceConfiguration(0L, crn, configRequest);
            Optional<String> valueOpt = Optional.ofNullable(configResponse).map(ClusterServiceConfigurationResponse::getValue);
            boolean realmAlreadyConfigured = valueOpt.map(val -> val.contains(realm)).orElse(false);
            if (!realmAlreadyConfigured) {
                String realmList = valueOpt.map(val -> val + "," + realm).orElse(realm);
                trustedRealmsConfiguration.setValue(realmList);
            }
            stackV4Endpoint.updateClusterServiceConfiguration(0L, crn, updateClusterServiceConfigurationRequest);
        });
    }

    public List<String> getStackCrnsForConfigUpdate(String envCrn, EnvironmentType environmentType) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, envCrn, false);
        List<String> responseToLog = Optional.ofNullable(stackViewV4Responses.getResponses()).orElse(List.of()).stream()
                .map(response -> String.format("[Name: %s; Crn: %s; Status: %s, ClusterStatus: %s]",
                        response.getName(), response.getCrn(), response.getStatus(), response.getCluster().getStatus()))
                .collect(Collectors.toList());
        LOGGER.info("Stacks returned for configuration update: {}", responseToLog);

        Predicate<StackViewV4Response> operationalStacks = stack -> AVAILABLE.equals(stack.getStatus());
        Predicate<StackViewV4Response> workloadsBasedOnEnvironmentType = stack ->
                environmentType == EnvironmentType.PUBLIC_CLOUD || StackType.WORKLOAD.name().equals(stack.getStackType());

        return stackViewV4Responses.getResponses().stream()
                .filter(operationalStacks)
                .filter(workloadsBasedOnEnvironmentType)
                .map(StackViewV4Response::getCrn)
                .collect(Collectors.toList());
    }
}
