package com.sequenceiq.datalake.service;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    public void checkFreeipaRunning(String envCrn) {
        Optional<DescribeFreeIpaResponse> freeipa = describe(envCrn);
        if (freeipa.map(DescribeFreeIpaResponse::getAvailabilityStatus).isPresent()) {
            if (!freeipa.map(DescribeFreeIpaResponse::getAvailabilityStatus).get().isAvailable()) {
                String message = "Freeipa should be in Available state but currently is " + freeipa.get().getStatus().name();
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        } else {
            String message = "Freeipa availability cannot be determined currently.";
            LOGGER.warn(message);
            if (freeipa.isPresent()) {
                throw new ServiceUnavailableException(message);
            }
        }

    }

    private Optional<DescribeFreeIpaResponse> describe(String envCrn) {
        try {
            return Optional.ofNullable(freeIpaV1Endpoint.describe(envCrn));
        } catch (NotFoundException e) {
            LOGGER.error("Could not find freeipa with envCrn: " + envCrn, e);
            return Optional.empty();
        }
    }
}
