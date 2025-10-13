package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXScaleTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXScaleTest.class);

    private final List<String> defaultVolumeIds = new ArrayList<>();

    private final List<String> scaledVolumeIds = new ArrayList<>();

    private int defaultInstanceVolumeRatio = 1;

    private enum DistroXInstanceActions {
        STOP("stop"),
        DELETE("delete");

        private final String name;

        DistroXInstanceActions(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createResourceGroup(testContext);
        createEnvironmentWithFreeIpa(testContext);
        createDatalakeWithoutDatabase(testContext);
        createDefaultDatahubForExistingDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, description = "Resilient Scaling: " +
            "UseCase1: " +
            "- Start upscale on running cluster " +
            "- Delete a non CM server host from provider other hostgroup during upscale " +
            "- Upscale should complete ")
    @Description(
            given = "there is a running default Distrox cluster",
            when = "deleted a Compute instance while cluster was upscaling (by Worker nodes)",
            then = "cluster can be resiliently scaled up then down with higher node count")
    public void testCreateAndScaleDistroX(TestContext testContext, ITestContext iTestContext) {
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());

        if (params.getTimes() < 1) {
            throw new TestFailException("Test should execute at least 1 round of scaling");
        }

        StringBuilder instanceToStopId = new StringBuilder();
        testContext.given(DistroXTestDto.class)
                .then((tc, testDto, client) -> {
                    Optional<String> anInstanceToStop = getDistroxInstanceIds(tc, testDto, client, params.getHostGroup()).stream().findFirst();
                    doDistroxInstanceAction(tc, testDto, List.of(anInstanceToStop.orElseThrow(() ->
                            new TestFailException(String.format(
                                    "At least 1 instance should be present in group %s to stop it then test targeted upscale.",
                                    params.getIrrelevantHostGroup())))), DistroXInstanceActions.STOP);
                    instanceToStopId.append(anInstanceToStop.get());
                    return testDto;
                })
                .await(STACK_NODE_FAILURE)
                .then((tc, testDto, client) -> validateVolumesOnScalingInstances(tc, testDto, client, params))
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                .then((tc, testDto, client) -> {
                    Optional<String> anInstanceToDelete = getDistroxInstanceIds(tc, testDto, client, params.getIrrelevantHostGroup()).stream().findFirst();
                    doDistroxInstanceAction(tc, testDto, List.of(anInstanceToDelete.orElseThrow(() ->
                            new TestFailException(String.format(
                                    "At least 1 instance should be present in group %s to delete it then test targeted upscale.",
                                    params.getIrrelevantHostGroup())))), DistroXInstanceActions.DELETE);
                    testDto.setInstanceIdsForActions(List.of(anInstanceToDelete.get()));
                    return testDto;
                })
                .awaitForFlow()
                .then((tc, testDto, client) -> validateVolumesOnScaledInstances(tc, testDto, client, params))
                // removing deleted instance since downscale still validates if stack is available
                .awaitForActionedInstances(DELETED_ON_PROVIDER_SIDE)
                .then((tc, testDto, client) -> {
                    testDto.setInstanceIdsForActions(mergeInstanceIdList(testDto.getInstanceIdsForAction(), instanceToStopId.toString()));
                    return testDto;
                })
                .when(distroXTestClient.removeInstances())
                .awaitForFlow()
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                .awaitForFlow()
                .validate();
        IntStream.range(1, params.getTimes()).forEach(i -> testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                .awaitForFlow()
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                .awaitForFlow()
                .validate());
    }

    private List<String> mergeInstanceIdList(List<String> instanceIdsForAction, String instance) {
        List<String> result = new ArrayList<>(instanceIdsForAction);
        result.add(instance);
        return result;
    }

    private DistroXTestDto validateVolumesOnScalingInstances(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient,
            DistroXScaleTestParameters distroXScaleTestParameters) {
        List<String> scalingInstances = getDistroxInstanceIds(testContext, testDto, cloudbreakClient, distroXScaleTestParameters.getHostGroup());
        if (scalingInstances.isEmpty()) {
            LOGGER.warn(String.format(
                    "At least 1 instance should be present for %s group by default!", distroXScaleTestParameters.getHostGroup()));
        }
        defaultVolumeIds.addAll(listDistroxInstancesVolumeIds(testContext, testDto, scalingInstances));
        defaultInstanceVolumeRatio = defaultVolumeIds.size() / scalingInstances.size();
        LOGGER.info(String.format("The selected group is %s for scaling. " +
                        "The group contains %d nodes by default and these have following volumes: %s.",
                distroXScaleTestParameters.getHostGroup(), scalingInstances.size(),
                getDistroxInstancesAndVolumes(testContext, testDto, scalingInstances, defaultInstanceVolumeRatio, distroXScaleTestParameters)));
        return testDto;
    }

    private DistroXTestDto validateVolumesOnScaledInstances(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient,
            DistroXScaleTestParameters distroXScaleTestParameters) {
        List<String> scaledInstances = getDistroxInstanceIds(testContext, testDto, cloudbreakClient, distroXScaleTestParameters.getHostGroup());
        if (scaledInstances.isEmpty() || scaledInstances.size() != distroXScaleTestParameters.getScaleUpTarget()) {
            LOGGER.warn(String.format(
                    "At least %d instances should be present for the scaled up %s group!", distroXScaleTestParameters.getScaleUpTarget(),
                    distroXScaleTestParameters.getHostGroup()));
        }
        scaledVolumeIds.addAll(listDistroxInstancesVolumeIds(testContext, testDto, scaledInstances));
        int scaledInstanceVolumeRatio = scaledVolumeIds.size() / scaledInstances.size();
        List<String> newVolumeIds = new ArrayList<>((CollectionUtils.removeAll(scaledVolumeIds, defaultVolumeIds)));
        if (newVolumeIds.isEmpty() || defaultInstanceVolumeRatio != scaledInstanceVolumeRatio) {
            throw new TestFailException(String.format(
                    "The required amount of new volumes per instance %d cannot get available after scaling of %s group!", scaledInstanceVolumeRatio,
                    distroXScaleTestParameters.getHostGroup()));
        } else {
            LOGGER.info(String.format("The %s group after scaling contains %d nodes and these have following volumes: %s.",
                    distroXScaleTestParameters.getHostGroup(), scaledInstances.size(),
                    getDistroxInstancesAndVolumes(testContext, testDto, scaledInstances, scaledInstanceVolumeRatio, distroXScaleTestParameters)));
        }
        return testDto;
    }

    private List<String> getDistroxInstanceIds(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient, String hostGroupName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        return distroxUtil.getInstanceIds(testDto, cloudbreakClient, hostGroupName);
    }

    private List<String> listDistroxInstancesVolumeIds(TestContext testContext, DistroXTestDto testDto, List<String> instances) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        return cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instances);
    }

    private Map<String, Set<String>> getDistroxInstancesAndVolumes(TestContext testContext, DistroXTestDto testDto, List<String> instances,
            int instanceVolumeRatio, DistroXScaleTestParameters distroXScaleTestParameters) {
        Map<String, Set<String>> selectedInstancesAndVolumes = new HashMap<>();
        instances.forEach(instanceId -> {
            Map<String, Set<String>> selectedInstanceWithVolumes = listDistroxInstanceVolumeIds(testContext, testDto, instanceId);
            Set<String> volumeIds = selectedInstanceWithVolumes.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            if (volumeIds.isEmpty() || volumeIds.size() != instanceVolumeRatio) {
                throw new TestFailException(String.format(
                        "At least %d volume should be present for %s instance in %s group!", instanceVolumeRatio, instanceId,
                        distroXScaleTestParameters.getHostGroup()));
            }
            selectedInstancesAndVolumes.putAll(selectedInstanceWithVolumes);
        });
        return selectedInstancesAndVolumes;
    }

    private Map<String, Set<String>> listDistroxInstanceVolumeIds(TestContext testContext, DistroXTestDto testDto, String instance) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        return cloudFunctionality.listInstanceVolumeIds(testDto.getName(), instance);
    }

    private void doDistroxInstanceAction(TestContext testContext, DistroXTestDto testDto, List<String> instanceIds, DistroXInstanceActions actionName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        switch (actionName) {
            case STOP -> cloudFunctionality.stopInstances(testDto.getName(), instanceIds);
            case DELETE -> cloudFunctionality.deleteInstances(testDto.getName(), instanceIds);
            default -> throw new IllegalStateException("Unexpected value: " + actionName.getName());
        }
    }
}
