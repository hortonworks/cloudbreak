package com.sequenceiq.it.cloudbreak.newway.action.v4.kerberos;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;

public class KerberosCreateAction implements Action<KerberosTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosCreateAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto entity, CloudbreakClient client) throws Exception {
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
}
