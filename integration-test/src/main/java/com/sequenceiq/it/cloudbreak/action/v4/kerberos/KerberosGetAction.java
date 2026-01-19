package com.sequenceiq.it.cloudbreak.action.v4.kerberos;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class KerberosGetAction implements Action<KerberosTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosGetAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" Kerberos get request by env crn:%n", testDto.getName()));
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .getKerberosConfigV1Endpoint()
                        .describe(testDto.getName()));
        Log.whenJson(LOGGER, format(" Kerberos get successfully:%n"), testDto.getResponse());
        return testDto;
    }
}
