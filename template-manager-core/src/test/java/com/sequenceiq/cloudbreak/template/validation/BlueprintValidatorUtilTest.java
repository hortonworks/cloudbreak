package com.sequenceiq.cloudbreak.template.validation;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
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

    @ParameterizedTest
    @MethodSource("testValidateHostGroupNodeCountsSource")
    public void testValidateHostGroupNodeCounts(int nodeCount, boolean valid) {
        String hostGroupName = "kraft";
        Predicate<Integer> nodeCountAtLeastThree = i -> i >= 3;
        Predicate<Integer> nodeCountIsOdd = i -> i % 2 == 1;
        Predicate<Integer> nodeCountIsZero = i -> i == 0;
        String errorMessage = "The kraft host group must have 0 nodes or at least 3 nodes with an odd number of nodes.";
        Map<String, Map.Entry<Predicate<Integer>, String>> hostGroupRestrictions = Map.of(
                hostGroupName,
                Map.entry((nodeCountAtLeastThree.and(nodeCountIsOdd)).or(nodeCountIsZero), errorMessage));
        HostGroup kraftHostGroup = new HostGroup();
        kraftHostGroup.setName(hostGroupName);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(hostGroupName);
        instanceGroup.setInstanceMetaData(Stream.generate(InstanceMetaData::new)
                .limit(nodeCount)
                .collect(Collectors.toSet()));
        kraftHostGroup.setInstanceGroup(instanceGroup);
        Set<HostGroup> hostGroups = Set.of(kraftHostGroup);

        if (valid) {
            assertDoesNotThrow(() -> underTest.validateHostGroupNodeCounts(hostGroups, hostGroupRestrictions));
        } else {
            BlueprintValidationException exception = assertThrows(BlueprintValidationException.class, () ->
                    underTest.validateHostGroupNodeCounts(hostGroups, hostGroupRestrictions));
            assertEquals(errorMessage, exception.getMessage());
        }
    }

    private static Stream<Arguments> testValidateHostGroupNodeCountsSource() {
        return Stream.of(
                Arguments.of(0, true),
                Arguments.of(1, false),
                Arguments.of(2, false),
                Arguments.of(3, true),
                Arguments.of(4, false),
                Arguments.of(5, true)
        );
    }
}
