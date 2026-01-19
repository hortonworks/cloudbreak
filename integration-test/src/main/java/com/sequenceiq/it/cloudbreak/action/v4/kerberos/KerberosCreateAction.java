package com.sequenceiq.it.cloudbreak.action.v4.kerberos;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class KerberosCreateAction implements Action<KerberosTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosCreateAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Kerberos post request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .getKerberosConfigV1Endpoint()
                        .create(testDto.getRequest()));
        Log.whenJson(LOGGER, format(" Kerberos created  successfully:%n"), testDto.getResponse());
        return testDto;
    }
}
