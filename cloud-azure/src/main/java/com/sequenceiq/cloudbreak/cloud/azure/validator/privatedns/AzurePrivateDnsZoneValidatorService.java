package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.azure.resourcemanager.privatedns.models.ProvisioningState.SUCCEEDED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandlerParameters;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class AzurePrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePrivateDnsZoneValidatorService.class);

    private static final AzureExceptionHandlerParameters AZURE_HANDLE_ALL_EXCEPTIONS = AzureExceptionHandlerParameters.builder()
            .withHandleAllExceptions(true)
            .build();

    @Inject
    private AzurePrivateDnsZoneMatcherService azurePrivateDnsZoneMatcherService;

    public ValidationResult.ValidationResultBuilder existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneDescriptor dnsZoneDescriptor,
            ResourceId existingPrivateDnsZoneResourceId, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!azurePrivateDnsZoneMatcherService.zoneNameMatchesPattern(dnsZoneDescriptor, existingPrivateDnsZoneResourceId.name())) {
            String validationMessage = String.format("The provided private DNS zone %s is not a valid DNS zone name for %s. Please use a DNS zone with " +
                    "name %s and try again.", existingPrivateDnsZoneResourceId.id(), dnsZoneDescriptor.getResourceType(),
                    dnsZoneDescriptor.getDnsZoneName(existingPrivateDnsZoneResourceId.resourceGroupName()));
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResultBuilder privateDnsZoneExists(AzureClient azureClient, ResourceId privateDnsZoneResourceId,
            ValidationResultBuilder resultBuilder) {
        boolean privateDnsZoneExists = azureClient.getPrivateDnsZonesByResourceGroup(privateDnsZoneResourceId.subscriptionId(),
                        privateDnsZoneResourceId.resourceGroupName())
                .getStream(AZURE_HANDLE_ALL_EXCEPTIONS)
                .anyMatch(pz -> pz.name().equals(privateDnsZoneResourceId.name()));
        if (!privateDnsZoneExists) {
            String validationMessage = String.format("The provided private DNS zone %s does not exist or you have no permission to access it. Please make " +
                    "sure the specified private DNS zone exists and try environment creation again.", privateDnsZoneResourceId.id());
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResult.ValidationResultBuilder privateDnsZoneConnectedToNetwork(AzureClient azureClient, String networkResourceGroupName,
            String networkName, ResourceId privateDnsZoneResourceId,
            ValidationResult.ValidationResultBuilder resultBuilder) {
        String networkId = azureClient.getNetworkByResourceGroup(networkResourceGroupName, networkName).id();
        List<String> connectedNetworks = azureClient.listNetworkLinksByPrivateDnsZoneName(
                        privateDnsZoneResourceId.subscriptionId(), privateDnsZoneResourceId.resourceGroupName(), privateDnsZoneResourceId.name())
                .getStream(AZURE_HANDLE_ALL_EXCEPTIONS)
                .map(vnl -> vnl.virtualNetwork().id()).toList();
        if (!connectedNetworks.contains(networkId)) {
            String validationMessage = String.format("The private DNS zone %s does not have a network link to network %s. Please make sure the private DNS " +
                    "zone is connected to the network provided to the environment.", privateDnsZoneResourceId.id(), networkName);
            addValidationError(validationMessage, resultBuilder);
        }
        return resultBuilder;
    }

    public ValidationResult privateDnsZonesNotConnectedToNetwork(AzureClient azureClient, String networkId, String resourceGroupName, String dnsZoneName,
            ValidationResultBuilder resultBuilder, List<PrivateDnsZone> privateDnsZoneList) {
        Optional<PrivateDnsZone> privateZoneWithNetworkLink =
                privateDnsZoneList.stream()
                        .filter(privateZone -> !privateZone.resourceGroupName().equalsIgnoreCase(resourceGroupName))
                        .filter(privateZone -> privateZone.name().equalsIgnoreCase(dnsZoneName))
                        .filter(privateZone -> privateZone.provisioningState().equals(SUCCEEDED))
                        .filter(privateZone ->
                                Objects.nonNull(azureClient.getNetworkLinkByPrivateDnsZone(privateZone.resourceGroupName(), dnsZoneName, networkId)))
                        .findFirst();
        if (privateZoneWithNetworkLink.isPresent()) {
            PrivateDnsZone privateZone = privateZoneWithNetworkLink.get();
            String validationMessage = String.format("Network link for the network %s already exists for Private DNS Zone %s in resource group %s. "
                            + "Please ensure that there is no existing network link and try again!",
                    networkId, dnsZoneName, privateZone.resourceGroupName());
            addValidationError(validationMessage, resultBuilder);
        }

        return resultBuilder.build();
    }

    private void addValidationError(String validationMessage, ValidationResultBuilder resultBuilder) {
        LOGGER.warn(validationMessage);
        resultBuilder.error(validationMessage);
    }

}
