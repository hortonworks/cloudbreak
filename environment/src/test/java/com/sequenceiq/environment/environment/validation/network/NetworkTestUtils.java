package com.sequenceiq.environment.environment.validation.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class NetworkTestUtils {

    private NetworkTestUtils() {
    }

    public static NetworkDto.Builder getNetworkDtoBuilder(AzureParams azureParams, AwsParams awsParams, YarnParams yarnParams, String networkId,
            String networkCidr, Integer numberOfSubnets, Integer numberOfLoadBalancerSubnets, RegistrationType registrationType) {
        return NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withAws(awsParams)
                .withYarn(yarnParams)
                .withNetworkCidr(networkCidr)
                .withNetworkId(networkId)
                .withSubnetMetas(getSubnetMetas(numberOfSubnets))
                .withEndpointGatewaySubnetMetas(getSubnetMetas(numberOfLoadBalancerSubnets))
                .withRegistrationType(registrationType);
    }

    public static NetworkDto getNetworkDto(AzureParams azureParams, AwsParams awsParams, YarnParams yarnParams, String networkId, String networkCidr,
            Integer numberOfSubnets, RegistrationType registrationType) {
        return getNetworkDtoBuilder(azureParams, awsParams, yarnParams, networkId, networkCidr, numberOfSubnets, 0, registrationType)
                .build();
    }

    public static NetworkDto getNetworkDto(AzureParams azureParams, AwsParams awsParams, YarnParams yarnParams, String networkId, String networkCidr,
            Integer numberOfSubnets, Integer numberOfLoadbalancerSubnets, RegistrationType registrationType) {
        return getNetworkDtoBuilder(azureParams, awsParams, yarnParams, networkId, networkCidr, numberOfSubnets, numberOfLoadbalancerSubnets, registrationType)
                .build();
    }

    public static NetworkDto getNetworkDto(
            AzureParams azureParams, AwsParams awsParams, YarnParams yarnParams, String networkId, String networkCidr, Integer numberOfSubnets) {
        return getNetworkDto(azureParams, awsParams, yarnParams, networkId, networkCidr, numberOfSubnets, RegistrationType.EXISTING);
    }

    public static Set<CloudNetwork> getCloudNetworks(int quantity) {
        Set<CloudNetwork> cloudNetworks = new LinkedHashSet<>(quantity);
        for (int i = 0; i < quantity; i++) {
            CloudNetwork cloudNetwork = new CloudNetwork("cloud-network-" + i, Integer.toString(i),
                    new HashSet<>(getSubnetMetas(1).values()), new LinkedHashMap<>());
            cloudNetworks.add(cloudNetwork);
        }
        return cloudNetworks;
    }

    public static AwsParams getAwsParams() {
        return getAwsParams("someVpcId");
    }

    public static AwsParams getAwsParams(String vpcId) {
        return AwsParams
                .builder()
                .withVpcId(vpcId)
                .build();
    }

    public static AzureParams getAzureParams() {
        return getAzureParams(true, true, true);
    }

    static Map<String, CloudSubnet> getSubnetMetas(Integer numberOfSubnets) {
        if (numberOfSubnets == null) {
            return null;
        }
        Map<String, CloudSubnet> subnetMetas = new HashMap<>(numberOfSubnets);
        for (int i = 0; i < numberOfSubnets; i++) {
            subnetMetas.put("key" + i, getCloudSubnet(
                    "eu-west-" + i + "a"));
        }
        return subnetMetas;
    }

    public static CloudSubnet getCloudSubnet(String availabilityZone) {
        return getCloudSubnet(availabilityZone, "eu-west-1");
    }

    public static CloudSubnet getCloudSubnet(String availabilityZone, String subnetId) {
        return new CloudSubnet.Builder()
                .id(subnetId)
                .name(subnetId)
                .availabilityZone(availabilityZone)
                .cidr("cidr")
                .build();
    }

    public static AzureParams getAzureParams(boolean noPublicIp, boolean withNetworkId, boolean withResourceGroupName) {
        AzureParams.Builder builder = AzureParams.builder();
        if (withNetworkId) {
            builder.withNetworkId("aNetworkId");
        }
        if (withResourceGroupName) {
            builder.withResourceGroupName("aResourceGroupId");
        }
        return builder
                .withNoPublicIp(noPublicIp)
                .build();
    }

    public static void checkErrorsPresent(ValidationResultBuilder resultBuilder, List<String> errorMessages) {
        ValidationResult validationResult = resultBuilder.build();
        assertEquals(errorMessages.size(), validationResult.getErrors().size(), validationResult.getFormattedErrors());
        List<String> actual = validationResult.getErrors();
        errorMessages.forEach(message -> assertThat(actual).contains(message));
    }

}
