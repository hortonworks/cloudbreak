package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class ParcelSizeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelSizeService.class);

    private static final long DIVIDER_TO_KB = 1024L;

    private static final long CM_PACKAGE_SIZE_IN_KB = 3145728L;

    public long getRequiredFreeSpace(Set<Response> parcelResponses) {
        long allParcelSizeInKb = getAllParcelSize(parcelResponses) / DIVIDER_TO_KB;
        LOGGER.debug("Size of the parcels {} Kb.", allParcelSizeInKb);
        return allParcelSizeInKb + CM_PACKAGE_SIZE_IN_KB;
    }

    private Long getAllParcelSize(Set<Response> parcelsResponses) {
        return parcelsResponses.stream()
                .map(response -> Long.parseLong(getParcelSize(response)))
                .reduce(1L, Math::addExact);
    }

    private String getParcelSize(Response response) {
        return Optional.ofNullable(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)).orElse("0");
    }
}
