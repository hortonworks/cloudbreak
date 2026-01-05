package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class MockSdxRepairTests extends AbstractMockTest {

    /**
     * statuschecker.intervalsec by default is 180 seconds. we need to wait for 2 syncs, one in SDX, one in CB.
     * We need at least 2 times that interval, testframework only waits for 1000*300 ms
     **/
    private static final long POLLING_INTERVAL_FOR_REPAIR_SECONDS = 2;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "terminate instances and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairTerminatedMasterAndIdbroker(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER, IDBROKER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/terminate", w -> w),
                SdxClusterStatusResponse.DELETED_ON_PROVIDER_SIDE);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "terminate instances and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairTerminatedMaster(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/terminate", w -> w),
                SdxClusterStatusResponse.NODE_FAILURE);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "terminate instances and repair an sdx cluster",
            then = "repair should fail and repaired instance should be in deleted on provider side"
    )
    public void repairTerminatedMasterAndItFailedButInstanceShouldBeDeletedOnProviderSide(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        CustomDomainSettingsV4Request customDomain = new CustomDomainSettingsV4Request();
        customDomain.setDomainName("dummydomainname");
        customDomain.setHostname("dummyhostname");
        customDomain.setClusterNameAsSubdomain(true);
        customDomain.setHostgroupNameAsHostname(true);
        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCustomDomain(customDomain)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = new ArrayList<>(sdxUtil.getInstanceIds(testDto, client, MASTER.getName()));
                    instancesToDelete.forEach(instanceId -> getExecuteQueryToMockInfrastructure()
                            .call("/" + testDto.getCrn() + "/spi/" + instanceId + "/terminate", w -> w));
                    getExecuteQueryToMockInfrastructure().executeMethod(Method.build("POST"), "/" + testDto.getCrn() + "/spi/disable_add_instance",
                            new HashMap<>(), null, response -> { }, w -> w);
                    return testDto;
                })
                .await(SdxClusterStatusResponse.NODE_FAILURE, pollingInterval(Duration.ofSeconds(POLLING_INTERVAL_FOR_REPAIR_SECONDS)))
                .when(sdxTestClient.repairInternal(MASTER.getName()), key(sdxInternal))
                .awaitForMasterDeletedOnProvider()
                .awaitForFlowFail()
                .then((tc, testDto, client) -> {
                    getExecuteQueryToMockInfrastructure().executeMethod(Method.build("POST"), "/" + testDto.getCrn() + "/spi/enable_add_instance",
                            new HashMap<>(), null, response -> { }, w -> w);
                    return testDto;
                })
                .when(sdxTestClient.repairInternal(MASTER.getName()), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, invocationCount = 1)
    @Description(
            given = "there is a running Cloudbreak",
            when = "stop and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairStoppedMasterAndIdbroker(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER, IDBROKER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/stop", w -> w),
                SdxClusterStatusResponse.STOPPED);
    }

    public void testRepair(MockedTestContext testContext,
            List<HostGroupType> hostGroups,
            Consumer<String> actionOnNode,
            SdxClusterStatusResponse stateBeforeRepair
    ) {
        String sdxInternal = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        CustomDomainSettingsV4Request customDomain = new CustomDomainSettingsV4Request();
        customDomain.setDomainName("dummydomainname");
        customDomain.setHostname("dummyhostname");
        customDomain.setClusterNameAsSubdomain(true);
        customDomain.setHostgroupNameAsHostname(true);
        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(Optional.ofNullable(testContext.getExistingResourceNames().get(EnvironmentTestDto.class))
                        .orElse(resourcePropertyProvider().getName(CloudPlatform.MOCK)))
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .withName(Optional.ofNullable(testContext.getExistingResourceNames().get(FreeIpaTestDto.class))
                        .orElse(resourcePropertyProvider().getName(CloudPlatform.MOCK)))
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withName(Optional.ofNullable(testContext.getExistingResourceNames().get(SdxInternalTestDto.class))
                        .orElse(resourcePropertyProvider().getName(CloudPlatform.MOCK)))
                .withDatabase(sdxDatabaseRequest)
                .withCustomDomain(customDomain)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = new ArrayList<>();
                    for (HostGroupType hostGroupType : hostGroups) {
                        instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, hostGroupType.getName()));
                    }
                    instancesToDelete.forEach(instanceId -> actionOnNode.accept("/" + testDto.getCrn() + "/spi/" + instanceId));
                    return testDto;
                })
                .await(stateBeforeRepair, pollingInterval(Duration.ofSeconds(POLLING_INTERVAL_FOR_REPAIR_SECONDS)))
                .when(sdxTestClient.repairInternal(hostGroups.stream().map(HostGroupType::getName).toArray(String[]::new)), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .validate();
    }
}
