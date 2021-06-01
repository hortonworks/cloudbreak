package com.sequenceiq.cloudbreak.service.spot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public class SpotInstanceUsageConditionTest {

    private final SpotInstanceUsageCondition underTest = new SpotInstanceUsageCondition();

    @Test
    public void testIsStackRunsOnSpotInstancesShouldReturnFalseWhenTheCloudPlatformIdNotSupported() {
        Stack stack = createStack("AZURE", Set.of(createInstanceGroup(0), createInstanceGroup(null)));
        assertFalse(underTest.isStackRunsOnSpotInstances(stack));
    }

    @Test
    public void testIsStackRunsOnSpotInstancesShouldReturnFalseWhenTheStackIsNotUsingSpotInstances() {
        Stack stack = createStack("AWS", Set.of(createInstanceGroup(0), createInstanceGroup(null)));
        assertFalse(underTest.isStackRunsOnSpotInstances(stack));
    }

    @Test
    public void testIsStackRunsOnSpotInstancesShouldReturnTrueWhenTheStackIsUsingSpotInstances() {
        Stack stack = createStack("AWS", Set.of(createInstanceGroup(100)));
        assertTrue(underTest.isStackRunsOnSpotInstances(stack));
    }

    private Stack createStack(String cloudPlatform, Set<InstanceGroup> instanceGroups) {
        Stack stack = new Stack();
        stack.setCloudPlatform(cloudPlatform);
        stack.setInstanceGroups(instanceGroups);
        return stack;
    }

    private InstanceGroup createInstanceGroup(Integer spotPercentage) {
        Template template = new Template();
        template.setAttributes(new Json(spotPercentage != null ? Map.of(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, spotPercentage) : Map.of()));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

}