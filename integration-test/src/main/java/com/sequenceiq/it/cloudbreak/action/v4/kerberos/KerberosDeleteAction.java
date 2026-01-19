package com.sequenceiq.it.cloudbreak.action.v4.kerberos;

import static java.lang.String.format;

import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class KerberosDeleteAction implements Action<KerberosTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosDeleteAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Kerberos config delete: %n"), testDto.getName());
        client.getDefaultClient(testContext)
                .getKerberosConfigV1Endpoint()
                .delete(testDto.getName());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext)
                            .getKerberosConfigV1Endpoint()
                            .describe(testDto.getName()));
        } catch (NotFoundException e) {
            Log.when(LOGGER, String.format(" Kerberos config was deleted successfully for environment %s", testDto.getName()));
        }
        return testDto;
    }
}
