package com.sequenceiq.it.cloudbreak.assertion.distrox;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.BaseMicroserviceClientDependentAssertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

import static org.assertj.core.api.Assertions.assertThat;

public final class DistroXExternalDatabaseAssertion extends BaseMicroserviceClientDependentAssertion {

    private DistroXExternalDatabaseAssertion() {
    }

    public static Assertion<DistroXTestDto, CloudbreakClient> validateTemplateContainsExternalDatabaseHostname() {
        return (testContext, distroXTestDto, cloudbreakClient) -> {
            String extendedBlueprint = distroXTestDto.getResponse().getCluster().getExtendedBlueprintText();
            String databaseServerCrn = distroXTestDto.getResponse().getCluster().getDatabaseServerCrn();
            RedbeamsClient redbeamsClient = getClient(testContext, testContext.getWho(RunningParameter.emptyRunningParameter()), RedbeamsClient.class);
            DatabaseServerV4Response databaseServer = redbeamsClient.getEndpoints()
                    .databaseServerV4Endpoint()
                    .getByCrn(databaseServerCrn);

            assertThat(extendedBlueprint)
                    .withFailMessage("Blueprint does not contain database server hostname")
                    .contains(databaseServer.getHost());

            return distroXTestDto;
        };
    }
}
