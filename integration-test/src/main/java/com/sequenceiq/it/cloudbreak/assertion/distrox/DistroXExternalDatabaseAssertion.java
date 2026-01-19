package com.sequenceiq.it.cloudbreak.assertion.distrox;

import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.BaseMicroserviceClientDependentAssertion;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public final class DistroXExternalDatabaseAssertion extends BaseMicroserviceClientDependentAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXExternalDatabaseAssertion.class);

    private DistroXExternalDatabaseAssertion() {
    }

    public static Assertion<DistroXTestDto, CloudbreakClient> validateTemplateContainsExternalDatabaseHostname() {
        return (testContext, distroXTestDto, cloudbreakClient) -> {
            String blueprintName = distroXTestDto.getResponse().getCluster().getBlueprint().getName();
            String extendedBlueprint = distroXTestDto.getResponse().getCluster().getExtendedBlueprintText();
            String databaseServerCrn = distroXTestDto.getResponse().getCluster().getDatabaseServerCrn();
            RedbeamsClient redbeamsClient = getClient(testContext, testContext.getActingUser(), RedbeamsClient.class);
            DatabaseServerV4Response databaseServer = redbeamsClient.getDefaultClient(testContext)
                    .databaseServerV4Endpoint()
                    .getByCrn(databaseServerCrn);

            if (StringUtils.containsIgnoreCase(extendedBlueprint, databaseServer.getHost())) {
                LOGGER.info(format("Cluster template '%s' contains external database server host '%s'.", blueprintName, databaseServer.getHost()));
                Log.then(LOGGER, format(" Cluster template '%s' contains external database server host '%s'. ", blueprintName, databaseServer.getHost()));
            } else {
                LOGGER.error(format("Cluster template '%s' does not contain external database server host '%s'!", blueprintName, databaseServer.getHost()));
                throw new TestFailException(format("Cluster template '%s' does not contain external database server host '%s'!", blueprintName,
                        databaseServer.getHost()));
            }

            return distroXTestDto;
        };
    }
}
