package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Component
public class DistroXDatabaseServerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDatabaseServerAction.class);

    public StackDatabaseServerResponse getExternalDatabaseConfigs(String distroxCrn, CloudbreakClient cloudbreakClient, TestContext testContext)
            throws Exception {
        Log.when(LOGGER, "DistroX endpoint: %s" + cloudbreakClient.getDefaultClient(testContext).distroXDatabaseServerV1Endpoint());
        Log.whenJson(LOGGER, "Distrox crn: ", distroxCrn);

        StackDatabaseServerResponse stackDatabaseServerResponse = cloudbreakClient.getDefaultClient(testContext).distroXDatabaseServerV1Endpoint()
                .getDatabaseServerByCrn(distroxCrn);

        Log.whenJson(LOGGER, "Stack Database Server Response: ", stackDatabaseServerResponse);

        return stackDatabaseServerResponse;
    }
}
