package com.sequenceiq.it.cloudbreak.testcase.e2e.azure;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class AzureSdxCloudStorageTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSdxCloudStorageTests.class);

    private static final SdxClusterStatusResponse SDX_RUNNING = SdxClusterStatusResponse.RUNNING;

    private static final SdxClusterStatusResponse SDX_DELETED = SdxClusterStatusResponse.DELETED;

    private static final InstanceStatus SERVICES_RUNNING = InstanceStatus.SERVICES_RUNNING;

    private static final String IDBROKER_HOSTGROUP = HostGroupType.IDBROKER.getName();

    private static final String MASTER_HOSTGROUP = HostGroupType.MASTER.getName();

    private Map<String, InstanceStatus> instancesRegistered = new HashMap<String, InstanceStatus>() {{
        put(MASTER_HOSTGROUP, SERVICES_RUNNING);
        put(IDBROKER_HOSTGROUP, SERVICES_RUNNING);
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
                .await(SDX_RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesRegistered);
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
