package com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;

public class KubernetesCreateIfNotExistAction implements Action<KubernetesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesCreateIfNotExistAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Create Kubernetes with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint().post(cloudbreakClient.getWorkspaceId(), testDto.getRequest())
            );
            logJSON(LOGGER, "Kubernetes created successfully: ", testDto.getRequest());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.info("Cannot create Kubernetes, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint()
                            .get(cloudbreakClient.getWorkspaceId(), testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Kubernetes could not be created.");
        }
        return testDto;
    }
}
