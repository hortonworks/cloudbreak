package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CertificateSwapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

public class RedbeamsCertificateSwapAction implements Action<CertificateSwapTestDto, RedbeamsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsCertificateSwapAction.class);

    @Override
    public CertificateSwapTestDto action(TestContext testContext, CertificateSwapTestDto testDto, RedbeamsClient client) throws Exception {
        Log.whenJson(LOGGER, " Certificate swap request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .supportV4Endpoint().swapCertificate(testDto.getRequest()));
        Log.whenJson(LOGGER, " Certificate swap was successful:\n", testDto.getResponse());

        return testDto;
    }
}
