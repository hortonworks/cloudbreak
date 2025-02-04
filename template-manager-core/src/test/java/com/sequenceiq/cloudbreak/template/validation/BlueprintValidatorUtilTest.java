package com.sequenceiq.cloudbreak.template.validation;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.template.utils.HostGroupUtils;

@ExtendWith(MockitoExtension.class)
public class BlueprintValidatorUtilTest {

    @Mock
    private HostGroupUtils hostGroupUtils;

    @InjectMocks
    private BlueprintValidatorUtil underTest;

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
        when(hostGroupUtils.isNotEcsHostGroup(any())).thenReturn(true);

        Exception e = assertThrows(BlueprintValidationException.class, () -> underTest.validateHostGroupsMatch(request, requirements.keySet()));
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
        when(hostGroupUtils.isNotEcsHostGroup(any())).thenReturn(true);

        Exception e = assertThrows(BlueprintValidationException.class, () -> underTest.validateHostGroupsMatch(request, requirements.keySet()));
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

        Exception e = assertThrows(BlueprintValidationException.class, () -> underTest.validateHostGroupCardinality(request, requirements));
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

        Exception e = assertThrows(BlueprintValidationException.class, () -> underTest.validateHostGroupCardinality(request, requirements));
        assertTrue(e.getMessage().contains("master"));
        assertFalse(e.getMessage().contains("worker"));
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

        underTest.validateHostGroupCardinality(request, requirements);
    }

    @Test
    public void testValidateHostGroupsMatchForNotRequiredHybridGroup() {
        Set<HostGroup> request = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("ecs", 2)
        );
        Map<String, InstanceCount> requirements = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.atLeast(3)
        );

        underTest.validateHostGroupCardinality(request, requirements);
    }

}
