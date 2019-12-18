package com.sequenceiq.it.cloudbreak.action.v4.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KubernetesCreateIfNotExistAction implements Action<KubernetesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesCreateIfNotExistAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.whenJson(LOGGER, " Kubernetes create request: ", testDto.getRequest());
        try {
            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint().post(cloudbreakClient.getWorkspaceId(), testDto.getRequest())
            );
            Log.whenJson(LOGGER, "Kubernetes created successfully: ", testDto.getResponse());
        } catch (ProxyMethodInvocationException e) {
            Log.whenJson(LOGGER, "Cannot create Kubernetes, fetch existed one: ", testDto.getRequest());

            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint()
                            .get(cloudbreakClient.getWorkspaceId(), testDto.getRequest().getName()));
            Log.whenJson(LOGGER, "Kubernetes fetched successfully: ", testDto.getResponse());
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Kubernetes could not be created.");
        }
        return testDto;
    }
}
