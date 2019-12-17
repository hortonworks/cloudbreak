package com.sequenceiq.environment.environment.validation.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

public class TestHelper {

    public NetworkDto getNetworkDto(AzureParams azureParams, AwsParams awsParams, String networkId, String networkCidr, Integer numberOfSubnets) {
        return NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withAws(awsParams)
                .withNetworkCidr(networkCidr)
                .withNetworkId(networkId)
                .withSubnetMetas(getSubnetMetas(numberOfSubnets))
                .build();
    }

    Map<String, CloudSubnet> getSubnetMetas(Integer numberOfSubnets) {
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

    CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
    }

    public AzureParams getAzureParams(boolean noPublicIp, boolean withNetworkId, boolean withResourceGroupName) {
        AzureParams.AzureParamsBuilder azureParamsBuilder = AzureParams.AzureParamsBuilder
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

}
