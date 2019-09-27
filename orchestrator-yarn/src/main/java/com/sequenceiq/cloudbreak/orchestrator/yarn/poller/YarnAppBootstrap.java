package com.sequenceiq.cloudbreak.orchestrator.yarn.poller;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.ApplicationState;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Container;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.ContainerState;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;

public class YarnAppBootstrap implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnAppBootstrap.class);

    private final String appName;

    private final String apiEndpoint;

    public YarnAppBootstrap(String appName, String apiEndpoint) {
        this.appName = appName;
        this.apiEndpoint = apiEndpoint;
    }

    @Override
    public Optional<Collection<String>> call() throws Exception {
        YarnClient dashHttpClient = new YarnHttpClient(apiEndpoint);

        // Make the request
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(appName);
        ResponseContext responseContext = dashHttpClient.getApplicationDetail(applicationDetailRequest);

        // If 404, application isn't ready, sleep
        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_NOT_FOUND) {
            String msg = String.format("Application %s not ready, received %d response, sleeping 1000 ms", appName, responseContext.getStatusCode());
            LOGGER.debug(msg);
            throw new CloudbreakOrchestratorFailedException(msg);
        }

        // If 200, check application state to ensure RUNNING
        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
            if (null != responseContext.getResponseObject()) {
                ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();

                // Validate the application is "RUNNING"
                if (!applicationDetailResponse.getState().equals(ApplicationState.READY.name())) {
                    LOGGER.debug(String.format("Application %s not ready, in %s state, sleeping 1000 ms", appName, applicationDetailResponse.getState()));
                    throw new CloudbreakOrchestratorFailedException(String.format("Application %s not ready, in %s state, sleeping 1000 ms", appName,
                            applicationDetailResponse.getState()));
                } else {
                    // Validate the container is running
                    if (!applicationDetailResponse.getContainers().isEmpty()) {
                        Container appContainer = applicationDetailResponse.getContainers().get(0);
                        if (!appContainer.getState().equals(ContainerState.READY.name())) {
                            String msg = String.format("Application %s not ready, in %s state, sleeping 1000 ms",
                                    appName, applicationDetailResponse.getState());
                            LOGGER.debug(msg);
                            throw new CloudbreakOrchestratorFailedException(msg);
                        } else {
                            String msg = String.format("Application %s has now successfully started, in %s state",
                                    appName, applicationDetailResponse.getState());
                            LOGGER.debug(msg);
                            return Optional.empty();
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
