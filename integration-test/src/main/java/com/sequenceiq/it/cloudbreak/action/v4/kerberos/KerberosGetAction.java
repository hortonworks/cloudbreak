package com.sequenceiq.it.cloudbreak.action.v4.kerberos;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KerberosGetAction implements Action<KerberosTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosGetAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Kerberos get request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .kerberosConfigV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, format(" Kerberos get successfully:%n"), testDto.getResponse());
        Log.log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));
        return testDto;
    }
}
