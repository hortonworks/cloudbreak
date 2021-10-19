package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@Service
public class CmProductChooserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmProductChooserService.class);

    /**
     * Using the CM parcel versions, will look for a matching product among the provided candidateProducts
     * A product is matching if the name and version both match.
     * @param activeParcels parcels installed on the CM server
     * @param candidateProducts A list of ClouderaManagerProducts extracted from image catalog
     * @return List of ClouderaManagerProducts installed on the DL / DH
     */
    Set<ClouderaManagerProduct> chooseParcelProduct(Set<ParcelInfo> activeParcels, Set<ClouderaManagerProduct> candidateProducts) {
        Set<ClouderaManagerProduct> foundProducts = activeParcels.stream()
                .map(ip -> findMatchingClouderaManagerProduct(candidateProducts, ip))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        LOGGER.debug("ClouderaManagerProducts found based on active parcels installed on the server: {}", foundProducts);
        return foundProducts;
    }

    /**
     * Using the installedCMVersion, will look for a matching clouderaManagerRepo.
     *
     * @param installedCmVersionOptional The version of the CM used for lookup
     * @param candidateCmVersions        The candidate cloudera manager repos
     * @return The found clouderaManagerRepo as an Optional, or empty
     */
    Optional<ClouderaManagerRepo> chooseCmRepo(Optional<String> installedCmVersionOptional, Set<ClouderaManagerRepo> candidateCmVersions) {
        if (installedCmVersionOptional.isEmpty()) {
            LOGGER.debug("No ClouderaManagerRepo found because the CM version retrieved from the server is empty.");
            return Optional.empty();
        } else {
            Optional<ClouderaManagerRepo> foundClouderaManagerRepo = candidateCmVersions.stream()
                    .filter(candidateCmVersion -> installedCmVersionOptional.get().equals(candidateCmVersion.getVersion()))
                    .findFirst();
            LOGGER.debug("ClouderaManagerRepo found based on the CM server version: {}", foundClouderaManagerRepo);
            return foundClouderaManagerRepo;
        }
    }

    private Optional<ClouderaManagerProduct> findMatchingClouderaManagerProduct(Set<ClouderaManagerProduct> candidateProducts, ParcelInfo ip) {
        List<ClouderaManagerProduct> matchingProducts = candidateProducts.stream()
                .filter(cp -> ip.getName().equals(cp.getName()))
                .filter(cp -> ip.getVersion().equals(cp.getVersion()))
                .collect(Collectors.toList());
        return matchingProducts.stream().findFirst();
    }

}