package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaMultiAzMigrationTest extends AbstractFreeipaE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMultiAzMigrationTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak with a non MultiAz FreeIPA stack",
            when = "migration to MultiAz is requested",
            then = "the MultiAz stack should be available")
    public void testFreeIpaMultiAzMigration(TestContext testContext) {
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .when(environmentTestClient.create())
                .awaitForCreationFlow();
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.migrateToMultiAz())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.MIGRATE_TO_MULTI_AZ);
                    return testDto;
                })
                .validate();
    }
}
