package com.sequenceiq.cloudbreak.service.freeipa;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeipaClientService freeipaClientService;

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public boolean checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = freeipaClientService.getByEnvironmentCrn(envCrn);
        if (freeipa == null || freeipa.getAvailabilityStatus() == null || freeipa.getAvailabilityStatus() == AvailabilityStatus.UNKNOWN) {
            String message = "Freeipa availability cannot be determined currently.";
            LOGGER.info(message);
            throw new CloudbreakServiceException(message);
        } else if (!freeipa.getAvailabilityStatus().isAvailable()) {
            String message = "Freeipa should be in Available state but currently is " + freeipa.getStatus().name();
            LOGGER.info(message);
            return false;
        } else {
            return true;
        }
    }

    @Recover
    public boolean recoverCheckFreeipaRunning(CloudbreakServiceException e, String envCrn) {
        String message = String.format("Freeipa availability trials exhausted for %s, defaulting to FreeIPA non-available", envCrn);
        LOGGER.warn(message, e);
        return false;
    }
}
