package com.sequenceiq.environment.network.service;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.cloudbreak.converter.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Service
public class NetworkCreationRequestFactory {

    private final Map<String, SubnetCidrProvider> subnetCidrProviders;

    private final DefaultSubnetCidrProvider defaultSubnetCidrProvider;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final EnvironmentTagProvider environmentTagProvider;

    private final ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter;

    public NetworkCreationRequestFactory(Collection<SubnetCidrProvider> subnetCidrProviders,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            DefaultSubnetCidrProvider defaultSubnetCidrProvider,
            EnvironmentTagProvider environmentTagProvider,
            ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter) {
        this.subnetCidrProviders = subnetCidrProviders.stream().collect(Collectors.toMap(SubnetCidrProvider::cloudPlatform, s -> s));
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.defaultSubnetCidrProvider = defaultSubnetCidrProvider;
        this.environmentTagProvider = environmentTagProvider;
        this.serviceEndpointCreationToEndpointTypeConverter = serviceEndpointCreationToEndpointTypeConverter;
    }

    public NetworkCreationRequest create(EnvironmentDto environment) {
        NetworkDto networkDto = environment.getNetwork();

        boolean privateSubnetEnabled = getPrivateSubnetEnabled(environment);

        Cidrs cidrs = getSubNetCidrs(environment.getCloudPlatform(), networkDto.getNetworkCidr(), privateSubnetEnabled);

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
                .withEndpointType(serviceEndpointCreationToEndpointTypeConverter.convert(
                        networkDto.getServiceEndpointCreation(), environment.getCloudPlatform()))
                .withUserName(getUserFromCrn(environment.getCreator()))
                .withAccountId(environment.getAccountId())
                .withCreatorCrn(environment.getCreator())
                .withTags(environmentTagProvider.getTags(environment, environment.getNetwork().getResourceCrn()))
                .withPrivateSubnets(cidrs.getPrivateSubnets())
                .withPublicSubnets(cidrs.getPublicSubnets());
        getNoPublicIp(networkDto).ifPresent(builder::withNoPublicIp);
        getResourceGroupName(environment).ifPresent(builder::withResourceGroup);
        return builder.build();
    }

    public NetworkResourcesCreationRequest createProviderSpecificNetworkResources(EnvironmentDto environment, BaseNetwork baseNetwork) {
        NetworkDto networkDto = environment.getNetwork();
        NetworkResourcesCreationRequest.Builder builder = new NetworkResourcesCreationRequest.Builder()
                .withNetworkId(NullUtil.getIfNotNull(baseNetwork, BaseNetwork::getNetworkId))
                .withNetworkResourceGroup(NullUtil.getIfNotNull(baseNetwork, this::getNetworkResourceGroupName))
                .withExistingNetwork(NullUtil.getIfNotNull(baseNetwork, this::isExistingNetwork))
                .withCloudCredential(getCredential(environment))
                .withCloudContext(getCloudContext(environment))
                .withRegion(Region.region(environment.getLocation().getName()))
                .withPrivateEndpointsEnabled(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT == networkDto.getServiceEndpointCreation())
                .withTags(environmentTagProvider.getTags(environment, environment.getNetwork().getResourceCrn()));
                getResourceGroupName(environment).ifPresent(builder::withResourceGroup);
        return builder.build();
    }

    private boolean isExistingNetwork(BaseNetwork baseNetwork) {
        return baseNetwork.getRegistrationType() == RegistrationType.EXISTING;
    }

    private CloudCredential getCredential(EnvironmentDto environment) {
        return credentialToCloudCredentialConverter.convert(environment.getCredential());
    }

    public String getStackName(EnvironmentDto environment) {
        return String.join("-", environment.getName(), String.valueOf(environment.getNetwork().getId()));
    }

    private String getNetworkResourceGroupName(BaseNetwork baseNetwork) {
        return baseNetwork instanceof AzureNetwork ? ((AzureNetwork) baseNetwork).getResourceGroupName() : null;
    }

    private boolean getPrivateSubnetEnabled(EnvironmentDto environmentDto) {
        if (environmentDto.getCloudPlatform().equals(CloudPlatform.AZURE.name())) {
            // There is no such thing like private network in case of Azure
            return true;
        } else {
            return PrivateSubnetCreation.ENABLED == environmentDto.getNetwork().getPrivateSubnetCreation();
        }
    }

    private Cidrs getSubNetCidrs(String cloudPlatform, String networkCidr, boolean privateSubnetEnabled) {
        SubnetCidrProvider subnetCidrProvider = subnetCidrProviders.getOrDefault(cloudPlatform, defaultSubnetCidrProvider);
        return subnetCidrProvider.provide(networkCidr, privateSubnetEnabled);
    }

    private Optional<Boolean> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::isNoPublicIp);
    }

    private Optional<String> getResourceGroupName(EnvironmentDto environmentDto) {
        return Optional.of(environmentDto)
                .map(EnvironmentDto::getParameters)
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .map(AzureResourceGroupDto::getName);
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }

    private CloudContext getCloudContext(EnvironmentDto environment) {
        return CloudContext.Builder.builder()
                .withId(environment.getId())
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withPlatform(environment.getCloudPlatform())
                .withVariant(environment.getCloudPlatform())
                .withLocation(location(Region.region(environment.getLocation().getName())))
                .withUserName(environment.getCreator())
                .withAccountId(environment.getAccountId())
                .build();
    }
}
