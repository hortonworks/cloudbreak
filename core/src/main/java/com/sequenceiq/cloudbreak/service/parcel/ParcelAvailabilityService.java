package com.sequenceiq.cloudbreak.service.parcel;

import static com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator.ARCHIVE_URL_PATTERN;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.validation.CmUrlProvider;
import com.sequenceiq.cloudbreak.service.upgrade.validation.ParcelUrlProvider;

@Service
public class ParcelAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelAvailabilityService.class);

    private static final String CM_REPOSITORY_METADATA_PATH = "repodata/repomd.xml";

    @Inject
    private ParcelUrlProvider parcelUrlProvider;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CmUrlProvider cmUrlProvider;

    @Inject
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    public Set<Response> validateAvailability(ClusterUpgradeProperties clusterUpgradeProperties, Long resourceId) {
        StackDto stackDto = stackDtoService.getById(resourceId);
        Set<String> requiredParcelUrlsFromImage = new HashSet<>(parcelUrlProvider.getRequiredParcelsFromImage(clusterUpgradeProperties, stackDto));
        String cmValidationUrl = getCmValidationUrl(clusterUpgradeProperties);
        if (cmValidationUrl != null) {
            requiredParcelUrlsFromImage.add(cmValidationUrl);
        }

        Map<String, Optional<Response>> parcelsByResponse = getParcelsByResponse(requiredParcelUrlsFromImage);
        Set<String> unavailableParcels = getUnavailableParcels(parcelsByResponse);

        if (unavailableParcels.isEmpty()) {
            LOGGER.debug("All required parcels are available on the image {}", clusterUpgradeProperties.getTargetImageId());
            return parcelsByResponse.entrySet().stream()
                    .filter(filterCmResource(cmValidationUrl).and(entry -> entry.getValue().isPresent()))
                    .map(entry -> entry.getValue().get())
                    .collect(Collectors.toSet());
        } else {
            LOGGER.debug("Parcels by response status: {}", parcelsByResponse);
            String errorMessage = buildErrorMessage(clusterUpgradeProperties, cmValidationUrl, unavailableParcels);
            LOGGER.error(errorMessage);
            throw new UpgradeValidationFailedException(errorMessage);
        }
    }

    private String getCmValidationUrl(ClusterUpgradeProperties properties) {
        ClouderaManagerRepo repo = properties.getClouderaManagerRepo();
        if (repo != null && StringUtils.isBlank(repo.getBuildNumber())) {
            String metadataUrl = StringUtils.appendIfMissing(repo.getBaseUrl(), "/") + CM_REPOSITORY_METADATA_PATH;
            LOGGER.info("CM build number was not specified; checking repository metadata at {}", metadataUrl);
            return metadataUrl;
        }
        return cmUrlProvider.getCmRpmUrl(properties);
    }

    private String buildErrorMessage(ClusterUpgradeProperties properties, String cmValidationUrl, Set<String> unavailableParcels) {
        StringBuilder errorMessageBuilder = new StringBuilder();
        if (unavailableParcels.contains(cmValidationUrl)) {
            ClouderaManagerRepo repo = properties.getClouderaManagerRepo();
            String resource = repo != null && StringUtils.isBlank(repo.getBuildNumber()) ? "repository metadata" : "RPM";
            errorMessageBuilder.append("Failed to access Cloudera Manager ").append(resource).append(": ").append(cmValidationUrl);
            unavailableParcels.remove(cmValidationUrl);
        }
        if (!unavailableParcels.isEmpty()) {
            errorMessageBuilder.append(" Failed to access the following parcels: ").append(unavailableParcels);
        }
        errorMessageBuilder.append(" Image ID: ").append(properties.getTargetImageId());
        return errorMessageBuilder.toString();
    }

    private Predicate<Map.Entry<String, Optional<Response>>> filterCmResource(String cmValidationUrl) {
        return entry -> !entry.getKey().equals(cmValidationUrl);
    }

    private Map<String, Optional<Response>> getParcelsByResponse(Set<String> requiredParcelsFromImage) {
        return requiredParcelsFromImage.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getHeadResponse
                ));
    }

    private Set<String> getUnavailableParcels(Map<String, Optional<Response>> parcelsByResponse) {
        return parcelsByResponse.entrySet().stream()
                .filter(entry -> isArchiveUrl(entry) && isNotAvailable(entry))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Optional<Response> getHeadResponse(String url) {
        try {
            return Optional.of(parcelAvailabilityRetrievalService.getHeadResponseForParcel(url));
        } catch (Exception e) {
            LOGGER.warn("Failed during HEAD request to url: {} Reason: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isArchiveUrl(Map.Entry<String, Optional<Response>> entry) {
        return ARCHIVE_URL_PATTERN.matcher(entry.getKey()).find();
    }

    private boolean isNotAvailable(Map.Entry<String, Optional<Response>> entry) {
        return entry.getValue().isEmpty() || entry.getValue().get().getStatus() != HttpStatus.OK.value();
    }

}
