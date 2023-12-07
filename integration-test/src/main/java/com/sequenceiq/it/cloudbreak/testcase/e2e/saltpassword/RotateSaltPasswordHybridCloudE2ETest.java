package com.sequenceiq.it.cloudbreak.testcase.e2e.saltpassword;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid.HybridCloudE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSaltPasswordActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class RotateSaltPasswordHybridCloudE2ETest extends HybridCloudE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordE2ETest.class);

    private static final LocalDate PAST_DATE = LocalDate.now().minusMonths(1);

    private static final AtomicReference<String> SHADOW_LINE_REFERENCE = new AtomicReference<>();

    @Inject
    private SshSaltPasswordActions sshSaltPasswordActions;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running environment with FreeIPA ",
            when = "rotating a salt password ",
            then = "FreeIPA should have a new salt password and password expiry ",
            and = "the same applies to both Datalake and Datahub"
    )
    public void testHybridRotateSaltPassword(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> preSaltPasswordRotation(testDto, getFreeipaIpAddresses(testDto)))
                .when(freeIpaTestClient.rotateSaltPassword())
                .awaitForFlow()
                .await(Status.AVAILABLE)
                .then((tc, testDto, client) -> validateSaltPasswordRotation(testDto, getFreeipaIpAddresses(testDto)))
                .validate();
        LOGGER.info("FreeIPA salt password rotation test PASSED");

        createChildDatalake(testContext);
        testContext
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then((tc, testDto, client) -> preSaltPasswordRotation(testDto, getSdxIpAddresses(testDto)))
                .when(sdxTestClient.rotateSaltPassword())
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING, key(CHILD_SDX_KEY).withWaitForFlow(Boolean.TRUE))
                .then((tc, testDto, client) -> validateSaltPasswordRotation(testDto, getSdxIpAddresses(testDto)))
                .validate();
        LOGGER.info("SDX salt password rotation test PASSED");

        createChildDatahubForExistingDatalake(testContext);
        testContext
                .given(CHILD_DISTROX_KEY, DistroXTestDto.class, CHILD_CLOUD_PLATFORM)
                .then((tc, testDto, client) -> preSaltPasswordRotation(testDto, getDistroXIpAddresses(testDto)))
                .when(distroXTestClient.rotateSaltPassword())
                .awaitForFlow()
                .then((tc, testDto, client) -> validateSaltPasswordRotation(testDto, getDistroXIpAddresses(testDto)))
                .validate();
        LOGGER.info("DistroX salt password rotation test PASSED");
    }

    private <T> T preSaltPasswordRotation(T testDto, Set<String> ipAddresses) {
        sshSaltPasswordActions.setPasswordChangeDate(ipAddresses, PAST_DATE);
        SHADOW_LINE_REFERENCE.set(sshSaltPasswordActions.getShadowLine(ipAddresses));
        return testDto;
    }

    private <T> T validateSaltPasswordRotation(T testDto, Set<String> ipAddresses) {
        String shadowLine = sshSaltPasswordActions.getShadowLine(ipAddresses);
        if (shadowLine.equals(SHADOW_LINE_REFERENCE.get())) {
            throw new TestFailException("Saltuser shadow line was not changed after password rotation");
        }
        SHADOW_LINE_REFERENCE.set("");

        LocalDate passwordChange = sshSaltPasswordActions.getPasswordChangeDate(ipAddresses);
        if (!passwordChange.isEqual(LocalDate.now())) {
            throw new TestFailException("Saltuser password change date was not modified to today after password rotation");
        }
        return testDto;
    }

    private static Set<String> getFreeipaIpAddresses(FreeIpaTestDto testDto) {
        return testDto.getResponse().getFreeIpa().getServerIp();
    }

    private static Set<String> getSdxIpAddresses(SdxInternalTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse().getStackV4Response());
    }

    private static Set<String> getDistroXIpAddresses(DistroXTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse());
    }

    private static Set<String> getStackIpAddresses(StackV4Response stack) {
        return stack.getInstanceGroups().stream()
                .filter(ig -> ig.getType().equals(InstanceGroupType.GATEWAY))
                .flatMap(ig -> ig.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }
}
