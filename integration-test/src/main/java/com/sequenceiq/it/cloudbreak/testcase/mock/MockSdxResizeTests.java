package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxResizeTests extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Resize request is sent",
            then = "Resized SDX should be available AND deletable"
    )
    public void testDefaultSDXResizeSuccessfully(MockedTestContext testContext) {
        String sdxName = resourcePropertyProvider().getName();
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.LIGHT_DUTY);
        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("mock://location/of/the/backup")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(sdxName, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .given(sdxName, SdxInternalTestDto.class)
                .then((tc, testDto, client) -> {
                    resizeTestValidator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    resizeTestValidator.setExpectedName(testDto.getName());
                    resizeTestValidator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.DATALAKE_RESTORE_INPROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .then((tc, dto, client) -> resizeTestValidator.validateResizedCluster(dto))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Resize request is sent but cancel datalake backup is called during the backup process",
            then = "stack status reason should be datalake cancelled when stack turns back to be RUNNING"
    )
    public void testFailedSDXResizeWithBackupCancelled(MockedTestContext testContext) {
        String sdxName = resourcePropertyProvider().getName();
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.LIGHT_DUTY);
        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("mock://location/of/the/backup/cancel")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(sdxName, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .given(sdxName, SdxInternalTestDto.class)
                .then((tc, testDto, client) -> {
                    resizeTestValidator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    resizeTestValidator.setExpectedName(testDto.getName());
                    resizeTestValidator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> {
                    SdxClusterDetailResponse sdx = testDto.getResponse();
                    assertEquals(sdx.getStatusReason(), "Datalake backup cancelled");
                    return testDto;
                })
                .validate();
    }
}
