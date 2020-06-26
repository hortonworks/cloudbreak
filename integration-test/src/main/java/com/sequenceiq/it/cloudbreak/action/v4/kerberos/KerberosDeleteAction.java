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
        Log.whenJson(LOGGER, format(" Kerberos delete:%n"), testDto.getName());
        client.getFreeIpaClient()
                .getKerberosConfigV1Endpoint()
                .delete(testDto.getName());
        Log.when(LOGGER, String.format(" Kerberos config was deleted successfully for environment " + testDto.getName()));
        return testDto;
    }
}
