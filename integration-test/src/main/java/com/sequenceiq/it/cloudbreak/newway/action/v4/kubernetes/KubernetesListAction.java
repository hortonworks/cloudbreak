package com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;

public class KubernetesListAction implements Action<KubernetesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesListAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Collection<KubernetesV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .kubernetesV4Endpoint()
                .list(cloudbreakClient.getWorkspaceId(), null, Boolean.TRUE)
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Kubernetes listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
