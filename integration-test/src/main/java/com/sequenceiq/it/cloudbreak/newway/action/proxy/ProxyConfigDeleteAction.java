package com.sequenceiq.it.cloudbreak.newway.action.proxy;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;

public class ProxyConfigDeleteAction implements Action<ProxyConfigEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigDeleteAction.class);

    @Override
    public ProxyConfigEntity action(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Proxy config delete request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Proxy config was deleted successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        return entity;
    }

}