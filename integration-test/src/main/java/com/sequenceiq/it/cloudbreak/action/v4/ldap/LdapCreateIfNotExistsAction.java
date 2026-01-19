package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class LdapCreateIfNotExistsAction implements Action<LdapTestDto, FreeIpaClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapCreateIfNotExistsAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, " Ldap post request:\n", testDto.getRequest());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext).getLdapConfigV1Endpoint().create(testDto.getRequest())
            );
            Log.whenJson(LOGGER, "LdapConfig created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            Log.when(LOGGER, "Cannot create LdapConfig, fetch existed one: " + testDto.getRequest().getName());
            testDto.setResponse(
                    client.getDefaultClient(testContext).getLdapConfigV1Endpoint()
                            .describe(testDto.getRequest().getEnvironmentCrn()));
            Log.whenJson(LOGGER, "LdapConfig fetched successfully: ", testDto.getRequest());
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("LdapConfig could not be created.");
        }
        return testDto;
    }
}
