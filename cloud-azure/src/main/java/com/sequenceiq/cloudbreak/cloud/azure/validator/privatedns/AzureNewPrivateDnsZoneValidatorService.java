package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateEndpointServicesProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzureNewPrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNewPrivateDnsZoneValidatorService.class);

    @Inject
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    @Inject
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    public ValidationResult.ValidationResultBuilder zonesNotConnectedToNetwork(AzureClient azureClient, String networkId, String singleResourceGroupName,
            Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingDnsZones, ValidationResult.ValidationResultBuilder resultBuilder) {
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = azurePrivateEndpointServicesProvider.getEnabledPrivateEndpointServices();
        for (AzurePrivateDnsZoneServiceEnum service : enabledPrivateEndpointServices) {
            if (!servicesWithExistingDnsZones.contains(service)) {
                LOGGER.debug("Validating network that no private DNS zone with name {} is connected to it.", service.getDnsZoneName());
                azurePrivateDnsZoneValidatorService.privateDnsZonesNotConnectedToNetwork(azureClient, networkId, singleResourceGroupName,
                        service.getDnsZoneName(), resultBuilder);
            }
        }

        return resultBuilder;
    }

}
