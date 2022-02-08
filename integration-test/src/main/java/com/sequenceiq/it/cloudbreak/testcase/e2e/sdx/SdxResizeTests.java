package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxResizeTests extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXResize(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        AtomicReference<String> expectedShape = new AtomicReference<>();
        AtomicReference<String> expectedCrn = new AtomicReference<>();
        AtomicReference<String> expectedName = new AtomicReference<>();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        testContext
                .given(sdx, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    expectedShape.set(sdxUtil.getShape(testDto, client));
                    expectedCrn.set(sdxUtil.getCrn(testDto, client));
                    expectedName.set(sdx);
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS)
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> validateStackCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateCrn(expectedCrn, dto))
                .then((tc, dto, client) -> validateShape(dto))
                .then((tc, dto, client) -> validateClusterName(expectedName, dto))
                .validate();
    }

    private SdxInternalTestDto validateStackCrn(AtomicReference<String> originalCrn, SdxInternalTestDto dto) {
        String newCrn = dto.getResponse().getStackV4Response().getCrn();
        Log.log(LOGGER, format(" Stack new crn: %s ", newCrn));
        if (!newCrn.equals(originalCrn.get())) {
            throw new TestFailException(" The stack CRN has changed to: " + newCrn + " instead of: " + originalCrn.get());
        }
        return dto;
    }

    private SdxInternalTestDto validateCrn(AtomicReference<String> originalCrn, SdxInternalTestDto dto) {
        String newCrn = dto.getResponse().getCrn();
        Log.log(LOGGER, format(" New crn: %s ", newCrn));
        if (!newCrn.equals(originalCrn.get())) {
            throw new TestFailException(" The stack CRN has changed to: " + newCrn + " instead of: " + originalCrn.get());
        }
        return dto;
    }

    private SdxInternalTestDto validateShape(SdxInternalTestDto dto) {
        SdxClusterShape newShape = dto.getResponse().getClusterShape();
        Log.log(LOGGER, format(" New shape: %s ", newShape.name()));
        if (!SdxClusterShape.MEDIUM_DUTY_HA.equals(newShape)) {
            throw new TestFailException(" The datalake shape is : " + newShape + " instead of: " + SdxClusterShape.MEDIUM_DUTY_HA.name());
        }
        return dto;
    }

    private SdxInternalTestDto validateClusterName(AtomicReference<String> originalName, SdxInternalTestDto dto) {
        String newClusterName = dto.getResponse().getStackV4Response().getCluster().getName();
        Log.log(LOGGER, format(" New cluster name: %s ", newClusterName));
        if (!originalName.get().equals(newClusterName)) {
            throw new TestFailException(" The datalake cluster name is : " + newClusterName + " instead of: " + originalName);
        }
        return dto;
    }
}
