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

import com.sequenceiq.authorization.service.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ParcelSizeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelSizeService.class);

    private static final long DIVIDER_TO_KB = 1024L;

    private static final int PARCEL_SIZE_MULTIPLIER = 2;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private ParcelUrlProvider parcelUrlProvider;

    long getAllParcelSize(String imageCatalogUrl, String imageCatalogName, String imageId, Stack stack) throws CloudbreakException {
        Map<String, Long> parcelsBySize = getParcelsBySize(imageCatalogUrl, imageCatalogName, imageId, stack);
        return getRequiredFreeSpace(parcelsBySize);
    }

    private Map<String, Long> getParcelsBySize(String imageCatalogUrl, String imageCatalogName, String imageId, Stack stack) throws CloudbreakException {
        Map<String, Long> result = new HashMap<>();
        Client client = restClientFactory.getOrCreateDefault();
        for (String parcelUrl : getParcelUrls(imageCatalogUrl, imageCatalogName, imageId, stack)) {
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

    private Set<String> getParcelUrls(String imageCatalogUrl, String imageCatalogName, String imageId, Stack stack) {
        return parcelUrlProvider.getRequiredParcelsFromImage(imageCatalogUrl, imageCatalogName, imageId, stack);
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

    private long getRequiredFreeSpace(Map<String, Long> parcelsBySize) {
        return (addParcelSize(parcelsBySize) / DIVIDER_TO_KB) * PARCEL_SIZE_MULTIPLIER;
    }

    private long addParcelSize(Map<String, Long> parcelsBySize) {
        return parcelsBySize.values()
                .stream()
                .filter(size -> !size.equals(0L))
                .reduce(1L, Math::addExact);
    }
}
