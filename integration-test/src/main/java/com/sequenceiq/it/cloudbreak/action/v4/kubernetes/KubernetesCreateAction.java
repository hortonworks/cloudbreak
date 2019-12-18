package com.sequenceiq.it.cloudbreak.action.v4.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KubernetesCreateAction implements Action<KubernetesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesCreateAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.whenJson(LOGGER, " Kubernetes create request:\n", testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .post(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
        Log.whenJson(LOGGER, " Kubernetes created successfully:\n", testDto.getResponse());

        return testDto;
    }
}
