package com.sequenceiq.datalake.service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    public void checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = describe(envCrn);
        if (freeipa != null && freeipa.getAvailabilityStatus() != null) {
            if (!freeipa.getAvailabilityStatus().isAvailable()) {
                String message = "Freeipa should be in Available state but currently is " + freeipa.getStatus().name();
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        } else {
            String message = "Freeipa availability cannot be determined currently.";
            LOGGER.warn(message);
            throw new ServiceUnavailableException(message);
        }

    }

    DescribeFreeIpaResponse describe(String envCrn) {
        try {
            return freeIpaV1Endpoint.describe(envCrn);
        } catch (NotFoundException e) {
            LOGGER.error("Could not find freeipa with envCrn: " + envCrn, e);
        }
        return null;
    }

    public long getNodeCount(String envCrn) {
        DescribeFreeIpaResponse response = null;
        try {
            response = freeIpaV1Endpoint.describe(envCrn);
        } catch (NotFoundException e) {
            LOGGER.error("Could not find freeipa with envCrn: " + envCrn, e);
        }
        if (response == null) {
            return 0;
        }
        return response.getInstanceGroups().stream().mapToLong(InstanceGroupBase::getNodeCount).sum();
    }
}
