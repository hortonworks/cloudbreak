package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxUpgradeRecoveryTests extends PreconditionSdxE2ETest {

    private static final String INJECT_UPGRADE_FAILURE_CMD =
            "sudo sh -c \"echo -e '"
                    + "\\n127.0.0.1 archive.cloudera.com"
                    + "\\n127.0.0.1 cloudera-build-us-west-1.vpc.cloudera.com"
                    + "\\n127.0.0.1 build-cache-azure.kc.cloudera.com"
                    + "\\n127.0.0.1 cloudera-build-2-us-west-2.vpc.cloudera.com"
                    + "\\n127.0.0.1 cloudera-build-3-us-central-1-3.gce.cloudera.com"
                    + "\\n127.0.0.1 build-cache.vpc.cloudera.com"
                    + "\\n127.0.0.1 cache-test1.vpc.cloudera.com'"
                    + " >> /etc/hosts\"";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeRecoveryTests.class);

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak and an SDX cluster in available state, "
                    + "failure point inserted to make the upgrade fail then upgrade is called on the SDX cluster",
            when = "recovery called on the SDX cluster",
            then = "SDX recovery should be successful, the cluster should be up and running, the image should be the same, stack CRN should be retained"
    )
    public void testSDXUpgradeRecovery(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        AtomicReference<String> originalImageId = new AtomicReference<>();
        AtomicReference<String> originalCrn = new AtomicReference<>();

        testContext
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(commonClusterManagerProperties.getUpgrade()
                        .getCurrentRuntimeVersion(testContext.getCloudProvider().getGovCloud()))
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> getOriginalImageIdAndStackCrn(originalImageId, originalCrn, testDto, client))
                .then((tc, testDto, client) -> executeCommandToCauseUpgradeFailure(testDto, client))
                .when(sdxTestClient.upgrade(), key(sdx))
                .awaitForFlowFail()
                .when(sdxTestClient.recoverFromUpgrade(), key(sdx))
                .awaitForFlow(emptyRunningParameter().withWaitForFlowSuccess())
                .when(sdxTestClient.refresh(), key(sdx))
                .then((tc, dto, client) -> validateStackCrn(originalCrn, dto))
                .then((tc, dto, client) -> validateImageId(originalImageId, dto))
                .validate();
    }

    private SdxTestDto getOriginalImageIdAndStackCrn(AtomicReference<String> originalImageId,
            AtomicReference<String> originalCrn, SdxTestDto testDto, SdxClient client) {
        originalImageId.set(sdxUtil.getImageId(testDto, client));
        originalCrn.set(testDto.getCrn());
        return testDto;
    }

    private SdxTestDto executeCommandToCauseUpgradeFailure(SdxTestDto testDto, SdxClient client) {
        Map<String, Pair<Integer, String>> hostsAppenderCmdResultByIpsMap = sshJClientActions.executeSshCommandOnHost(
                testDto.getResponse().getStackV4Response().getInstanceGroups(), List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()),
                INJECT_UPGRADE_FAILURE_CMD, false);
        LOGGER.debug("SSH hosts file edit result: " + hostsAppenderCmdResultByIpsMap);
        return testDto;
    }

    private SdxTestDto validateStackCrn(AtomicReference<String> originalCrn, SdxTestDto dto) {
        String newCrn = dto.getResponse().getStackV4Response().getCrn();
        Log.log(LOGGER, format(" Stack new crn: %s ", newCrn));
        if (!newCrn.equals(originalCrn.get())) {
            throw new TestFailException(" The stack CRN has changed to: " + newCrn + " instead of: " + originalCrn.get());
        }
        return dto;
    }

    private SdxTestDto validateImageId(AtomicReference<String> originalImageId, SdxTestDto dto) {
        StackImageV4Response image = dto.getResponse().getStackV4Response().getImage();
        Log.log(LOGGER, format(" Image Catalog Name: %s ", image.getCatalogName()));
        Log.log(LOGGER, format(" Image Catalog URL: %s ", image.getCatalogUrl()));
        Log.log(LOGGER, format(" Image ID: %s ", image.getId()));

        if (!image.getId().equals(originalImageId.get())) {
            throw new TestFailException(" The selected image ID is: " + image.getId() + " instead of: " + originalImageId.get());
        }
        return dto;
    }
}
