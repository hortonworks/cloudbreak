package com.sequenceiq.cloudbreak.orchestrator.yarn.client;

import java.net.MalformedURLException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnEndpoint;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;

public class YarnHttpClient implements YarnClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnHttpClient.class);

    private final String apiEndpoint;

    public YarnHttpClient(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    @Override
    public ResponseContext createApplication(CreateApplicationRequest createApplicationRequest) throws
            MalformedURLException {
        YarnEndpoint dashEndpoint = new YarnEndpoint(apiEndpoint, YarnResourceConstants.APPLICATIONS_PATH);

        ResponseContext responseContext = new ResponseContext();
        // Construct the webresource and perform the get
        try (Client client = getLoggingClient()) {
            WebTarget webResource = client.target(dashEndpoint.getFullEndpointUrl().toString());
            ClientResponse response = webResource
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .post(Entity.entity(createApplicationRequest, MediaType.APPLICATION_JSON), ClientResponse.class);
            responseContext.setStatusCode(response.getStatus());

            // Validate the results
            if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_ACCEPTED) {
                responseContext.setResponseObject(response.readEntity(ApplicationDetailResponse.class));
            } else {
                responseContext.setResponseError(response.readEntity(ApplicationErrorResponse.class));
            }

            return responseContext;
        }
    }

    @Override
    public void deleteApplication(DeleteApplicationRequest deleteApplicationRequest) throws CloudbreakOrchestratorFailedException, MalformedURLException {

        // Add the application name to the URL
        YarnEndpoint dashEndpoint = new YarnEndpoint(apiEndpoint,
                YarnResourceConstants.APPLICATIONS_PATH
                        + '/' + deleteApplicationRequest.getName());

        try (Client client = getLoggingClient()) {
            // Delete the application
            WebTarget webResource = client.target(dashEndpoint.getFullEndpointUrl().toString());
            ClientResponse response = webResource
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .delete(ClientResponse.class);

            // Validate HTTP 204 return
            String msg;
            switch (response.getStatus()) {
                case YarnResourceConstants.HTTP_NO_CONTENT:
                    msg = String.format("Successfully deleted application %s", deleteApplicationRequest.getName());
                    LOGGER.debug(msg);
                    break;
                case YarnResourceConstants.HTTP_NOT_FOUND:
                    msg = String.format("Application %s not found, already deleted?", deleteApplicationRequest.getName());
                    LOGGER.debug(msg);
                    break;
                default:
                    msg = String.format("Received %d status code from url %s, reason: %s",
                            response.getStatus(),
                            dashEndpoint.getFullEndpointUrl().toString(),
                            response.readEntity(String.class));
                    LOGGER.debug(msg);
                    throw new CloudbreakOrchestratorFailedException(msg);
            }
        }
    }

    @Override
    public void validateApiEndpoint() throws CloudbreakOrchestratorFailedException, MalformedURLException {
        YarnEndpoint dashEndpoint = new YarnEndpoint(
                apiEndpoint,
                YarnResourceConstants.APPLICATIONS_PATH
                );

        try (Client client = getLoggingClient()) {
            WebTarget webResource = client.target(dashEndpoint.getFullEndpointUrl().toString());
            ClientResponse response = webResource
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);

            // Validate HTTP 200 status code
            if (response.getStatus() != YarnResourceConstants.HTTP_SUCCESS) {
                String msg = String.format("Received %d status code from url %s, reason: %s",
                        response.getStatus(),
                        dashEndpoint.getFullEndpointUrl().toString(),
                        response.readEntity(String.class));
                LOGGER.debug(msg);
                throw new CloudbreakOrchestratorFailedException(msg);
            }
        }
    }

    @Override
    public ResponseContext getApplicationDetail(ApplicationDetailRequest applicationDetailRequest)
            throws MalformedURLException {

        ResponseContext responseContext = new ResponseContext();

        // Add the application name to the URL
        YarnEndpoint dashEndpoint = new YarnEndpoint(apiEndpoint,
                YarnResourceConstants.APPLICATIONS_PATH
                        + '/' + applicationDetailRequest.getName());

        // Construct the webresource and perform the get
        try (Client client = getLoggingClient()) {
            WebTarget webResource = client.target(dashEndpoint.getFullEndpointUrl().toString());
            ClientResponse response = webResource
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);

            responseContext.setStatusCode(response.getStatus());

            // Validate the results
            if (checkStatusCode(response, YarnResourceConstants.HTTP_SUCCESS)) {
                responseContext.setResponseObject(response.readEntity(ApplicationDetailResponse.class));
            } else {
                responseContext.setResponseError(response.readEntity(ApplicationErrorResponse.class));
            }

            return responseContext;
        }
    }

    public boolean checkStatusCode(ClientResponse response, int successStatusCode) {
        return successStatusCode == response.getStatus();
    }

    private Client getLoggingClient() {
        return ClientBuilder.newClient().register(LoggingFeature.class);
    }
}
