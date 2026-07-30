package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaMigrationToMultiAzAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMigrationToMultiAzAction.class);

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        FreeIpaMultiAzMigrationV1Request request = new FreeIpaMultiAzMigrationV1Request();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        request.setEnvironmentCrn(environmentCrn);
        Log.whenJson(LOGGER, format(" FreeIPA migration request:%n"), request);
        FreeIpaMultiAzMigrationV1Response response = client.getDefaultClient(testContext)
                .getFreeIpaMigrationV1Endpoint()
                .migrateToMultiAz(request);
        testDto.setOperationId(response.getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA migration started: %n"), response);
        LOGGER.info(" FreeIPA migration started for environment: {}, response: {}", environmentCrn, response);
        return testDto;
    }
}
