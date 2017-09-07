package com.sequenceiq.cloudbreak.orchestrator.yarn.client;

import java.net.MalformedURLException;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;

public interface YarnClient {

    ResponseContext createApplication(
            CreateApplicationRequest createApplicationRequest)
            throws MalformedURLException;

    void deleteApplication(DeleteApplicationRequest deleteApplicationRequest)
            throws CloudbreakOrchestratorFailedException, MalformedURLException;

    void validateApiEndpoint() throws CloudbreakOrchestratorFailedException, MalformedURLException;

    ResponseContext getApplicationDetail(
            ApplicationDetailRequest applicationDetailRequest)
            throws MalformedURLException;
}
