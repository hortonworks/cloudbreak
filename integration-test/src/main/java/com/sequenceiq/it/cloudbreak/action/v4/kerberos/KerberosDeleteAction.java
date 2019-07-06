package com.sequenceiq.it.cloudbreak.action.v4.kerberos;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KerberosDeleteAction implements Action<KerberosTestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosDeleteAction.class);

    public KerberosTestDto action(TestContext testContext, KerberosTestDto testDto, FreeIPAClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Kerberos delete:%n"), testDto.getRequest());
        client.getFreeIpaClient()
                .getKerberosConfigV1Endpoint()
                .delete(testDto.getName());
        Log.log(LOGGER, String.format(" Kerberos config was deleted successfully for environment %s", testDto.getName()));
        return testDto;
    }
}
