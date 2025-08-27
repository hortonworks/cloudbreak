package com.sequenceiq.distrox.v1.distrox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;

@ExtendWith(MockitoExtension.class)
class HybridClusterTemplateValidatorTest {

    @InjectMocks
    private HybridClusterTemplateValidator underTest;

    @Mock
    private ClusterTemplateView clusterTemplateView;

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void userManagedClusterTemplateView(Boolean hybridEnvironment) {
        when(clusterTemplateView.getStatus()).thenReturn(ResourceStatus.USER_MANAGED);
        boolean result = underTest.shouldPopulate(clusterTemplateView, hybridEnvironment);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(ClusterTemplateV4Type.class)
    void nullHybridEnv(ClusterTemplateV4Type type) {
        when(clusterTemplateView.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        lenient().when(clusterTemplateView.getType()).thenReturn(type);
        boolean result = underTest.shouldPopulate(clusterTemplateView, null);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void defaultHybridClusterTemplateView(boolean hybridEnvironment) {
        when(clusterTemplateView.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        lenient().when(clusterTemplateView.getType()).thenReturn(ClusterTemplateV4Type.HYBRID_DATAENGINEERING_HA);
        boolean result = underTest.shouldPopulate(clusterTemplateView, hybridEnvironment);
        assertThat(result).isEqualTo(hybridEnvironment);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void defaultNonHybridClusterTemplateView(boolean hybridEnvironment) {
        when(clusterTemplateView.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        lenient().when(clusterTemplateView.getType()).thenReturn(ClusterTemplateV4Type.DATAENGINEERING_HA);
        boolean result = underTest.shouldPopulate(clusterTemplateView, hybridEnvironment);
        assertThat(result).isEqualTo(!hybridEnvironment);
    }

}
