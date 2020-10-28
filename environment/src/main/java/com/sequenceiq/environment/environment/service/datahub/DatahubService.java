package com.sequenceiq.environment.environment.service.datahub;

import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;

@Service
public class DatahubService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubService.class);

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DatahubService(DistroXV1Endpoint distroXV1Endpoint,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public StackViewV4Responses list(String environmentCrn) {
        try {
            return distroXV1Endpoint.list(null, environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to list Datahub clusters for environment '%s' due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public StackV4Response getByCrn(String crn, Set<String> entries) {
        try {
            return distroXV1Endpoint.getByCrn(crn, entries);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get Datahub cluster by crn %s due to: '%s'.", crn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void putStartByCrns(String environmentCrn, List<String> crns) {
        try {
            // we are not able to authorize distrox list start endpoint, this is the reason why we do it in a cycle
            for (String crn : crns) {
                distroXV1Endpoint.putStartByCrn(crn);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed start Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void putStopByCrns(String environmentCrn, List<String> crns) {
        try {
            // we are not able to authorize distrox list stop endpoint, this is the reason why we do it in a cycle
            for (String crn : crns) {
                distroXV1Endpoint.putStopByCrn(crn);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed stop Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void deleteMultiple(String environmentCrn, DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        try {
            // we are not able to authorize distrox list delete endpoint, this is the reason why we do it in a cycle
            for (String crn : multiDeleteRequest.getCrns()) {
                distroXV1Endpoint.deleteByCrn(crn, forced);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed delete multiple Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }
}
