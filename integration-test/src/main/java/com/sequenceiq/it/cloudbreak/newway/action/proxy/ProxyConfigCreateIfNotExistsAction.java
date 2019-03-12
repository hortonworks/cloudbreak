package com.sequenceiq.it.cloudbreak.newway.action.proxy;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;

public class ProxyConfigCreateIfNotExistsAction implements Action<ProxyConfigEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigCreateIfNotExistsAction.class);

    @Override
    public ProxyConfigEntity action(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient client) {
        LOGGER.info("Create ProxyConfig with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().proxyConfigV4Endpoint().post(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "ProxyConfig created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create ProxyConfig, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().proxyConfigV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("ProxyConfig could not be created.");
        }
        return entity;
    }
}
