package com.sequenceiq.cloudbreak.orchestrator.yarn.poller;

import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.ONE_SECOND;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.RETRIES;

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
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

public class YarnApplicationPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnApplicationPoller.class);

    public void waitForApplicationStart(String appName, String apiEndpoint) throws CloudbreakOrchestratorException, MalformedURLException {

        YarnClient dashHttpClient = new YarnHttpClient(apiEndpoint);

        for (int i = 1; i <= RETRIES; i++) {
            // Make the request
            ResponseContext responseContext = dashHttpClient.getApplicationDetail(getApplicationDetailRequest(appName));

            // If 404, application isn't ready, sleep
            if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_NOT_FOUND) {
                handle404(appName, responseContext);
            }

            // If 200, check application state to ensure RUNNING
            if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
                if (null != responseContext.getResponseObject()) {
                    ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();

                    // Validate the application is "RUNNING"
                    if (!applicationDetailResponse.getState().equals(ApplicationState.READY.name())) {
                        LOGGER.debug(String.format("Application %s not ready, in %s state, sleeping 1000 ms", appName, applicationDetailResponse.getState()));
                        sleep();
                    } else {
                        // Validate the container is running
                        if (!applicationDetailResponse.getContainers().isEmpty()) {
                            Container appContainer = applicationDetailResponse.getContainers().get(0);
                            if (!appContainer.getState().equals(ContainerState.READY.name())) {
                                String msg = String.format("Application %s not ready, in %s state, sleeping 1000 ms",
                                        appName, applicationDetailResponse.getState());
                                LOGGER.debug(msg);
                                sleep();
                            } else {
                                String msg = String.format("Application %s has now successfully started, in %s state",
                                        appName, applicationDetailResponse.getState());
                                LOGGER.debug(msg);
                                break;
                            }
                        }
                    }
                }
            }

            if (i == RETRIES) {
                String msg = String.format("ERROR: %s did not start in %d retries", appName, RETRIES);
                throw new CloudbreakOrchestratorFailedException(msg);
            }

        }

    }

    private void handle404(String appName, ResponseContext responseContext) {
        String msg = String.format("Application %s not ready, received %d response, sleeping 1000 ms", appName, responseContext.getStatusCode());
        LOGGER.debug(msg);
        sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(ONE_SECOND);
        } catch (InterruptedException e) {

        }
    }

    private ApplicationDetailRequest getApplicationDetailRequest(String appName) {
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(appName);
        return applicationDetailRequest;
    }

}
