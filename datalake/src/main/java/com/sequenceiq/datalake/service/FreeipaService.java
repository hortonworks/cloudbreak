package com.sequenceiq.datalake.service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    public DescribeFreeIpaResponse describe(String envCrn) {
        try {
            return freeIpaV1Endpoint.describe(envCrn);
        } catch (NotFoundException e) {
            LOGGER.info("Could not find freeipa with envCrn: " + envCrn);
        }
        return null;
    }
}
