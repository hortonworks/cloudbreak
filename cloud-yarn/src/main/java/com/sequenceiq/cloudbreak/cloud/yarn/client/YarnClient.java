package com.sequenceiq.cloudbreak.cloud.yarn.client;

import java.net.MalformedURLException;

import com.sequenceiq.cloudbreak.cloud.yarn.client.exception.YarnClientException;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;

public interface YarnClient {
    ResponseContext createApplication(CreateApplicationRequest createApplicationRequest) throws MalformedURLException;

    void deleteApplication(DeleteApplicationRequest deleteApplicationRequest) throws YarnClientException, MalformedURLException;

    void validateApiEndpoint() throws YarnClientException, MalformedURLException;

    ResponseContext getApplicationDetail(ApplicationDetailRequest applicationDetailRequest) throws MalformedURLException;
}
