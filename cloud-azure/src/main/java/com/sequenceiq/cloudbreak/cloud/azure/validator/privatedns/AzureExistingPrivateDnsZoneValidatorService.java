package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import java.security.InvalidParameterException;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.arm.resources.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzureExistingPrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZoneValidatorService.class);

    @Inject
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    public ValidationResult.ValidationResultBuilder validate(AzureClient azureClient, String networkResourceGroupName,
            String networkName, Map<AzurePrivateDnsZoneServiceEnum, String> serviceToPrivateDnsZoneId, ValidationResult.ValidationResultBuilder resultBuilder) {
        serviceToPrivateDnsZoneId.forEach((service, databasePrivateDnsZoneId) -> {
            try {
                ResourceId privateDnsZoneResourceId = ResourceId.fromString(databasePrivateDnsZoneId);
                azurePrivateDnsZoneValidatorService.existingPrivateDnsZoneNameIsSupported(service, privateDnsZoneResourceId, resultBuilder);
                azurePrivateDnsZoneValidatorService.privateDnsZoneExists(azureClient, privateDnsZoneResourceId, resultBuilder);
                azurePrivateDnsZoneValidatorService.privateDnsZoneConnectedToNetwork(azureClient, networkResourceGroupName, networkName,
                        privateDnsZoneResourceId, resultBuilder);
            } catch (InvalidParameterException e) {
                String validationMessage = String.format("The provided private DNS zone id %s for service %s is not a valid azure resource id.",
                        databasePrivateDnsZoneId, service.getResourceType());
                LOGGER.warn(validationMessage);
                resultBuilder.error(validationMessage);
            }
        });

        return resultBuilder;
    }
}