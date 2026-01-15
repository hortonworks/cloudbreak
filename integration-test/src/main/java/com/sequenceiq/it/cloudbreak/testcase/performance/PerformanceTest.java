package com.sequenceiq.it.cloudbreak.testcase.performance;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class PerformanceTest extends AbstractMockTest {

    private static final Logger LOGGER = getLogger(PerformanceTest.class);

    private static final int ENV_COUNT = 5;

    private static final int DATAHUB_COUNT = 10;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX with Cloudera Manager is created",
            then = "the cluster should be available")
    public void performanceTests(MockedTestContext testContext) throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(100);
        long start = System.currentTimeMillis();
        String baseName = "perfenv" + Math.round(Math.random() * 10000000);

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < ENV_COUNT; i++) {
            String envName = baseName + "-" + i;
            futures.add(executeEnvironmentCreation(testContext, envName, threadPool));
        }
        for (Future<String> future : futures) {
            String envName = future.get();
            executeDhCreationWhenDatalakeReady(testContext, envName, threadPool);
        }
        List<Future<String>> dhWaitingFutures = new ArrayList<>();
        for (int i = 0; i < ENV_COUNT; i++) {
            for (int dhi = 0; dhi < DATAHUB_COUNT; dhi++) {
                String envName = baseName + "-" + i;
                String dhKey = envName + "-dh-" + dhi;

                dhWaitingFutures.add(threadPool.submit(() -> {
                    waitForDatahubCreation(testContext, dhKey);
                    return dhKey;
                }));
            }
        }
        waitingForFinishAllThread(dhWaitingFutures, t -> {
        });
        long duration = (System.currentTimeMillis() - start) / 1000;
        LOGGER.info("Duration of test: {}", duration);
    }

    private <T> void waitingForFinishAllThread(List<Future<T>> futures, Consumer<T> consumer) throws InterruptedException, ExecutionException {
        while (!futures.isEmpty()) {
            List<Future<T>> done = new ArrayList<>();
            for (Future<T> future : futures) {
                if (future.isDone()) {
                    done.add(future);
                    T t = future.get();
                    consumer.accept(t);
                }
            }
            futures.removeAll(done);
            Thread.sleep(500);
        }
    }

    protected void initiateDatahubCreation(TestContext testContext, String envName, String dhKey) {
        testContext
                .given(dhKey, DistroXTestDto.class)
                .withEnvironmentName(envName)
                .when(distroXTestClient.create())
                .validate();
    }

    protected void waitForDatahubCreation(TestContext testContext, String dhKey) {
        testContext.given(dhKey, DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.get())
                .validate();
    }

    private Future<String> executeEnvironmentCreation(TestContext testContext, String name, ExecutorService threadPoolExecutor) {
        return threadPoolExecutor.submit(() -> {
            createEnvironmentWithFreeIpa(testContext, name);
            createDatalake(testContext, name);
            waitingForEnv(testContext, name);
            return name;
        });
    }

    private void executeDhCreationWhenDatalakeReady(TestContext testContext, String envName, ExecutorService threadPoolExecutor)
            throws ExecutionException, InterruptedException {
        String sdxKey = envName + "-sdx";
        waitForDatalakeCreation(testContext, sdxKey);
        List<Future<Void>> futures = new ArrayList<>();
        futures.add(threadPoolExecutor.submit(() -> {
            for (int dhi = 0; dhi < DATAHUB_COUNT; dhi++) {
                final int index = dhi;
                Future<Void> future = threadPoolExecutor.submit(() -> {
                    String dhKey = envName + "-dh-" + index;
                    initiateDatahubCreation(testContext, envName, dhKey);
                    return null;
                });
                futures.add(future);
            }
            return null;
        }));
        waitingForFinishAllThread(futures, t -> {
        });
    }

    protected void createDatalake(TestContext testContext, String name) {
        String sdxKey = name + "-sdx";
        initiateDatalakeCreation(testContext, sdxKey, name);
    }

    protected void initiateDatalakeCreation(TestContext testContext, String sdxKey, String name) {
        testContext
                .given(sdxKey, SdxInternalTestDto.class)
                .withEnvironmentName(name)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .validate();
    }

    protected void waitForDatalakeCreation(TestContext testContext, String sdxKey) {
        testContext.given(sdxKey, SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    private void createEnvironmentWithFreeIpa(TestContext testContext, String name) {
        testContext.given(name, EnvironmentTestDto.class)
                .withName(name)
                .when(environmentTestClient.create());
    }

    private void waitingForEnv(TestContext testContext, String name) {
        testContext.given(name, EnvironmentTestDto.class)
                .awaitForCreationFlow()
                .when(environmentTestClient.describe())
                .validate();
    }
}
