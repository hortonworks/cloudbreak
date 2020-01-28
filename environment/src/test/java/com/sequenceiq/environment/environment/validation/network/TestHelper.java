package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class TestHelper {

    public NetworkDto getNetworkDto(
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

    public void checkErrorsPresent(ValidationResult.ValidationResultBuilder resultBuilder, List<String> errorMessages) {
        ValidationResult validationResult = resultBuilder.build();
        assertEquals(errorMessages.size(), validationResult.getErrors().size(), validationResult.getFormattedErrors());
        List<String> actual = validationResult.getErrors();
        errorMessages.forEach(message -> assertTrue(actual.stream().anyMatch(item -> item.equals(message)), validationResult::getFormattedErrors));
    }
}
