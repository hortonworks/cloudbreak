package com.sequenceiq.it.cloudbreak.assertion.distrox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class AvailabilityZoneAssertion implements Assertion<DistroXTestDto, CloudbreakClient> {

    @Override
    public DistroXTestDto doAssertion(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if (testDto.getCloudPlatform() != CloudPlatform.AWS) {
            return testDto;
        }
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        checkAwsNativeVariantIfMultiAzEnabled(testContext, testDto);
        instanceGroups.stream().forEach(instanceGroup -> {
            if (instanceGroup.getAvailabilityZones() == null || instanceGroup.getAvailabilityZones().size() <= 1) {
                return;
            }
            Set<InstanceMetaDataV4Response> instances = instanceGroup.getMetadata();
            if (instances.size() == 0) {
                return;
            }
            Map<String, Integer> instanceNoByAvailabilityZone = getSubtotalByAvailabilityZone(instances);
            int max = getMax(instanceNoByAvailabilityZone);
            boolean result = allAvailabilityZoneHasNearlyTheSameInstanceCount(instanceNoByAvailabilityZone, max);
            if (!result) {
                throw new TestFailException("Instances are not divided into multiple available zone correctly " + instanceNoByAvailabilityZone.toString());
            }
        });
        return testDto;
    }

    private void checkAwsNativeVariantIfMultiAzEnabled(TestContext testContext, DistroXTestDto testDto) {
        if ("AWS_NATIVE".equals(testContext.getCloudProvider().getVariant()) &&
                !testContext.getCloudProvider().getVariant().equals(testDto.getResponse().getVariant())) {
            throw new TestFailException("The Multi-AZ enabled but the stack variant is not AWS_NATIVE: " + testDto.getResponse().getVariant());
        }
    }

    private boolean allAvailabilityZoneHasNearlyTheSameInstanceCount(Map<String, Integer> instanceNoByAvailabilityZone, int max) {
        return instanceNoByAvailabilityZone.values().stream().allMatch(i -> (max - i) <= 1);
    }

    private int getMax(Map<String, Integer> alma) {
        return alma.entrySet().stream().mapToInt(Map.Entry::getValue).max().getAsInt();
    }

    @NotNull
    private Map<String, Integer> getSubtotalByAvailabilityZone(Set<InstanceMetaDataV4Response> instances) {
        Map<String, Integer> alma = new HashMap<String, Integer>();
        instances.forEach(instance -> {
            Integer result = alma.get(instance.getAvailabilityZone());
            if (result == null) {
                alma.put(instance.getAvailabilityZone(), 1);
            } else {
                alma.put(instance.getAvailabilityZone(), result + 1);
            }
        });
        return alma;
    }
}
