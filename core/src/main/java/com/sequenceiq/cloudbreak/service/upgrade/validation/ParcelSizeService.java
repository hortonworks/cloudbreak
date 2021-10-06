package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
class ParcelSizeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelSizeService.class);

    private static final long DIVIDER_TO_KB = 1024L;

    private static final long CM_PACKAGE_SIZE_IN_KB = 3145728L;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private ParcelUrlProvider parcelUrlProvider;

    long getRequiredFreeSpace(StatedImage targetImage, Stack stack) throws CloudbreakException {
        Map<String, Long> parcelsBySize = getParcelsBySize(targetImage, stack);
        return getAllParcelSize(parcelsBySize) + CM_PACKAGE_SIZE_IN_KB;
    }

    private Map<String, Long> getParcelsBySize(StatedImage targetImage, Stack stack) throws CloudbreakException {
        Map<String, Long> result = new HashMap<>();
        Client client = restClientFactory.getOrCreateDefault();
        for (String parcelUrl : getParcelUrls(targetImage, stack)) {
            try {
                WebTarget target = client.target(parcelUrl);
                paywallCredentialPopulator.populateWebTarget(parcelUrl, target);
                long parcelSize = Long.parseLong(target.request().head().getHeaderString("Content-Length"));
                result.put(parcelUrl, parcelSize);
            } catch (Exception e) {
                LOGGER.warn("Could not get the size of the parcel: {} Reason: {}", parcelUrl, e.getMessage());
                result.put(parcelUrl, 0L);
            }
        }
        validateParcelSizes(result);
        return result;
    }

    private Set<String> getParcelUrls(StatedImage targetImage, Stack stack) {
        Set<String> requiredParcelsFromImage = parcelUrlProvider.getRequiredParcelsFromImage(targetImage, stack);
        LOGGER.debug("The required parcel URLs {} from image {}", requiredParcelsFromImage, targetImage.getImage().getUuid());
        return requiredParcelsFromImage;
    }

    private void validateParcelSizes(Map<String, Long> parcelsBySize) throws CloudbreakException {
        Set<String> unknownParcels = getUnknownParcels(parcelsBySize);
        if (!unknownParcels.isEmpty()) {
            throw new CloudbreakException(
                    String.format("Failed to validate the required free space for upgrade because we're unable to get the size of the following parcels: %s",
                            unknownParcels));
        }
    }

    private Set<String> getUnknownParcels(Map<String, Long> parcelsBySize) {
        return parcelsBySize.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == null || entry.getValue() == 0 || entry.getValue() == -1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private long getAllParcelSize(Map<String, Long> parcelsBySize) {
        return addParcelSize(parcelsBySize) / DIVIDER_TO_KB;
    }

    private long addParcelSize(Map<String, Long> parcelsBySize) {
        return parcelsBySize.values()
                .stream()
                .filter(size -> !size.equals(0L))
                .reduce(1L, Math::addExact);
    }
}
