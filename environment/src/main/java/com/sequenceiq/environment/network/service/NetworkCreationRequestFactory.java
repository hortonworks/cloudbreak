package com.sequenceiq.environment.network.service;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class NetworkCreationRequestFactory {

    @Inject
    private SubnetCidrProvider subnetCidrProvider;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public NetworkCreationRequest create(EnvironmentDto environment) {
        NetworkDto networkDto = environment.getNetwork();
        NetworkCreationRequest.Builder builder = new NetworkCreationRequest.Builder()
                .withEnvName(environment.getName())
                .withCloudCredential(credentialToCloudCredentialConverter.convert(environment.getCredential()))
                .withVariant(environment.getCloudPlatform())
                .withRegion(Region.region(environment.getLocation().getName()))
                .withNetworkCidr(networkDto.getNetworkCidr())
                .withId(networkDto.getId())
                .withSubnetCidrs(getSubNetCidrs(networkDto.getNetworkCidr()));
        getNoPublicIp(networkDto).ifPresent(builder::withNoPublicIp);
        getNoFirewallRules(networkDto).ifPresent(builder::withNoFirewallRules);
        return builder.build();
    }

    private Set<String> getSubNetCidrs(String networkCidr) {
        return subnetCidrProvider.provide(networkCidr);
    }

    private Optional<Boolean> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoPublicIp);
    }

    private Optional<Boolean> getNoFirewallRules(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoFirewallRules);
    }
}
