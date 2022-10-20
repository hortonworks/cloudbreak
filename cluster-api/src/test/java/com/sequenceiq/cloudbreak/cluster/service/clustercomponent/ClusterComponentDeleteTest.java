package com.sequenceiq.cloudbreak.cluster.service.clustercomponent;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.cluster.common.TestUtil;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

class ClusterComponentDeleteTest extends ClusterComponentConfigProviderTestBase {

    @Test
    @DisplayName("Test deleteClusterComponents when input set is null then no deletion should happen")
    void testDeleteClusterComponentsForNull() {
        getUnderTest().deleteClusterComponents(null);

        verify(getMockComponentRepository(), never()).deleteAll(any());
    }

    @Test
    @DisplayName("Test deleteClusterComponents when input set is empty then no deletion should happen")
    void testDeleteClusterComponentsForEmpty() {
        getUnderTest().deleteClusterComponents(emptySet());

        verify(getMockComponentRepository(), never()).deleteAll(any());
    }

    @ParameterizedTest
    @EnumSource(ComponentType.class)
    @DisplayName("Test deleteClusterComponents when input set is not empty then deletion should happen.")
    void testDeleteClusterComponentsForProperInput(ComponentType componentType) {
        ClusterComponent clusterComponent = new ClusterComponent(componentType, componentType.name() + "_component", MOCK_JSON, TestUtil.cluster());

        getUnderTest().deleteClusterComponents(Set.of(clusterComponent));

        verify(getMockComponentRepository(), times(1)).deleteAll(any());
    }

}