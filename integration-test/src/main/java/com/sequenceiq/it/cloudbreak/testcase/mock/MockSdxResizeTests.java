package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxResizeTests extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    private String sdxName;

    protected void setupTest(TestContext testContext) {
        sdxName = resourcePropertyProvider().getName();
        String networkKey = "someNetwork";

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("mock://location/of/the/backup")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(sdxName, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Resize request is sent",
            then = "Resized SDX should be available AND deletable"
    )
    public void testDefaultSDXResizeSuccessfully(MockedTestContext testContext) {
        testContext
                .given(sdxName, SdxInternalTestDto.class)
                .when(sdxTestClient.resize(), key(sdxName))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
                .validate();

//        testContext
//                .given(sdxName+"-detached", SdxTestDto.class)
//                .when(sdxTestClient.checkStatus(sdxName+"-detached"))
//                .await(SdxClusterStatusResponse.RUNNING, key(sdxName+"-detached"))
//                .validate();
                //TODO Make sure that the runtime is same, storage location is also same.
    }
}
