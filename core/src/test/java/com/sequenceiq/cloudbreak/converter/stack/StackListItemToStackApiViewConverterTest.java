package com.sequenceiq.cloudbreak.converter.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.common.api.type.ConfigStalenessState;

@ExtendWith(MockitoExtension.class)
class StackListItemToStackApiViewConverterTest {

    @InjectMocks
    private StackListItemToStackApiViewConverter underTest;

    @Mock
    private StackListItem item;

    private Map<Long, Integer> stackInstanceCounts;

    private HostGroupView hostGroupView;

    private InstanceGroupView instanceGroupView;

    @BeforeEach
    void setUp() {
        stackInstanceCounts = new HashMap<>();
        hostGroupView = new HostGroupView();
        instanceGroupView = new InstanceGroupView();
    }

    @Test
    void convertEmpty() {
        StackApiView result = underTest.convert(item, stackInstanceCounts, null, null);

        assertThat(result.getCluster().getHostGroups()).isEmpty();
        assertThat(result.getInstanceGroups()).isEmpty();
        assertThat(result.getCluster().getConfigStalenessState()).isEqualTo(ConfigStalenessState.UP_TO_DATE);
        assertThat(result.getCluster().getConfigStalenessDetails()).isNull();
    }

    @Test
    void convertValues() {
        when(item.getConfigStalenessState()).thenReturn(ConfigStalenessState.STALE);
        when(item.getConfigStalenessDetails()).thenReturn("stale");

        StackApiView result = underTest.convert(item, stackInstanceCounts, List.of(hostGroupView), List.of(instanceGroupView));

        assertThat(result.getCluster().getHostGroups()).hasSize(1);
        assertThat(result.getInstanceGroups()).hasSize(1);
        assertThat(result.getCluster().getConfigStalenessState()).isEqualTo(ConfigStalenessState.STALE);
        assertThat(result.getCluster().getConfigStalenessDetails()).isEqualTo("stale");
    }

}
