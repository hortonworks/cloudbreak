package com.sequenceiq.it.cloudbreak.action.v4.kubernetes;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KubernetesListAction implements Action<KubernetesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesListAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Collection<KubernetesV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .kubernetesV4Endpoint()
                .list(cloudbreakClient.getWorkspaceId())
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        Log.whenJson(LOGGER, " Kubernetes listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
