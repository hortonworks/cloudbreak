package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.parcel.ParcelAvailabilityRetrievalService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@Service
public class OsChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsChangeService.class);

    private static final int HTTP_OK = 200;

    private static final String YUM_URL = "/yum/";

    @Inject
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public boolean isOsChangePermitted(Image targetImage, OsType currentOsType, Set<OsType> osUsedByInstances, String currentArchitecture,
            Map<String, String> stackRelatedParcels) {
        if (!currentOsType.getMajorOsTargets().contains(OsType.getByOs(targetImage.getOs())) || !allInstanceUseSameOs(currentOsType, osUsedByInstances)) {
            LOGGER.debug("OS change from {} to {} is not allowed. Supported target OS versions are: {}", currentOsType.getOs(), targetImage.getOs(),
                    currentOsType.getMajorOsTargets());
            return false;
        }
        try {
            Set<String> urlsForTargetOs = getUrlsForTargetOs(targetImage, stackRelatedParcels);
            Set<String> updatedUrlsForCurrentOs = urlsForTargetOs.stream()
                    .map(url -> updateUrl(targetImage.getOsType(), currentOsType, url, currentArchitecture))
                    .collect(Collectors.toSet());

            LOGGER.debug("Original URLs from target image {} replaced to the current OS {}", urlsForTargetOs, updatedUrlsForCurrentOs);
            Map<String, Integer> responsesByUrl = updatedUrlsForCurrentOs.stream()
                    .collect(Collectors.toMap(url -> url,
                    url -> parcelAvailabilityRetrievalService.getHeadResponseForParcel(url).getStatus()));

            boolean repoUrlsReachable = responsesByUrl.values().stream().allMatch(response -> response != null && response == HTTP_OK);
            if (!repoUrlsReachable) {
                LOGGER.debug("Response for URLs {}", responsesByUrl);
            }
            return repoUrlsReachable;
        } catch (Exception e) {
            LOGGER.warn("Failed to determine the possibility of the OS change for image: {}. Current OS: {}", targetImage, currentOsType, e);
            return false;
        }
    }

    public ClouderaManagerRepo updateCmRepoInCaseOfOsChange(ClouderaManagerRepo clouderaManagerRepo, OsType currentOsType, OsType targetOsType,
            String currentArchitecture) {
        if (currentOsType != null && currentOsType.getMajorOsTargets().contains(OsType.getByOs(targetOsType.getOs()))) {
            try {
                String updatedBaseUrl = updateUrl(targetOsType.getOsType(), currentOsType, clouderaManagerRepo.getBaseUrl(), currentArchitecture);
                String updateGpgKeyUrl = updateUrl(targetOsType.getOsType(), currentOsType, clouderaManagerRepo.getGpgKeyUrl(), currentArchitecture);
                LOGGER.debug("Updating Cloudera Manager repo with {} based URLs {}, {}", currentOsType.getOs(), updatedBaseUrl, updateGpgKeyUrl);
                return clouderaManagerRepo
                        .withBaseUrl(updatedBaseUrl)
                        .withGpgKeyUrl(updateGpgKeyUrl);
            } catch (Exception e) {
                String errorMessage = "Failed to update the CM repo URL with the current OS";
                LOGGER.error(errorMessage, e);
                throw new CloudbreakRuntimeException(errorMessage, e);
            }
        } else {
            return clouderaManagerRepo;
        }
    }

    public Set<ClouderaManagerProduct> updatePreWarmParcelUrlInCaseOfOsChange(Set<ClouderaManagerProduct> products, OsType currentOsType, OsType targetOsType,
            String currentArchitecture) {
        if (currentOsType != null && currentOsType.getMajorOsTargets().contains(OsType.getByOs(targetOsType.getOs()))) {
            try {
                return products.stream()
                        .map(product -> product.getParcel().contains(targetOsType.getOsType()) ?
                                product.withParcel(updateUrl(targetOsType.getOsType(), currentOsType, product.getParcel(), currentArchitecture)) : product)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                String errorMessage = "Failed to update the pre-warm parcel URL with the current OS";
                LOGGER.error(errorMessage, e);
                throw new CloudbreakRuntimeException(errorMessage, e);
            }
        } else {
            return products;
        }
    }

    private Set<String> getUrlsForTargetOs(Image targetImage, Map<String, String> stackRelatedParcels) {
        Set<String> targetPreWarmParcelUrls = getRequiredPreWarmParcelsToValidate(targetImage, stackRelatedParcels);
        Set<String> urlsForTargetOs = new HashSet<>(targetPreWarmParcelUrls);
        urlsForTargetOs.add(targetImage.getRepo().get(targetImage.getOsType()));
        return urlsForTargetOs;
    }

    private Set<String> getRequiredPreWarmParcelsToValidate(Image targetImage, Map<String, String> stackRelatedParcels) {
        if (!stackRelatedParcels.isEmpty()) {
            Set<ClouderaManagerProduct> clouderaManagerProducts = clouderaManagerProductTransformer.transform(targetImage, false, true);
            return clouderaManagerProducts.stream()
                    .filter(parcelFromImage -> stackRelatedParcels.keySet().stream()
                            .anyMatch(requiredParcelName -> parcelFromImage.getName().equalsIgnoreCase(requiredParcelName)))
                    .map(ClouderaManagerProduct::getParcel)
                    .map(url -> url.endsWith("/") ? url : url + "/")
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private String updateUrl(String targetOsType, OsType currentOsType, String targetUrl, String currentArchitecture) {
        String architecturePart = Architecture.ARM64.equals(Architecture.fromStringWithValidation(currentArchitecture)) ? currentArchitecture : "";
        String targetImageRepoPart = targetOsType + architecturePart + YUM_URL;
        if (targetUrl != null && targetUrl.contains(targetImageRepoPart)) {
            return targetUrl.replace(targetImageRepoPart, currentOsType.getOsType() + architecturePart + YUM_URL);
        } else {
            throw new CloudbreakRuntimeException(
                    String.format("Failed to update target repo URL because the %s part not found in the target repo URL %s", targetImageRepoPart,
                            targetUrl));
        }
    }

    private boolean allInstanceUseSameOs(OsType currentOsType, Set<OsType> usedOperatingSystems) {
        return usedOperatingSystems.size() == 1 && usedOperatingSystems.contains(currentOsType);
    }
}