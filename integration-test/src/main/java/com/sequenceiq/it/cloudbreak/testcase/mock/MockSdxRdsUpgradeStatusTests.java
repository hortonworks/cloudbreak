package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertTrue;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseUpgradeStatus;

public class MockSdxRdsUpgradeStatusTests extends AbstractMockTest {

    private static final Set<String> VALID_STATUSES = Set.of("UPGRADE_REQUIRED", "UPGRADE_NOT_REQUIRED", "UNKNOWN", "NO_DATALAKE");

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak with an available SDX cluster",
            when = "the RDS upgrade status is queried by datalake CRN, by name and by a single-element CRN list",
            then = "every endpoint returns a status that echoes the datalake CRN and carries a consistent, known upgrade status")
    public void testRdsUpgradeStatusEndpointsReturnConsistentResult(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then(this::verifyRdsUpgradeStatusEndpoints)
                .validate();
    }

    private SdxInternalTestDto verifyRdsUpgradeStatusEndpoints(TestContext tc, SdxInternalTestDto testDto, SdxClient client) {
        String datalakeCrn = testDto.getCrn();
        String datalakeName = testDto.getName();

        SdxDatabaseUpgradeStatus byCrn = client.getDefaultClient(tc).sdxUpgradeEndpoint()
                .getDatabaseServerUpgradeStatusByDatalakeCrn(datalakeCrn);
        assertEquals(byCrn.getDatalakeCrn(), datalakeCrn);
        assertTrue("Upgrade status returned by CRN must be one of " + VALID_STATUSES + " but was: " + byCrn.getUpgradeStatus(),
                VALID_STATUSES.contains(byCrn.getUpgradeStatus()));

        SdxDatabaseUpgradeStatus byName = client.getDefaultClient(tc).sdxUpgradeEndpoint()
                .getDatabaseServerUpgradeStatusByDatalakeName(datalakeName);
        assertEquals(byName.getDatalakeCrn(), datalakeCrn);
        assertEquals(byName.getUpgradeStatus(), byCrn.getUpgradeStatus());

        List<SdxDatabaseUpgradeStatus> byCrns = client.getDefaultClient(tc).sdxUpgradeEndpoint()
                .getDatabaseServerUpgradeStatusByDatalakeCrns(List.of(datalakeCrn));
        assertEquals(byCrns.size(), 1);
        assertEquals(byCrns.get(0).getDatalakeCrn(), datalakeCrn);
        assertEquals(byCrns.get(0).getUpgradeStatus(), byCrn.getUpgradeStatus());
        return testDto;
    }
}
