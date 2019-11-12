package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeIpaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaService.class);

    private static final int MAX_ATTEMPT = 5;

    private static final int DELAY = 5000;

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = MAX_ATTEMPT, backoff = @Backoff(delay = DELAY))
    public Boolean isFreeIpaEnabled(String environmentCrn) {
        return describeFreeIpa(environmentCrn).isPresent();
    }

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = MAX_ATTEMPT, backoff = @Backoff(delay = DELAY))
    public String getFreeIpaDomain(String environmentCrn) {
        Optional<DescribeFreeIpaResponse> resp = describeFreeIpa(environmentCrn);
        String domain = null;
        if (resp.isPresent()) {
            domain = resp.get().getFreeIpa().getDomain();
        }
        return domain;
    }

    private Optional<DescribeFreeIpaResponse> describeFreeIpa(String environmentCrn) {
        Optional<DescribeFreeIpaResponse> resp;
        try {
            resp = Optional.ofNullable(freeIpaV1Endpoint.describe(environmentCrn));
        } catch (NotFoundException | ForbiddenException notFoundEx) {
            LOGGER.debug("No FreeIpa config found for {} environment.", environmentCrn);
            resp = Optional.empty();
        }
        return resp;
    }
}
