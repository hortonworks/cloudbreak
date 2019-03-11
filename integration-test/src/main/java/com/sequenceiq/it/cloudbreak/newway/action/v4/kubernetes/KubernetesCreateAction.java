package com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kubernetes.KubernetesTestDto;

public class KubernetesCreateAction implements Action<KubernetesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesCreateAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        logJSON(LOGGER, " Kubernetes create request:\n", entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .post(cloudbreakClient.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Kubernetes created successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}
