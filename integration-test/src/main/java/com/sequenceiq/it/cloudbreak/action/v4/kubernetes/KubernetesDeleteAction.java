package com.sequenceiq.it.cloudbreak.action.v4.kubernetes;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KubernetesDeleteAction implements Action<KubernetesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDeleteAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, format("Kubernetes delete request: ", testDto.getName()));
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        Log.whenJson(LOGGER, " Kubernetes deleted successfully:\n", testDto.getResponse());
        return testDto;
    }
}
