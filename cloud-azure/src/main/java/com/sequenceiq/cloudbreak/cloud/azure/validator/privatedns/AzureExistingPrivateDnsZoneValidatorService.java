package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRegisteredPrivateDnsZoneServiceType;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class AzureExistingPrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZoneValidatorService.class);

    private static final String NONE_DNS_ZONE_ID = "None";

    @Inject
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    public ValidationResultBuilder validate(AzureClient azureClient, String networkResourceGroupName,
            String networkName, Map<AzurePrivateDnsZoneDescriptor, String> serviceToPrivateDnsZoneId, ValidationResultBuilder resultBuilder) {
        serviceToPrivateDnsZoneId.forEach((service, privateDnsZoneId) -> {
            try {
                if (AzureRegisteredPrivateDnsZoneServiceType.AKS.getResourceType().equals(service.getResourceType())
                        && privateDnsZoneId.equalsIgnoreCase(NONE_DNS_ZONE_ID)) {
                    LOGGER.info("AKS Private DNS Zone Id is set to None, skip validations");
                    return;
                }
                ResourceId privateDnsZoneResourceId = ResourceId.fromString(privateDnsZoneId);
                azurePrivateDnsZoneValidatorService.existingPrivateDnsZoneNameIsSupported(service, privateDnsZoneResourceId, resultBuilder);
                azurePrivateDnsZoneValidatorService.privateDnsZoneExists(azureClient, privateDnsZoneResourceId, resultBuilder);
                azurePrivateDnsZoneValidatorService.privateDnsZoneConnectedToNetwork(azureClient, networkResourceGroupName, networkName,
                        privateDnsZoneResourceId, resultBuilder);
            } catch (IllegalArgumentException e) {
                String validationMessage = String.format("The provided private DNS zone id %s for service %s is not a valid azure resource id.",
                        privateDnsZoneId, service.getResourceType());
                LOGGER.warn(validationMessage);
                resultBuilder.error(validationMessage);
            }
        });

        return resultBuilder;
    }
}