package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AwsParams.AwsParamsBuilder;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.AzureParams.AzureParamsBuilder;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class NetworkTestUtils {

    private NetworkTestUtils() {
    }

    public static NetworkDto getNetworkDto(
            AzureParams azureParams, AwsParams awsParams, YarnParams yarnParams, String networkId, String networkCidr, Integer numberOfSubnets) {
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
                .build();
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
        return AwsParamsBuilder
                .anAwsParams()
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
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < numberOfSubnets; i++) {
            subnetMetas.put("key" + i, getCloudSubnet(
                    "eu-west-" + i + "a"));
        }
        return subnetMetas;
    }

    static CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
    }

    public static AzureParams getAzureParams(boolean noPublicIp, boolean withNetworkId, boolean withResourceGroupName) {
        AzureParamsBuilder azureParamsBuilder = AzureParamsBuilder
                .anAzureParams();
        if (withNetworkId) {
            azureParamsBuilder
                    .withNetworkId("aNetworkId");
        }
        if (withResourceGroupName) {
            azureParamsBuilder
                    .withResourceGroupName("aResourceGroupId");
        }
        return azureParamsBuilder
                .withNoPublicIp(noPublicIp)
                .build();
    }

    public static void checkErrorsPresent(ValidationResultBuilder resultBuilder, List<String> errorMessages) {
        ValidationResult validationResult = resultBuilder.build();
        assertEquals(errorMessages.size(), validationResult.getErrors().size(), validationResult.getFormattedErrors());
        List<String> actual = validationResult.getErrors();
        errorMessages.forEach(message -> assertTrue(actual.stream().anyMatch(item -> item.equals(message)), validationResult::getFormattedErrors));
    }

}
