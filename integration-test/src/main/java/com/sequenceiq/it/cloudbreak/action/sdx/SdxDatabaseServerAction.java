package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

@Component
public class SdxDatabaseServerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseServerAction.class);

    public StackDatabaseServerResponse getSdxExternalDatabaseConfigs(String sdxClusterCrn, SdxClient sdxClient) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + sdxClient.getDefaultClient().databaseServerEndpoint());
        Log.whenJson(LOGGER, " SDX crn: ", sdxClusterCrn);

        StackDatabaseServerResponse stackDatabaseServerResponse = sdxClient.getDefaultClient()
                .databaseServerEndpoint().getDatabaseServerByCrn(sdxClusterCrn);

        Log.whenJson(LOGGER, "Stack Database Server Response: ", stackDatabaseServerResponse);

        return stackDatabaseServerResponse;
    }
}
