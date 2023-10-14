package com.sequenceiq.it.cloudbreak.testcase.load;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogDeleteAction;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.config.LoadTestProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;

public class PeriscopeLoadCleanUpTest extends AbstractMockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriscopeLoadCleanUpTest.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private LoadTestProperties loadTestProperties;

    @Override
    protected void setupTest(TestContext testContext) {
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Environments are already created",
            when = "cleanup is done",
            then = "Environments, catalog , credentials and blueprints should be deleted")
    public void testCleanUpClustersForLoadTesting(MockedTestContext testContext) {
        List<Future<String>> tasks = new ArrayList<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(loadTestProperties.getNumThreads());
        for (int tenantCounter = 1; tenantCounter <= loadTestProperties.getNumTenants(); tenantCounter++) {
            int tenantCounterFinal = tenantCounter;
            tasks.add(threadPool.submit(() -> {
                deleteTenantSetup(testContext, tenantCounterFinal, threadPool);
                return "";
            }));
        }
        waitingForFinishAllThread(tasks);
    }

    private void deleteTenantSetup(MockedTestContext testContext, int tenantIndex, ExecutorService threadPool) {
        String userCrn = PeriscopeLoadUtils.getUserCrn(tenantIndex);
        String accessKey = Base64.getEncoder().encodeToString(userCrn.getBytes(StandardCharsets.UTF_8));
        String secretKey = testContext.getTestParameter().get(CloudbreakTest.SECRET_KEY);
        CloudbreakUser user = new CloudbreakUser(accessKey, secretKey);
        user.setCrn(userCrn);
        testContext.as(user);
        String credentialName = PeriscopeLoadUtils.getCredentialName(tenantIndex);
        String catalogName = PeriscopeLoadUtils.getCatalogName(tenantIndex);
        String bluePrintName = PeriscopeLoadUtils.getBlueprintName(tenantIndex);
        List<Future<String>> tasks = new ArrayList<>();
        for (int envCounter = 1; envCounter <= loadTestProperties.getNumEnvironmentsPerTenant(); envCounter++) {
            String envName = PeriscopeLoadUtils.getEnvironmentName(tenantIndex, envCounter);
            tasks.add(threadPool.submit(() -> {
                testContext.given(envName, EnvironmentTestDto.class)
                        .withName(envName)
                        .when(environmentTestClient.describe(), RunningParameter.who(user))
                        .when(environmentTestClient.delete(), RunningParameter.who(user))
                        .await(EnvironmentStatus.ARCHIVED, RunningParameter.who(user))
                        .validate();
                return envName;
            }));
        }
        waitingForFinishAllThread(tasks);
        testContext.given(credentialName, CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.delete(), RunningParameter.who(user))
                .validate();
        testContext
                .given(catalogName, ImageCatalogTestDto.class)
                .withName(catalogName)
                .when(new ImageCatalogDeleteAction(), RunningParameter.who(user))
                .validate();
        testContext.given(bluePrintName, BlueprintTestDto.class)
                .withName(bluePrintName)
                .when(blueprintTestClient.deleteV4(), RunningParameter.who(user))
                .validate();
    }

    private void waitingForFinishAllThread(List<Future<String>> futures) {
        while (!futures.isEmpty()) {
            List<Future<String>> done = new ArrayList<>();
            for (Future<String> future : futures) {
                if (future.isDone()) {
                    done.add(future);
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new TestFailException("Test failed due to " + e.getMessage());
                    }
                }
            }
            futures.removeAll(done);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOGGER.info("Thread was interrupted while checking if tasks are completed");
            }
        }
    }
}
