package com.sequenceiq.it.cloudbreak.assertion.distrox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.BaseMicroserviceClientDependentAssertion;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public final class DistroXExternalDatabaseAssertion extends BaseMicroserviceClientDependentAssertion {

    private DistroXExternalDatabaseAssertion() {
    }

    public static Assertion<DistroXTestDto, CloudbreakClient> validateTemplateContainsExternalDatabaseHostname() {
        return (testContext, distroXTestDto, cloudbreakClient) -> {
            String extendedBlueprint = distroXTestDto.getResponse().getCluster().getExtendedBlueprintText();
            String databaseServerCrn = distroXTestDto.getResponse().getCluster().getDatabaseServerCrn();
            RedbeamsClient redbeamsClient = getClient(testContext, testContext.getActingUser(), RedbeamsClient.class);
            DatabaseServerV4Response databaseServer = redbeamsClient.getDefaultClient()
                    .databaseServerV4Endpoint()
                    .getByCrn(databaseServerCrn);

            assertThat(extendedBlueprint)
                    .withFailMessage("Blueprint does not contain database server hostname")
                    .contains(databaseServer.getHost());

            return distroXTestDto;
        };
    }
}
