package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
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

    public boolean isOsChangePermitted(Image targetImage, OsType currentOsType, Set<OsType> osUsedByInstances, String currentArchitecture) {
        if (!currentOsType.getMajorOsTargets().contains(OsType.getByOs(targetImage.getOs())) || !allInstanceUseSameOs(currentOsType, osUsedByInstances)) {
            LOGGER.debug("OS change from {} to {} is not allowed. Supported target OS versions are: {}", currentOsType.getOs(), targetImage.getOs(),
                    currentOsType.getMajorOsTargets());
            return false;
        }
        try {
            String targetCmRepoUrl = targetImage.getRepo().get(targetImage.getOsType());
            String cmRepoUrlForCurrentOs = updateCmRepoUrl(targetImage.getOsType(), currentOsType, targetCmRepoUrl, currentArchitecture);
            LOGGER.debug("Original CM url from target image {} replaced to the current OS {}", targetCmRepoUrl, cmRepoUrlForCurrentOs);
            Response response = parcelAvailabilityRetrievalService.getHeadResponseForParcel(cmRepoUrlForCurrentOs);
            boolean repoUrlReachable = response != null && response.getStatus() == HTTP_OK;
            if (!repoUrlReachable) {
                LOGGER.debug("Response for {} is {}", cmRepoUrlForCurrentOs, Optional.ofNullable(response).map(Response::getStatus).orElse(null));
            }
            return repoUrlReachable;
        } catch (Exception e) {
            LOGGER.warn("Failed to determine the possibility of the OS change for image: {}. Current OS: {}", targetImage, currentOsType, e);
            return false;
        }
    }

    public ClouderaManagerRepo updateCmRepoInCaseOfOsChange(ClouderaManagerRepo clouderaManagerRepo, OsType currentOsType, OsType targetOsType,
            String currentArchitecture) {
        if (currentOsType != null && currentOsType.getMajorOsTargets().contains(OsType.getByOs(targetOsType.getOs()))) {
            try {
                String updatedBaseUrl = updateCmRepoUrl(targetOsType.getOsType(), currentOsType, clouderaManagerRepo.getBaseUrl(), currentArchitecture);
                String updateGpgKeyUrl = updateCmRepoUrl(targetOsType.getOsType(), currentOsType, clouderaManagerRepo.getGpgKeyUrl(), currentArchitecture);
                LOGGER.debug("Updating Cloudera Manager repo with {} based URLs {}, {}", currentOsType.getOs(), updatedBaseUrl, updateGpgKeyUrl);
                return clouderaManagerRepo
                        .withBaseUrl(updatedBaseUrl)
                        .withGpgKeyUrl(updateGpgKeyUrl);
            } catch (Exception e) {
                LOGGER.warn("Failed to update the CM repo URL with the current OS", e);
                return clouderaManagerRepo;
            }
        } else {
            return clouderaManagerRepo;
        }
    }

    private String updateCmRepoUrl(String targetOsType, OsType currentOsType, String targetCmRepoUrl, String currentArchitecture) {
        String architecturePart = Architecture.ARM64.equals(Architecture.fromStringWithValidation(currentArchitecture)) ? currentArchitecture : "";
        String targetImageRepoPart = targetOsType + architecturePart + YUM_URL;
        if (targetCmRepoUrl != null && targetCmRepoUrl.contains(targetImageRepoPart)) {
            return targetCmRepoUrl.replace(targetImageRepoPart, currentOsType.getOsType() + architecturePart + YUM_URL);
        } else {
            throw new CloudbreakRuntimeException(
                    String.format("Failed to update CM repo URL because the %s part not found in the target repo URL %s", targetImageRepoPart,
                            targetCmRepoUrl));
        }
    }

    private boolean allInstanceUseSameOs(OsType currentOsType, Set<OsType> usedOperatingSystems) {
        return usedOperatingSystems.size() == 1 && usedOperatingSystems.contains(currentOsType);
    }
}