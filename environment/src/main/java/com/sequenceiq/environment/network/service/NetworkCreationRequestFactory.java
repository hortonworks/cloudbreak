package com.sequenceiq.environment.network.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class NetworkCreationRequestFactory {

    private final Map<String, SubnetCidrProvider> subnetCidrProviders;

    private final DefaultSubnetCidrProvider defaultSubnetCidrProvider;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final CostTagging costTagging;

    public NetworkCreationRequestFactory(Collection<SubnetCidrProvider> subnetCidrProviders,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            CostTagging costTagging, DefaultSubnetCidrProvider defaultSubnetCidrProvider) {
        this.subnetCidrProviders = subnetCidrProviders.stream().collect(Collectors.toMap(SubnetCidrProvider::cloudPlatform, s -> s));
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.costTagging = costTagging;
        this.defaultSubnetCidrProvider = defaultSubnetCidrProvider;
    }

    public NetworkCreationRequest create(EnvironmentDto environment) {
        NetworkDto networkDto = environment.getNetwork();

        boolean privateSubnetEnabled = getPrivateSubnetEnabled(environment);

        Cidrs cidrs = getSubNetCidrs(environment.getCloudPlatform(), networkDto.getNetworkCidr(), privateSubnetEnabled);

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
                .withPrivateSubnetEnabled(privateSubnetEnabled)
                .withUserName(getUserFromCrn(environment.getCreator()))
                .withCreatorCrn(environment.getCreator())
                .withTags(costTagging.mergeTags(mergeRequest))
                .withPrivateSubnets(cidrs.getPrivateSubnets())
                .withPublicSubnets(cidrs.getPublicSubnets());
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

    private Cidrs getSubNetCidrs(String cloudPlatform, String networkCidr, boolean privateSubnetEnabled) {
        SubnetCidrProvider subnetCidrProvider = subnetCidrProviders.getOrDefault(cloudPlatform, defaultSubnetCidrProvider);
        return subnetCidrProvider.provide(networkCidr, privateSubnetEnabled);
    }

    private Optional<Boolean> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoPublicIp);
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }
}
