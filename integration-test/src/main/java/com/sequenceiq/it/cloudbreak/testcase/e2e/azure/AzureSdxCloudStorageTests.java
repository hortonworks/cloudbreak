package com.sequenceiq.it.cloudbreak.testcase.e2e.azure;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class AzureSdxCloudStorageTests extends AbstractE2ETest {

    private Map<String, InstanceStatus> instancesHealthy = new HashMap<String, InstanceStatus>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @Inject
    private WaitUtil waitUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeAzureCloudStorage(testContext);
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request with attached ADLS Gen2 Cloud Storage has been sent",
            then = "SDX should be available AND deletable along with the created ADLS Gen2 objects"
    )
    public void testSDXWithCloudStorageCanBeCreatedThenDeletedSuccessfully(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesHealthy);
                })
                .then((tc, testDto, client) -> {
                    return azureCloudBlobUtil.listAllFoldersInAContaier(tc, testDto, client);
                })
                .then((tc, testDto, client) -> {
                    return azureCloudBlobUtil.cleanupContainer(tc, testDto, client);
                })
                .validate();
    }
}
