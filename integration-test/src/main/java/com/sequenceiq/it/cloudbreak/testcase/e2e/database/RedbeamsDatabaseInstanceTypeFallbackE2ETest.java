package com.sequenceiq.it.cloudbreak.testcase.e2e.database;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.ALL_ATTEMPTS;
import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.KEY;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.TestCrnGenerator;
import com.sequenceiq.it.cloudbreak.assertion.database.RedbeamsDatabaseTestAssertion;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag;
import com.sequenceiq.redbeams.api.model.common.Status;

/**
 * Exercises the database instance type fallback against a real cloud provider. Real capacity errors cannot be forced, so the
 * CDP_SIMULATE_DATABASE_CAPACITY_FAILURE entitlement together with the {@link SimulatedDatabaseCapacityFailureTag} database server tag makes the
 * provider layer report a capacity error for the first N instance types before it really deploys the next one.
 */
public class RedbeamsDatabaseInstanceTypeFallbackE2ETest extends AbstractE2ETest {

    private static final int MINIMUM_INSTANCE_TYPE_COUNT = 3;

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
        testContext
                .given(EnvironmentTestDto.class)
                .then(RedbeamsDatabaseTestAssertion.simulatedDatabaseCapacityFailureEntitled())
                .validate();
        createEnvironment(testContext, Boolean.FALSE, testContext.getCloudProvider().getFreeIpaDefaultNodeCount());
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an environment and the simulated database capacity failure entitlement is granted",
            when = "standalone database servers are created with a primary and two fallback instance types, simulating a capacity error for the "
                    + "first one, the first two and all instance types",
            then = "the database servers fall back to the first and to the second fallback instance type, "
                    + "and the creation fails with a clear error when every instance type is exhausted")
    public void testDatabaseInstanceTypeFallback(TestContext testContext) {
        List<String> instanceTypes = testContext.getCloudProvider().getDatabaseFallbackTestInstanceTypes();
        if (instanceTypes.size() < MINIMUM_INSTANCE_TYPE_COUNT) {
            throw new TestFailException(String.format("At least %d database fallback test instance types are required, but only %s are configured!",
                    MINIMUM_INSTANCE_TYPE_COUNT, instanceTypes));
        }
        String primaryInstanceType = instanceTypes.get(0);
        List<String> fallbackInstanceTypes = instanceTypes.subList(1, instanceTypes.size());
        String accountId = testContext.getActingUserCrn().getAccountId();

        String firstFallback = resourcePropertyProvider().getName();
        String secondFallback = resourcePropertyProvider().getName();
        String exhausted = resourcePropertyProvider().getName();

        createDatabaseServer(testContext, firstFallback, accountId, primaryInstanceType, fallbackInstanceTypes, "1");
        createDatabaseServer(testContext, secondFallback, accountId, primaryInstanceType, fallbackInstanceTypes, "2");
        createDatabaseServer(testContext, exhausted, accountId, primaryInstanceType, fallbackInstanceTypes, ALL_ATTEMPTS);
        testContext
                .given(firstFallback, RedbeamsDatabaseServerTestDto.class)
                .await(Status.AVAILABLE, key(firstFallback))
                .then(RedbeamsDatabaseTestAssertion.hasInstanceType(RedbeamsDatabaseServerTestDto::getCrn, instanceTypes.get(1)),
                        key(firstFallback))
                .given(secondFallback, RedbeamsDatabaseServerTestDto.class)
                .await(Status.AVAILABLE, key(secondFallback))
                .then(RedbeamsDatabaseTestAssertion.hasInstanceType(RedbeamsDatabaseServerTestDto::getCrn, instanceTypes.get(2)),
                        key(secondFallback))
                .given(exhausted, RedbeamsDatabaseServerTestDto.class)
                .await(Status.CREATE_FAILED, key(exhausted))
                .then(RedbeamsDatabaseTestAssertion.failedWithExhaustedInstanceTypeFallback(RedbeamsDatabaseServerTestDto::getCrn), key(exhausted))
                .given(firstFallback, RedbeamsDatabaseServerTestDto.class)
                .when(redbeamsDatabaseServerTestClient.delete(), key(firstFallback))
                .given(secondFallback, RedbeamsDatabaseServerTestDto.class)
                .when(redbeamsDatabaseServerTestClient.delete(), key(secondFallback))
                .given(exhausted, RedbeamsDatabaseServerTestDto.class)
                .when(redbeamsDatabaseServerTestClient.delete(), key(exhausted))
                .given(firstFallback, RedbeamsDatabaseServerTestDto.class)
                .await(Status.DELETE_COMPLETED, key(firstFallback))
                .given(secondFallback, RedbeamsDatabaseServerTestDto.class)
                .await(Status.DELETE_COMPLETED, key(secondFallback))
                .given(exhausted, RedbeamsDatabaseServerTestDto.class)
                .await(Status.DELETE_COMPLETED, key(exhausted))
                .validate();
    }

    private void createDatabaseServer(TestContext testContext, String name, String accountId, String primaryInstanceType,
            List<String> fallbackInstanceTypes, String simulatedCapacityFailures) {
        testContext
                .given(name, RedbeamsDatabaseServerTestDto.class)
                    .withName(name)
                    .withClusterCrn(TestCrnGenerator.getDatalakeCrn(UUID.randomUUID().toString(), accountId))
                    .withProviderDefaults()
                    .withInstanceType(primaryInstanceType)
                    .withFallbackInstanceTypes(fallbackInstanceTypes)
                    .withTags(Map.of(KEY, simulatedCapacityFailures))
                .when(redbeamsDatabaseServerTestClient.create(), key(name));
    }
}
