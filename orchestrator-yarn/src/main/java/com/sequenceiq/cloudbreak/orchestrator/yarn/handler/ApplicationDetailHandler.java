package com.sequenceiq.cloudbreak.orchestrator.yarn.handler;

import java.net.MalformedURLException;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;
import com.sequenceiq.cloudbreak.orchestrator.yarn.util.ApplicationUtils;

@Service
public class ApplicationDetailHandler {

    @Inject
    private ApplicationUtils applicationUtils;

    public ContainerInfo getContainerInfo(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint, int componentNumber)
            throws CloudbreakOrchestratorFailedException {
        String applicationName = applicationUtils.getApplicationName(constraint, componentNumber);

        // Build the ApplicationDetailRequest
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(applicationName);

        try {
            // Validate that the app exists
            YarnClient yarnHttpClient = new YarnHttpClient(cred.getApiEndpoint());
            ResponseContext appDetailResponseContext = yarnHttpClient.getApplicationDetail(applicationDetailRequest);
            if (appDetailResponseContext.getResponseError() != null) {
                ApplicationErrorResponse applicationErrorResponse = appDetailResponseContext.getResponseError();
                throw new CloudbreakOrchestratorFailedException(String.format("ERROR: HTTP Return: %d Error: %s.", appDetailResponseContext.getStatusCode(),
                        applicationErrorResponse.getDiagnostics()));
            }
            // Return the details
            String componentHostName = applicationUtils.getComponentHostName(constraint, cred, componentNumber);
            String image = String.format("%s:%s", config.getName(), config.getVersion());
            return new ContainerInfo(applicationName, applicationName, componentHostName, image);
        } catch (MalformedURLException e) {
            String msg = String.format("ERROR: URL is malformed: %s", cred.getApiEndpoint());
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }
}
