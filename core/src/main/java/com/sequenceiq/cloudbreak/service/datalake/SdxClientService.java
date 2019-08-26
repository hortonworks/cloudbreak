package com.sequenceiq.cloudbreak.service.datalake;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClientService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    public List<SdxClusterResponse> getByEnvironmentCrn(String crn) {
        try {
            return sdxEndpoint.getByEnvCrn(crn);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error(String.format("Failed to get datalake clusters for environment %s", crn), e);
            return new ArrayList<>();
        }
    }

    public List<SdxClusterResponse> list() {
        try {
            return sdxEndpoint.list(null);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error("Failed to list datalake clusters.", e);
            return new ArrayList<>();
        }
    }
}
