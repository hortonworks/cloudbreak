package com.sequenceiq.it.cloudbreak.util.resize;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

public class SdxResizeTestValidator {
    private final SdxClusterShape expectedShape;

    private final AtomicReference<String> expectedCrn;

    private final AtomicReference<String> expectedName;

    private final AtomicReference<String> expectedRuntime;

    private final AtomicReference<Long> expectedCreationTimestamp;

    private final AtomicReference<List<SdxInstanceGroupRequest>> expectedCustomInstanceGroups;

    private final AtomicReference<List<SdxInstanceGroupDiskRequest>> expectedSdxInstanceGroupDiskRequest;

    public SdxResizeTestValidator(SdxClusterShape expectedShape) {
        this.expectedShape = expectedShape;
        expectedCrn = new AtomicReference<>();
        expectedName = new AtomicReference<>();
        expectedRuntime = new AtomicReference<>();
        expectedCreationTimestamp = new AtomicReference<>();
        expectedCustomInstanceGroups = new AtomicReference<>();
        expectedSdxInstanceGroupDiskRequest = new AtomicReference<>();
    }

    public void setExpectedCrn(String expectedCrn) {
        this.expectedCrn.set(expectedCrn);
    }

    public void setExpectedName(String expectedName) {
        this.expectedName.set(expectedName);
    }

    public void setExpectedRuntime(String expectedRuntime) {
        this.expectedRuntime.set(expectedRuntime);
    }

    public void setExpectedCreationTimestamp(Long expectedCreationTimestamp) {
        this.expectedCreationTimestamp.set(expectedCreationTimestamp);
    }

    public void setExpectedCustomInstanceGroups(List<SdxInstanceGroupRequest> expectedCustomInstanceGroups) {
        this.expectedCustomInstanceGroups.set(expectedCustomInstanceGroups);
    }

    public void setExpectedSdxInstanceGroupDiskRequest(List<SdxInstanceGroupDiskRequest> expectedSdxInstanceGroupDiskRequest) {
        this.expectedSdxInstanceGroupDiskRequest.set(expectedSdxInstanceGroupDiskRequest);
    }

    public SdxInternalTestDto validateResizedCluster(SdxInternalTestDto dto) {
        SdxClusterDetailResponse response = dto.getResponse();
        validateClusterShape(response.getClusterShape());
        validateCrn(response.getCrn());
        validateStackCrn(response.getStackCrn());
        validateName(response.getName());
        validateRuntime(response.getRuntime());
        validateCustomInstanceGroups(response.getStackV4Response().getInstanceGroups());
        validateCustomInstanceDiskSize(response.getStackV4Response().getInstanceGroups());
        return dto;
    }

    private void validateCustomInstanceGroups(List<InstanceGroupV4Response> instanceGroupV4Responses) {
        List<SdxInstanceGroupRequest> customInstanceGroups = expectedCustomInstanceGroups.get();
        if (!CollectionUtils.isEmpty(customInstanceGroups)) {
            customInstanceGroups
                    .forEach(customInstanceGroup ->
                            getInstanceGroupByName(customInstanceGroup.getName(), instanceGroupV4Responses)
                                    .ifPresent(instanceGroup -> {
                                        if (!customInstanceGroup.getInstanceType().equals(instanceGroup.getTemplate().getInstanceType())) {
                                            fail("instanceType " + customInstanceGroup.getName(),
                                                    customInstanceGroup.getInstanceType(), instanceGroup.getTemplate().getInstanceType());
                                        }
                                    }));
        }

    }

    private void validateCustomInstanceDiskSize(List<InstanceGroupV4Response> instanceGroupV4Responses) {
        List<SdxInstanceGroupDiskRequest> customInstanceGroupDisks = expectedSdxInstanceGroupDiskRequest.get();
        if (!CollectionUtils.isEmpty(customInstanceGroupDisks)) {
            customInstanceGroupDisks
                    .forEach(customInstanceGroupDisk ->
                            getInstanceGroupByName(customInstanceGroupDisk.getName(), instanceGroupV4Responses)
                                    .ifPresent(instanceGroup -> {
                                        Integer actualDiskSize =
                                                instanceGroup
                                                        .getTemplate()
                                                        .getAttachedVolumes()
                                                        .stream()
                                                        .findAny()
                                                        .get()
                                                        .getSize();
                                        if (!actualDiskSize.equals(customInstanceGroupDisk.getInstanceDiskSize())) {
                                            fail("instanceDiskSize " + customInstanceGroupDisk.getName(),
                                                    customInstanceGroupDisk.getInstanceDiskSize().toString(), actualDiskSize.toString());
                                        }
                                    }));
        }
    }

    private Optional<InstanceGroupV4Response> getInstanceGroupByName(String name, List<InstanceGroupV4Response> instanceGroupV4Responses) {
        return instanceGroupV4Responses
                .stream()
                .filter(instanceGroup -> StringUtils.equals(instanceGroup.getName(), name))
                .findAny();
    }

    public SdxInternalTestDto validateRecoveredCluster(SdxInternalTestDto dto) {
        validateResizedCluster(dto);
        SdxClusterResponse response = dto.getResponse();
        validateNotDetached(response.isDetached());
        validateCreationTimestamp(response.getCreated());
        return dto;
    }

    private void validateClusterShape(SdxClusterShape shape) {
        if (!expectedShape.equals(shape)) {
            fail("cluster shape", expectedShape.name(), shape.name());
        }
    }

    private void validateCrn(String crn) {
        if (!expectedCrn.get().equals(crn)) {
            fail("crn", expectedCrn.get(), crn);
        }
    }

    private void validateStackCrn(String stackCrn) {
        if (!expectedCrn.get().equals(stackCrn)) {
            fail("stack crn", expectedCrn.get(), stackCrn);
        }
    }

    private void validateName(String name) {
        if (!expectedName.get().equals(name)) {
            fail("name", expectedName.get(), name);
        }
    }

    private void validateRuntime(String runtime) {
        if (!expectedRuntime.get().equals(runtime)) {
            fail("runtime", expectedRuntime.get(), runtime);
        }
    }

    private void validateNotDetached(boolean detached) {
        if (detached) {
            fail("detached", "false", "true");
        }
    }

    private void validateCreationTimestamp(Long creationTimestamp) {
        if (!expectedCreationTimestamp.get().equals(creationTimestamp)) {
            fail("creation timestamp", expectedCreationTimestamp.get().toString(), creationTimestamp.toString());
        }
    }

    private void fail(String testField, String expected, String actual) {
        throw new TestFailException(
                " The DL's field '" + testField + "' is '" + actual + "' instead of '" + expected + '\''
        );
    }
}
