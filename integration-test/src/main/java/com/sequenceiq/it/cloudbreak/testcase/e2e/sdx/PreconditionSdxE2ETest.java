package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;

public class PreconditionSdxE2ETest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionSdxE2ETest.class);

    private static final String CREATE_FILE_RECIPE = "classpath:/recipes/post-install.sh";

    private final Map<String, InstanceStatus> instancesHealthy = InstanceUtil.getHealthySDXInstances();

    private final Map<String, InstanceStatus> instancesDeletedOnProviderSide = InstanceUtil.getInstanceStatuses(
            InstanceStatus.DELETED_ON_PROVIDER_SIDE, MASTER, IDBROKER);

    private final Map<String, InstanceStatus> instancesStopped = InstanceUtil.getInstanceStatuses(
            InstanceStatus.STOPPED, MASTER, IDBROKER);

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
    }

    protected SdxTestClient sdxTestClient() {
        return sdxTestClient;
    }

    protected Map<String, InstanceStatus> getSdxInstancesHealthyState() {
        return instancesHealthy;
    }

    protected Map<String, InstanceStatus> getSdxInstancesDeletedOnProviderSideState() {
        return instancesDeletedOnProviderSide;
    }

    protected Map<String, InstanceStatus> getSdxInstancesStoppedState() {
        return instancesStopped;
    }

    protected String getRecipePath() {
        return CREATE_FILE_RECIPE;
    }

    protected String getDefaultSDXBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    protected String getBaseLocation(SdxTestDto testDto) {
        return testDto.getRequest().getCloudStorage().getBaseLocation();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

}
