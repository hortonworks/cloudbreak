package com.sequenceiq.cloudbreak.template.validation;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil.validateHostGroupCardinality;
import static com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil.validateHostGroupsMatch;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

public class BlueprintValidatorUtilTest {

    @Test
    public void rejectsMissingHostGroup() {
        Set<HostGroup> request = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 2)
        );
        Map<String, InstanceCount> requirements = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.atLeast(3),
                "compute", InstanceCount.ZERO_OR_MORE
        );

        Exception e = assertThrows(BlueprintValidationException.class, () -> validateHostGroupsMatch(request, requirements.keySet()));
        assertTrue(e.getMessage().contains("compute"));
        Set.of("master", "worker").forEach(hostGroup -> assertFalse(e.getMessage().contains(hostGroup), hostGroup));
    }

    @Test
    public void rejectsUnknownHostGroup() {
        Set<HostGroup> request = Set.of(
                hostGroup(">OK<", 1),
                hostGroup("unknown", 2)
        );
        Map<String, InstanceCount> requirements = Map.of(
                ">OK<", InstanceCount.ONE_OR_MORE
        );

        Exception e = assertThrows(BlueprintValidationException.class, () -> validateHostGroupsMatch(request, requirements.keySet()));
        assertTrue(e.getMessage().contains("unknown"));
        assertFalse(e.getMessage().contains(">OK<"));
    }

    @Test
    public void rejectsTooFewNodes() {
        Set<HostGroup> request = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 2)
        );
        Map<String, InstanceCount> requirements = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.atLeast(3)
        );

        Exception e = assertThrows(BlueprintValidationException.class, () -> validateHostGroupCardinality(request, requirements));
        assertTrue(e.getMessage().contains("worker"));
        assertFalse(e.getMessage().contains("master"));
    }

    @Test
    public void rejectsTooManyNodes() {
        Set<HostGroup> request = Set.of(
                hostGroup("master", 2),
                hostGroup("worker", 4)
        );
        Map<String, InstanceCount> requirements = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.atLeast(3)
        );

        Exception e = assertThrows(BlueprintValidationException.class, () -> validateHostGroupCardinality(request, requirements));
        assertTrue(e.getMessage().contains("master"));
        Assert.assertFalse(e.getMessage().contains("worker"));
    }

    @Test
    public void acceptsNodeCountWithinLimits() {
        Set<HostGroup> request = Set.of(
                hostGroup("master", 2),
                hostGroup("worker", 4)
        );
        Map<String, InstanceCount> requirements = Map.of(
                "master", InstanceCount.of(1, 2),
                "worker", InstanceCount.atLeast(3)
        );

        validateHostGroupCardinality(request, requirements);
    }

}
