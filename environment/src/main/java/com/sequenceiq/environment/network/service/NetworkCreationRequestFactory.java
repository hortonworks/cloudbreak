package com.sequenceiq.environment.network.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class NetworkCreationRequestFactory {

    private final SubnetCidrProvider extendedSubnetCidrProvider;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public NetworkCreationRequestFactory(SubnetCidrProvider extendedSubnetCidrProvider,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.extendedSubnetCidrProvider = extendedSubnetCidrProvider;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public NetworkCreationRequest create(EnvironmentDto environment) {
        NetworkDto networkDto = environment.getNetwork();
        NetworkCreationRequest.Builder builder = new NetworkCreationRequest.Builder()
                .withStackName(getStackName(environment))
                .withEnvId(environment.getId())
                .withEnvName(environment.getName())
                .withCloudCredential(getCredential(environment))
                .withVariant(environment.getCloudPlatform())
                .withRegion(Region.region(environment.getLocation().getName()))
                .withNetworkCidr(networkDto.getNetworkCidr())
                .withSubnetCidrs(getSubNetCidrs(networkDto.getNetworkCidr()));
        getNoPublicIp(networkDto).ifPresent(builder::withNoPublicIp);
        getNoFirewallRules(networkDto).ifPresent(builder::withNoFirewallRules);
        return builder.build();
    }

    private CloudCredential getCredential(EnvironmentDto environment) {
        return credentialToCloudCredentialConverter.convert(environment.getCredential());
    }

    public String getStackName(EnvironmentDto environment) {
        return String.join("-", environment.getName(), String.valueOf(environment.getNetwork().getId()));
    }

    private Set<String> getSubNetCidrs(String networkCidr) {
        return extendedSubnetCidrProvider.provide(networkCidr);
    }

    private Optional<Boolean> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoPublicIp);
    }

    private Optional<Boolean> getNoFirewallRules(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoFirewallRules);
    }
}
