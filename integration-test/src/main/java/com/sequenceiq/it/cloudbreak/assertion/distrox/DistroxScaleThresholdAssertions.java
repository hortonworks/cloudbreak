package com.sequenceiq.it.cloudbreak.assertion.distrox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroxScaleThresholdAssertions implements Assertion<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxScaleThresholdAssertions.class);

    private final String hostGroupName;

    private final int scaleUpTarget;

    private final long threshold;

    public DistroxScaleThresholdAssertions(String hostGroupName, int scaleUpTarget, long threshold) {
        this.hostGroupName = hostGroupName;
        this.scaleUpTarget = scaleUpTarget;
        this.threshold = threshold;
    }

    private String getHostGroup() {
        return Arrays.stream(HostGroupType.values())
                .filter(hostGroup -> hostGroup.name().equalsIgnoreCase(hostGroupName))
                .findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no HostGroupType for '%s' name!", hostGroupName)))
                .name();
    }

    private List<String> getInstanceIds(DistroXTestDto testDto) {
        Optional<InstanceGroupV4Response> instanceGroup = testDto.getResponse().getInstanceGroups().stream()
                .filter(instanceGroups -> getHostGroup().equalsIgnoreCase(instanceGroups.getName()))
                .filter(instanceGroups -> instanceGroups.getMetadata().stream()
                        .anyMatch(instanceMetaData -> Objects.nonNull(instanceMetaData.getInstanceId())))
                .findAny();
        if (instanceGroup.isPresent()) {
            return instanceGroup.get().getMetadata().stream()
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            throw new TestFailException(String.format("Can't find valid instance group with this '%s' name!", hostGroupName));
        }
    }

    private float getScalingThreshold() {
        float scalingPercentage = (float) threshold / 100;
        return scaleUpTarget * scalingPercentage;
    }

    @Override
    public DistroXTestDto doAssertion(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        int instanceCount = getInstanceIds(testDto).size();

        assertThat(
                String.format("Available Compute node count '%d' is NOT match with the required (at least) scale percentage '%d'.",
                        instanceCount, threshold),
                (float) instanceCount,
                greaterThanOrEqualTo(getScalingThreshold()));

        return testDto;
    }
}