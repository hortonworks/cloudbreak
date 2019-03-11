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

public class KubernetesDeleteAction implements Action<KubernetesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDeleteAction.class);

    @Override
    public KubernetesTestDto action(TestContext testContext, KubernetesTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Kubernetes delete request:\n", entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Kubernetes deleted successfully:\n", entity.getResponse());
        return entity;
    }
}
