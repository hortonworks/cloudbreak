package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static java.lang.String.format;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;

public class PreconditionSdxE2ETest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionSdxE2ETest.class);

    private static final String CREATE_FILE_RECIPE = "classpath:/recipes/post-install.sh";

    @Value("${integrationtest.defaultSdxInternalTemplate}")
    private String defaultTemplate;

    private Map<String, InstanceStatus> instancesHealthy = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    private final Map<String, InstanceStatus> instancesDeletedOnProviderSide = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        put(IDBROKER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
    }};

    private final Map<String, InstanceStatus> instancesStopped = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.STOPPED);
        put(IDBROKER.getName(), InstanceStatus.STOPPED);
    }};

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
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
        return defaultTemplate;
    }

    protected SdxTestDto compareVolumeIdsAfterRepair(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        actualVolumeIds.sort(Comparator.naturalOrder());
        expectedVolumeIds.sort(Comparator.naturalOrder());

        if (!actualVolumeIds.equals(expectedVolumeIds)) {
            LOGGER.error("Host Group does not have the desired volume IDs!");
            actualVolumeIds.forEach(volumeid -> Log.log(LOGGER, format(" Actual volume ID: %s ", volumeid)));
            expectedVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Desired volume ID: %s ", volumeId)));
            throw new TestFailException("Host Group does not have the desired volume IDs!");
        } else {
            actualVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Before and after SDX repair volume IDs are equal [%s]. ", volumeId)));
        }
        return sdxTestDto;
    }

    protected String getBaseLocation(SdxTestDto testDto) {
        return testDto.getRequest().getCloudStorage().getBaseLocation();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

}
