package com.sequenceiq.environment.environment.service.sdx;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    private final SdxEndpoint sdxEndpoint;

    public SdxService(SdxEndpoint sdxEndpoint) {
        this.sdxEndpoint = sdxEndpoint;
    }

    public SdxClusterResponse getByCrn(String clusterCrn) {
        try {
            return sdxEndpoint.getByCrn(clusterCrn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to get SDX cluster by crn '%s' due to '%s'.", clusterCrn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public List<SdxClusterResponse> list(String envName) {
        try {
            return sdxEndpoint.list(envName);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to list SDX clusters by environment name '%s' due to '%s'.", envName, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public void startByCrn(String crn) {
        try {
            sdxEndpoint.startByCrn(crn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to start SDX cluster by crn '%s' due to '%s'.", crn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public void stopByCrn(String crn) {
        try {
            sdxEndpoint.stopByCrn(crn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to stop SDX cluster by crn '%s' due to '%s'.", crn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

}
