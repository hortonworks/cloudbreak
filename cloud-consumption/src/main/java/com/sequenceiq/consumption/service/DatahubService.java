package com.sequenceiq.consumption.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackInstancesV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;

@Service
public class DatahubService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubService.class);

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public StackInstancesV4Responses getInstancesByCrn(String datahubCrn) {
        try {
            LOGGER.debug("Getting datahub instances by crn '{}'", datahubCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> cloudbreakInternalCrnClient.withInternalCrn().distroXInternalV1Endpoint().getInstancesByCrn(datahubCrn));
        } catch (WebApplicationException e) {
            LOGGER.error("Could not get datahub by crn '{}'. Reason: {}", datahubCrn, e.getMessage(), e);
            throw webApplicationExceptionHandler.handleException(e);
        }
    }
}
