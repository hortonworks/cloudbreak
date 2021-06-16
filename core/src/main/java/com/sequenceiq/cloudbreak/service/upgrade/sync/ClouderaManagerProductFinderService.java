package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@Service
public class ClouderaManagerProductFinderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerProductFinderService.class);

    /**
     * Using the CM parcel versions, will look for a matching component among the provided candidateProducts
     * A product is matching if the name and version both match.
     * @param installedParcels parcels installed on the CM server
     * @param candidateProducts A list of ClouderaManagerProducts extracted from image catalog
     * @return List of ClouderaManagerProducts installed on the DL / DH
     */
    public List<ClouderaManagerProduct> findInstalledProduct(List<ParcelInfo> installedParcels, List<ClouderaManagerProduct> candidateProducts) {
        List<ClouderaManagerProduct> foundProducts = installedParcels.stream()
                .map(ip -> {
                    List<ClouderaManagerProduct> matchingProducts = candidateProducts.stream()
                            .filter(cp -> ip.getName().equals(cp.getName()))
                            .filter(cp -> ip.getVersion().equals(cp.getVersion()))
                            .collect(Collectors.toList());
                    return matchingProducts.stream().findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        LOGGER.debug("ClouderaManagerProducts found based on active parcels installed on the server: {}", foundProducts);
        return foundProducts;
    }

}