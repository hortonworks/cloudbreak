package com.sequenceiq.cloudbreak.orchestrator.yarn.handler;

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.yarn.poller.YarnApplicationPoller;
import com.sequenceiq.cloudbreak.orchestrator.yarn.util.ApplicationUtils;

@Service
public class ApplicationWaitHandler {

    @Inject
    private ApplicationUtils applicationUtils;

    /**
     * Wait for the appliation to start
     */
    public void waitForApplicationStart(OrchestrationCredential cred, ContainerConstraint constraint, int componentNumber)
            throws CloudbreakOrchestratorFailedException {
        String applicationName = applicationUtils.getApplicationName(constraint, componentNumber);
        try {
            YarnApplicationPoller yarnApplicationPoller = new YarnApplicationPoller();
            yarnApplicationPoller.waitForApplicationStart(applicationName, cred.getApiEndpoint());
        } catch (MalformedURLException e) {
            String msg = String.format("ERROR: URL is malformed: %s", cred.getApiEndpoint());
            throw new CloudbreakOrchestratorFailedException(msg, e);
        } catch (CloudbreakOrchestratorException e) {
            String msg = String.format("ERROR: Application %s did not start in the allotted time", applicationName);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }

}
