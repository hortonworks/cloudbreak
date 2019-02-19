package com.sequenceiq.it.cloudbreak.newway.action.kubernetes;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kubernetes.KubernetesTestDto;

public class KubernetesTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesTestAction.class);

    private KubernetesTestAction() {

    }

    public static KubernetesTestDto createIfNotExist(TestContext testContext, KubernetesTestDto entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create Kubernetes with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().kubernetesV4Endpoint().post(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Kubernetes created successfully: ", entity.getRequest());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.info("Cannot create Kubernetes, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().kubernetesV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("Kubernetes could not be created.");
        }
        return entity;
    }

    public static KubernetesTestDto list(TestContext testContext, KubernetesTestDto entity, CloudbreakClient client) throws Exception {
        Collection<KubernetesV4Response> responses = client.getCloudbreakClient()
                .kubernetesV4Endpoint()
                .list(client.getWorkspaceId(), null, Boolean.TRUE)
                .getResponses();
        entity.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Kubernetes listed successfully:\n", entity.getResponses());
        return entity;
    }

    public static KubernetesTestDto delete(TestContext testContext, KubernetesTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Kubernetes delete request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Kubernetes deleted successfully:\n", entity.getResponse());
        return entity;
    }

    public static KubernetesTestDto create(TestContext testContext, KubernetesTestDto entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Kubernetes create request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Kubernetes created successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}
