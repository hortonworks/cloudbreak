package com.sequenceiq.cloudbreak.cluster.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Service
public class CdhPatchUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdhPatchUpgradeService.class);

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public boolean isCdhPatchUpgrade(Set<ClouderaManagerProduct> products, ClusterApi connector, StackDto stackDto) throws Exception {
        Optional<ClouderaManagerProduct> cdhProductOpt = clouderaManagerProductsProvider.getCdhProduct(products);
        if (cdhProductOpt.isPresent()) {
            String cdhProductVersion = cdhProductOpt.get().getVersion();
            Optional<String> targetCdhStackVersion = CdhVersionProvider.getCdhStackVersionFromVersionString(cdhProductVersion);
            Optional<Integer> targetCdhPatchVersion = CdhVersionProvider.getCdhPatchVersionFromVersionString(cdhProductVersion);
            Optional<Integer> targetCdhBuildNumber = CdhVersionProvider.getCdhBuildNumberFromVersionString(cdhProductVersion);
            return isCdhPatchUpgrade(connector, stackDto.getName(), targetCdhStackVersion, targetCdhPatchVersion, targetCdhBuildNumber);
        } else {
            LOGGER.info("No CDH product found among the upgrade candidates, so no need for patch upgrade.");
            return false;
        }
    }

    private boolean isCdhPatchUpgrade(ClusterApi connector, String stackName, Optional<String> targetCdhStackVersion, Optional<Integer> targetCdhPatchVersion,
            Optional<Integer> targetCdhBuildNumber) throws Exception {
        Optional<String> currentCdhVersion = Optional.ofNullable(connector.getStackCdhVersion(stackName));
        Optional<String> currentCdhStackVersion = currentCdhVersion.flatMap(CdhVersionProvider::getCdhStackVersionFromVersionString);
        Optional<Integer> currentCdhPatchVersion = currentCdhVersion.flatMap(CdhVersionProvider::getCdhPatchVersionFromVersionString);
        Optional<Integer> currentCdhBuildNumber = currentCdhVersion.flatMap(CdhVersionProvider::getCdhBuildNumberFromVersionString);
        LOGGER.debug("Current CDH version on the cluster is: {}, target image CDH version is: {}", currentCdhVersion, targetCdhBuildNumber);
        return cdhStackVersionMatches(currentCdhStackVersion, targetCdhStackVersion)
                && cdhPatchVersionOrBuildNumberIsDifferent(currentCdhPatchVersion, targetCdhPatchVersion, currentCdhBuildNumber, targetCdhBuildNumber);
    }

    private boolean cdhStackVersionMatches(Optional<String> currentCdhStackVersion, Optional<String> targetCdhStackVersion) {
        return currentCdhStackVersion.isPresent() && currentCdhStackVersion.equals(targetCdhStackVersion);
    }

    private boolean cdhPatchVersionOrBuildNumberIsDifferent(Optional<Integer> currentCdhPatchVersion, Optional<Integer> targetCdhPatchVersion,
            Optional<Integer> currentCdhBuildNumber, Optional<Integer> targetCdhBuildNumber) {
        return Stream.of(currentCdhPatchVersion, targetCdhPatchVersion, currentCdhBuildNumber, targetCdhBuildNumber).allMatch(Optional::isPresent)
                && (!currentCdhPatchVersion.equals(targetCdhPatchVersion) || !currentCdhBuildNumber.equals(targetCdhBuildNumber));
    }

}
