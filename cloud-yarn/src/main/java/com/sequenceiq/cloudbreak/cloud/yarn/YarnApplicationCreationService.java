package com.sequenceiq.cloudbreak.cloud.yarn;

import java.net.MalformedURLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;

@Service
public class YarnApplicationCreationService {
    public static final String ARTIFACT_TYPE_DOCKER = "DOCKER";

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnApplicationCreationService.class);

    @Value("${cb.yarn.defaultQueue}")
    private String defaultQueue;

    /*
     * Default lifetime in seconds.
     */
    @Value("${cb.yarn.defaultLifeTime:}")
    private int defaultLifeTime;

    /**
     * Checks to see if the application described by applicationName already exists on the Yarn client.
     */
    public boolean checkApplicationAlreadyCreated(YarnClient yarnClient, String applicationName) throws MalformedURLException {
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(applicationName);
        boolean created = yarnClient.getApplicationDetail(applicationDetailRequest).getStatusCode() == YarnResourceConstants.HTTP_SUCCESS;
        LOGGER.debug("The yarn application " + applicationName + " already being created = " + created + ".");
        return created;
    }

    /**
     * Creates the application using the given application request by sending the request to the Yarn client, and ensuring that the request was
     * correctly received.
     */
    public void createApplication(YarnClient yarnClient, CreateApplicationRequest createApplicationRequest) throws MalformedURLException {
        LOGGER.info("Creating the Yarn application " + createApplicationRequest.getName() + ".");
        ResponseContext responseContext = yarnClient.createApplication(createApplicationRequest);
        if (Objects.nonNull(responseContext.getResponseError())) {
            LOGGER.warn("Received a response error from the Yarn client when trying to create the application " + createApplicationRequest.getName() + ".");
            ApplicationErrorResponse applicationErrorResponse = responseContext.getResponseError();
            throw new CloudConnectorException(String.format("Yarn Application creation error: HTTP Return: %d Error: %s", responseContext.getStatusCode(),
                    applicationErrorResponse.getDiagnostics()));
        }
    }

    /**
     * Returns a generic initial form for a Yarn {@link CreateApplicationRequest} which can be built upon to create
     * more complex and specialized requests.
     */
    public CreateApplicationRequest initializeRequest(CloudStack stack, String applicationName) {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(applicationName);
        createApplicationRequest.setQueue(stack.getParameters().getOrDefault(YarnConstants.YARN_QUEUE_PARAMETER, defaultQueue));
        String lifeTimeStr = stack.getParameters().get(YarnConstants.YARN_LIFETIME_PARAMETER);
        createApplicationRequest.setLifetime(lifeTimeStr != null ? Integer.parseInt(lifeTimeStr) : defaultLifeTime);
        LOGGER.debug("Created an initial application request for " + applicationName + ".");
        return createApplicationRequest;
    }
}
