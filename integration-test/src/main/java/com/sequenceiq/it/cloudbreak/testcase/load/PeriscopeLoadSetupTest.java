package com.sequenceiq.it.cloudbreak.testcase.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.assertion.audit.DatahubAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.PeriscopeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.config.LoadTestProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.autoscale.AutoScaleConfigDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class PeriscopeLoadSetupTest extends AbstractClouderaManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriscopeLoadSetupTest.class);

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String DIX_NET_KEY = "dixNet";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private DatahubAuditGrpcServiceAssertion auditGrpcServiceAssertion;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private PeriscopeTestClient periscopeTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private LoadTestProperties loadTestProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "setup for load testing is called",
            then = "the clusters should be available along with autoscaling rules")
    public void testSetUpClustersForLoadTesting(MockedTestContext testContext) {
        List<Future<String>> tasks = new ArrayList<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(loadTestProperties.getNumThreads());
        if (loadTestProperties.getNumTenants() > 1) {
            throw new TestFailException("Only one tenant is supported");
        }
        for (int tenantCounter = 1; tenantCounter <= loadTestProperties.getNumTenants(); tenantCounter++) {
            int tenantCounterFinal = tenantCounter;
            tasks.add(threadPool.submit(() -> {
                setUpForTenant(testContext, tenantCounterFinal, threadPool);
                return String.valueOf(tenantCounterFinal);
            }));
        }
        waitingForFinishAllThread(tasks);
    }

    private void setUpForTenant(MockedTestContext testContext, Integer tenantIndex, ExecutorService threadPool) {
        String credentialName = PeriscopeLoadUtils.getCredentialName(tenantIndex);
        String catalogName = PeriscopeLoadUtils.getCatalogName(tenantIndex);
        String bluePrintName = PeriscopeLoadUtils.getBlueprintName(tenantIndex);
        String imageName = PeriscopeLoadUtils.getImageName(tenantIndex);
        testContext.given(credentialName, CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.create())
                .validate();
        testContext
                .given(catalogName, ImageCatalogTestDto.class)
                .withName(catalogName)
                .when(new ImageCatalogCreateRetryAction())
                .validate();
        testContext.given(imageName, DistroXImageTestDto.class)
                .withImageCatalog(catalogName)
                .withImageId(loadTestProperties.getImageCatalogId());
        createCmBlueprint(testContext, bluePrintName);
        List<Future<String>> tasks = new ArrayList<>();
        for (int envCounter = 1; envCounter <= loadTestProperties.getNumEnvironmentsPerTenant(); envCounter++) {
            String envName = PeriscopeLoadUtils.getEnvironmentName(tenantIndex, envCounter);
            tasks.add(threadPool.submit(() -> {
                createEnvironment(testContext, envName, credentialName, catalogName, bluePrintName, imageName, threadPool);
                return envName;
            }));
        }
        waitingForFinishAllThread(tasks);
    }

    private void createEnvironment(MockedTestContext testContext, String envName, String credentialName, String catalogName, String bluePrintName,
            String imageName, ExecutorService threadPool) {
        String freeIpaName = PeriscopeLoadUtils.getFreeIpaName(envName);
        String dataLakeName = PeriscopeLoadUtils.getDataLakeName(envName);
        EnvironmentTestDto environmentDetails = testContext.given(envName, EnvironmentTestDto.class);
        environmentDetails.withCreateFreeIpa(Boolean.FALSE)
                .withName(envName)
                .withCredentialName(credentialName)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.describe());
        FreeIpaTestDto freeIpaTestDto = testContext.given(freeIpaName, FreeIpaTestDto.class);
        freeIpaTestDto.withName(freeIpaName)
                .withEnvironmentCrn(environmentDetails.getCrn())
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow();
        SdxInternalTestDto sdxInternalTestDto = testContext.given(dataLakeName, SdxInternalTestDto.class);
        StackV4Request stackV4Request = sdxInternalTestDto.getRequest().getStackV4Request();
        sdxInternalTestDto.withName(dataLakeName)
                .withEnvironmentName(environmentDetails.getName())
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withEnableMultiAz()
                .withTelemetry("telemetry")
                .withStackRequest(copyStackWithNameAndImage(stackV4Request, dataLakeName, catalogName))
                .when(sdxTestClient.createInternal())
                .given(dataLakeName, SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING);
        List<Future<String>> tasks = new ArrayList<>();
        for (int dhCounter = 1; dhCounter <= loadTestProperties.getNumDataHubsPerEnvironment(); dhCounter++) {
            int dhCounterFinal = dhCounter;
            tasks.add(threadPool.submit(() -> {
                createDataHub(dhCounterFinal, testContext, envName, bluePrintName, imageName);
                return "";
            }));
        }
        waitingForFinishAllThread(tasks);
    }

    private String createDataHub(int dataHubIndex, MockedTestContext testContext, String envName, String bluePrintName,
            String imageName) {
        String dataHubName = PeriscopeLoadUtils.getDataHubName(envName, dataHubIndex);
        String clusterName = PeriscopeLoadUtils.getClusterName(dataHubName);
        String loadAlertName = PeriscopeLoadUtils.getLoadAlertName(dataHubName);
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class);
        DistroXTestDto distroXTestDto = testContext.given(dataHubName, DistroXTestDto.class);
        distroXTestDto.withCluster(testContext.given(clusterName, DistroXClusterTestDto.class)
                        .withBlueprintName(bluePrintName)
                        .withValidateBlueprint(false)
                        .withClouderaManager(CM_FOR_DISTRO_X))
                .withImageSettings(imageName)
                .withNetwork(DIX_NET_KEY)
                .withEnvironmentName(envName)
                .withName(dataHubName)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE);
        AutoScaleConfigDto autoScaleConfigDto = new AutoScaleConfigDto(testContext);
        autoScaleConfigDto.setName(dataHubName);
        autoScaleConfigDto.withEnableAutoScaling(true);
        autoScaleConfigDto.withUseStopStartMechanism(true);
        autoScaleConfigDto.withLoadAlert(loadAlertName, loadTestProperties.getMinNodes(), loadTestProperties.getMaxNodes());
        autoScaleConfigDto.when(periscopeTestClient.create());
        return dataHubName;
    }

    @Override
    public void tearDown(Object[] data) {
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
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

    private StackV4Request copyStackWithNameAndImage(StackV4Request source, String name, String catalogName) {
        StackV4Request result = new StackV4Request();
        result.setName(name);
        ImageSettingsV4Request image = new ImageSettingsV4Request();
        image.setCatalog(catalogName);
        image.setId(loadTestProperties.getImageCatalogId());
        result.setImage(image);
        result.setPlacement(source.getPlacement());
        result.setInstanceGroups(source.getInstanceGroups());
        result.setAuthentication(source.getAuthentication());
        result.setNetwork(source.getNetwork());
        result.setCluster(source.getCluster());
        result.setType(source.getType());
        result.setEnableLoadBalancer(source.isEnableLoadBalancer());
        result.setEnableMultiAz(source.isEnableMultiAz());
        return result;
    }
}
