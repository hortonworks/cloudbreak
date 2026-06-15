package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertTrue;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXDatabaseUpgradeStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRdsUpgradeStatusTests extends AbstractMockTest {

    private static final Set<String> VALID_STATUSES = Set.of("UPGRADE_REQUIRED", "UPGRADE_NOT_REQUIRED", "UNKNOWN", "NO_DATAHUB");

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak with an available DistroX cluster",
            when = "the RDS upgrade status is queried by datahub CRN, by name and by a single-element CRN list",
            then = "every endpoint returns a status that echoes the datahub CRN and carries a consistent, known upgrade status")
    public void testRdsUpgradeStatusEndpointsReturnConsistentResult(MockedTestContext testContext) {
        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(this::verifyRdsUpgradeStatusEndpoints)
                .validate();
    }

    private DistroXTestDto verifyRdsUpgradeStatusEndpoints(TestContext tc, DistroXTestDto testDto, CloudbreakClient client) {
        String datahubCrn = testDto.getCrn();
        String datahubName = testDto.getName();

        DistroXDatabaseUpgradeStatus byCrn = client.getDefaultClient(tc).distroXUpgradeV1Endpoint()
                .getDatabaseServerUpgradeRequiredByDatahubCrn(datahubCrn);
        assertEquals(byCrn.getDatahubCrn(), datahubCrn);
        assertTrue("Upgrade status returned by CRN must be one of " + VALID_STATUSES + " but was: " + byCrn.getUpgradeStatus(),
                VALID_STATUSES.contains(byCrn.getUpgradeStatus()));

        DistroXDatabaseUpgradeStatus byName = client.getDefaultClient(tc).distroXUpgradeV1Endpoint()
                .getDatabaseServerUpgradeRequiredByDatahubName(datahubName);
        assertEquals(byName.getDatahubCrn(), datahubCrn);
        assertEquals(byName.getUpgradeStatus(), byCrn.getUpgradeStatus());

        List<DistroXDatabaseUpgradeStatus> byCrns = client.getDefaultClient(tc).distroXUpgradeV1Endpoint()
                .getDatabaseServerUpgradeRequiredByDatahubCrns(List.of(datahubCrn));
        assertEquals(byCrns.size(), 1);
        assertEquals(byCrns.get(0).getDatahubCrn(), datahubCrn);
        assertEquals(byCrns.get(0).getUpgradeStatus(), byCrn.getUpgradeStatus());
        return testDto;
    }
}
