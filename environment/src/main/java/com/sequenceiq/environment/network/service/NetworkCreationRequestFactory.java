package com.sequenceiq.environment.network.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.common.cost.CostTagging;
import com.sequenceiq.cloudbreak.common.service.CDPTagMergeRequest;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class NetworkCreationRequestFactory {

    private final SubnetCidrProvider extendedSubnetCidrProvider;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final CostTagging costTagging;

    public NetworkCreationRequestFactory(SubnetCidrProvider extendedSubnetCidrProvider,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            CostTagging costTagging) {
        this.extendedSubnetCidrProvider = extendedSubnetCidrProvider;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.costTagging = costTagging;
    }

    public NetworkCreationRequest create(EnvironmentDto environment) {
        NetworkDto networkDto = environment.getNetwork();

        CDPTagMergeRequest mergeRequest = CDPTagMergeRequest.Builder
                .builder()
                .withEnvironmentTags(environment.getTags().getUserDefinedTags())
                .withPlatform(environment.getCloudPlatform())
                .withRequestTags(environment.getTags().getDefaultTags())
                .build();

        NetworkCreationRequest.Builder builder = new NetworkCreationRequest.Builder()
                .withStackName(getStackName(environment))
                .withEnvId(environment.getId())
                .withEnvName(environment.getName())
                .withEnvCrn(environment.getResourceCrn())
                .withCloudCredential(getCredential(environment))
                .withVariant(environment.getCloudPlatform())
                .withRegion(Region.region(environment.getLocation().getName()))
                .withNetworkCidr(networkDto.getNetworkCidr())
                .withPrivateSubnetEnabled(getPrivateSubnetEnabled(environment))
                .withUserName(getUserFromCrn(environment.getCreator()))
                .withCreatorCrn(environment.getCreator())
                .withTags(costTagging.mergeTags(mergeRequest))
                .withSubnetCidrs(getSubNetCidrs(networkDto.getNetworkCidr()));
        getNoPublicIp(networkDto).ifPresent(builder::withNoPublicIp);
        return builder.build();
    }

    private CloudCredential getCredential(EnvironmentDto environment) {
        return credentialToCloudCredentialConverter.convert(environment.getCredential());
    }

    public String getStackName(EnvironmentDto environment) {
        return String.join("-", environment.getName(), String.valueOf(environment.getNetwork().getId()));
    }

    private boolean getPrivateSubnetEnabled(EnvironmentDto environmentDto) {
        return PrivateSubnetCreation.ENABLED == environmentDto.getNetwork().getPrivateSubnetCreation();
    }

    private Set<String> getSubNetCidrs(String networkCidr) {
        return extendedSubnetCidrProvider.provide(networkCidr);
    }

    private Optional<Boolean> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoPublicIp);
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }
}
