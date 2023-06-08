package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;

@Component
public class CustomParcelFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomParcelFilterService.class);

    public Set<ParcelInfo> filterCustomParcels(Set<ParcelInfo> activeParcels, Set<ClouderaManagerProduct> availableProducts) {
        Set<ParcelInfo> customParcels = findCustomParcels(activeParcels, availableProducts);
        if (customParcels.isEmpty()) {
            return activeParcels;
        } else {
            LOGGER.debug("Found custom parcels during CM parcel sync: {}", customParcels);
            return activeParcels.stream()
                    .filter(parcelInfo -> !customParcels.contains(parcelInfo))
                    .collect(Collectors.toSet());
        }
    }

    private Set<ParcelInfo> findCustomParcels(Set<ParcelInfo> activeParcels, Set<ClouderaManagerProduct> candidateProducts) {
        Set<String> allParcelName = candidateProducts.stream().map(ClouderaManagerProduct::getName).collect(Collectors.toSet());
        return activeParcels.stream()
                .filter(parcel -> !allParcelName.contains(parcel.getName()))
                .collect(Collectors.toSet());
    }
}
