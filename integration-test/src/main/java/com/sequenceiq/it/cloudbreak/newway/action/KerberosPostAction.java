package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class KerberosPostAction implements Action<KerberosEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosPostAction.class);

    @Override
    public KerberosEntity action(TestContext testContext, KerberosEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Kerberos post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .kerberosConfigV4Endpoint()
                        .create(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Kerberos created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        return entity;
    }

    public static KerberosPostAction create() {
        return new KerberosPostAction();
    }
}