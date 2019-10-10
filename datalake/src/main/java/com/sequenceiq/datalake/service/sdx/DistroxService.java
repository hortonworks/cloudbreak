package com.sequenceiq.datalake.service.sdx;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Service
public class DistroxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxService.class);

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    public Collection<StackViewV4Response> getAttachedDistroXClusters(String environmentName, String environmentCrn) {
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environmentName);
        try {
            return distroXV1Endpoint.list(environmentName, environmentCrn).getResponses();
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DistroX clusters from Cloudbreak service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
    }
}
