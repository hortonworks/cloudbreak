package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class MockSdxRepairTests extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

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
                SdxClusterStatusResponse.CLUSTER_AMBIGUOUS);
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
        String networkKey = "someOtherNetwork";

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        CustomDomainSettingsV4Request customDomain = new CustomDomainSettingsV4Request();
        customDomain.setDomainName("dummydomainname");
        customDomain.setHostname("dummyhostname");
        customDomain.setClusterNameAsSubdomain(true);
        customDomain.setHostgroupNameAsHostname(true);
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(sdxInternal, SdxInternalTestDto.class)
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
                .await(stateBeforeRepair)
                .when(sdxTestClient.repairInternal(hostGroups.stream().map(HostGroupType::getName).toArray(String[]::new)), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .validate();
    }
}
