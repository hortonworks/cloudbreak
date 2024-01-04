package com.sequenceiq.cloudbreak.cloud.yarn.client;

import java.net.MalformedURLException;

import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnEndpoint;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.exception.YarnClientException;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;

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
        WebResource webResource = getNewWebResource(dashEndpoint.getFullEndpointUrl().toString());
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, createApplicationRequest);
        responseContext.setStatusCode(response.getStatus());

            // Validate the results
        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_ACCEPTED) {
            responseContext.setResponseObject(response.getEntity(ApplicationDetailResponse.class));
        } else {
            responseContext.setResponseError(response.getEntity(ApplicationErrorResponse.class));
        }

        return responseContext;
    }

    @Override
    public void deleteApplication(DeleteApplicationRequest deleteApplicationRequest) throws YarnClientException, MalformedURLException {

        // Add the application name to the URL
        YarnEndpoint dashEndpoint = new YarnEndpoint(apiEndpoint,
                YarnResourceConstants.APPLICATIONS_PATH
                        + '/' + deleteApplicationRequest.getName());

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        // Delete the application
        WebResource webResource = client.resource(dashEndpoint.getFullEndpointUrl().toString());
        ClientResponse response = webResource.accept("application/json").type("application/json").delete(ClientResponse.class);

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
                        response.getEntity(String.class));
                LOGGER.debug(msg);
                throw new YarnClientException(msg);
        }

    }

    @Override
    public void validateApiEndpoint() throws YarnClientException, MalformedURLException {
        YarnEndpoint dashEndpoint = new YarnEndpoint(apiEndpoint, YarnResourceConstants.APPLICATIONS_PATH);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        WebResource webResource = client.resource(dashEndpoint.getFullEndpointUrl().toString());
        ClientResponse response = webResource.accept("application/json").type("application/json").get(ClientResponse.class);

        // Validate HTTP 200 status code
        if (response.getStatus() != YarnResourceConstants.HTTP_SUCCESS) {
            String msg = String.format("Received %d status code from url %s, reason: %s",
                    response.getStatus(),
                    dashEndpoint.getFullEndpointUrl().toString(),
                    response.getEntity(String.class));
            LOGGER.debug(msg);
            throw new YarnClientException(msg);
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
        WebResource webResource = getNewWebResource(dashEndpoint.getFullEndpointUrl().toString());
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        responseContext.setStatusCode(response.getStatus());

        // Validate the results
        if (checkStatusCode(response, YarnResourceConstants.HTTP_SUCCESS)) {
            responseContext.setResponseObject(response.getEntity(ApplicationDetailResponse.class));
        } else {
            responseContext.setResponseError(response.getEntity(ApplicationErrorResponse.class));
        }

        return responseContext;

    }

    public boolean checkStatusCode(ClientResponse response, int successStatusCode) {
        boolean success = false;
        if (successStatusCode == response.getStatus()) {
            success = true;
        }
        return success;
    }

    public WebResource getNewWebResource(String url) {
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter());
        return client.resource(url);
    }

    public WebResource getNewWebResourceWithClientConfig(ClientConfig clientConfig, String url) {
        Client client = Client.create(clientConfig);
        return client.resource(url);
    }
}
