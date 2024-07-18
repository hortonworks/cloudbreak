package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateEndpointServicesProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzureNewPrivateDnsZoneValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNewPrivateDnsZoneValidatorService.class);

    @Inject
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    @Inject
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    public ValidationResult.ValidationResultBuilder zonesNotConnectedToNetwork(
            AzureClient azureClient, String networkId, String singleResourceGroupName,
            Set<AzureManagedPrivateDnsZoneServiceType> servicesWithExistingDnsZones,
            PrivateDatabaseVariant privateDatabaseVariant, ValidationResult.ValidationResultBuilder resultBuilder) {
        List<AzureManagedPrivateDnsZoneServiceType> cdpManagedPrivateEndpointServices = azurePrivateEndpointServicesProvider
                .getCdpManagedDnsZoneServices(servicesWithExistingDnsZones, privateDatabaseVariant);
        if (cdpManagedPrivateEndpointServices.isEmpty()) {
            LOGGER.debug("There are no private DNS zone services that CDP would manage on its own, skipping checking if DNS zones are already connected " +
                    "to the network");
            return resultBuilder;
        }

        List<PrivateDnsZone> privateDnsZoneList = azureClient.getPrivateDnsZoneList().getAll();
        for (AzureManagedPrivateDnsZoneServiceType service : cdpManagedPrivateEndpointServices) {
            LOGGER.debug("Validating network that no private DNS zone with name {} is connected to it.", service.getDnsZoneName(singleResourceGroupName));
            azurePrivateDnsZoneValidatorService.privateDnsZonesNotConnectedToNetwork(azureClient, networkId, singleResourceGroupName,
                    service.getDnsZoneName(singleResourceGroupName), resultBuilder, privateDnsZoneList);
        }

        return resultBuilder;
    }

}