package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.microsoft.azure.management.privatedns.v2018_09_01.ProvisioningState.SUCCEEDED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.management.privatedns.v2018_09_01.PrivateZone;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzurePrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePrivateDnsZoneValidatorService.class);

    public ValidationResult.ValidationResultBuilder existingPrivateDnsZoneNameIsSupported(
            AzurePrivateDnsZoneServiceEnum serviceEnum, String existingPrivateDnsZoneId, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!serviceEnum.getDnsZoneName().equals(ResourceId.fromString(existingPrivateDnsZoneId).name())) {
            String validationMessage = String.format("The provided private DNS zone %s is not a valid DNS zone name for %s. Please use a DNS zone with " +
                    "name %s and try again.", existingPrivateDnsZoneId, serviceEnum.getResourceType(), serviceEnum.getDnsZoneName());
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResult.ValidationResultBuilder privateDnsZoneExists(AzureClient azureClient, String privateDnsZoneId,
            ValidationResult.ValidationResultBuilder resultBuilder) {
        ResourceId privateDnsZoneResourceId = ResourceId.fromString(privateDnsZoneId);
        boolean privateDnsZoneExists = azureClient.getPrivateDnsZonesByResourceGroup(privateDnsZoneResourceId.subscriptionId(),
                        privateDnsZoneResourceId.resourceGroupName())
                .stream()
                .anyMatch(pz -> pz.name().equals(privateDnsZoneResourceId.name()));
        if (!privateDnsZoneExists) {
            String validationMessage = String.format("The provided private DNS zone %s does not exist. Please make sure the specified " +
                    "private DNS zone exists and try environment creation again.", privateDnsZoneId);
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResult.ValidationResultBuilder privateDnsZoneConnectedToNetwork(AzureClient azureClient, String networkResourceGroupName,
            String networkName, String privateDnsZoneId, ValidationResult.ValidationResultBuilder resultBuilder) {
        String networkId = azureClient.getNetworkByResourceGroup(networkResourceGroupName, networkName).id();
        ResourceId privateDnsZoneResId = ResourceId.fromString(privateDnsZoneId);
        PagedList<VirtualNetworkLinkInner> virtualNetworkLinks = azureClient.listNetworkLinksByPrivateDnsZoneName(privateDnsZoneResId.subscriptionId(),
                privateDnsZoneResId.resourceGroupName(), privateDnsZoneResId.name());
        List<String> connectedNetworks = virtualNetworkLinks.stream().map(vnl -> vnl.virtualNetwork().id()).collect(Collectors.toList());
        if (!connectedNetworks.contains(networkId)) {
            String validationMessage = String.format("The private DNS zone %s does not have a network link to network %s. Please make sure the private DNS " +
                    "zone is connected to the network provided to the environment.", privateDnsZoneId, networkName);
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResult privateDnsZonesNotConnectedToNetwork(AzureClient azureClient, String networkId, String resourceGroupName, String dnsZoneName,
            ValidationResult.ValidationResultBuilder resultBuilder) {
        PagedList<PrivateZone> privateDnsZoneList = azureClient.getPrivateDnsZoneList();
        Optional<PrivateZone> privateZoneWithNetworkLink = privateDnsZoneList.stream()
                .filter(privateZone -> !privateZone.resourceGroupName().equalsIgnoreCase(resourceGroupName))
                .filter(privateZone -> privateZone.name().equalsIgnoreCase(dnsZoneName))
                .filter(privateZone -> privateZone.provisioningState().equals(SUCCEEDED))
                .filter(privateZone -> Objects.nonNull(azureClient.getNetworkLinkByPrivateDnsZone(privateZone.resourceGroupName(), dnsZoneName, networkId)))
                .findFirst();
        if (privateZoneWithNetworkLink.isPresent()) {
            PrivateZone privateZone = privateZoneWithNetworkLink.get();
            String validationMessage = String.format("Network link for the network %s already exists for Private DNS Zone %s in resource group %s. "
                            + "Please ensure that there is no existing network link and try again!",
                    networkId, dnsZoneName, privateZone.resourceGroupName());
            addValidationError(validationMessage, resultBuilder);
        }

        return resultBuilder.build();
    }

    private void addValidationError(String validationMessage, ValidationResult.ValidationResultBuilder resultBuilder) {
        LOGGER.warn(validationMessage);
        resultBuilder.error(validationMessage);
    }

}