package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AzurePrivateEndpointServicesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePrivateEndpointServicesProvider.class);

    @Value("${cb.arm.privateendpoint.services:}")
    private List<String> privateEndpointServices;

    public List<AzurePrivateDnsZoneServiceEnum> getEnabledPrivateEndpointServices() {
        List<AzurePrivateDnsZoneServiceEnum> serviceEnumList = privateEndpointServices.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getBySubResource)
                .collect(Collectors.toList());
        LOGGER.debug("Enabled private endpoint services: {}", serviceEnumList);
        return serviceEnumList;
    }
}
