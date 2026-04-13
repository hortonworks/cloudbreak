package com.sequenceiq.cloudbreak.converter.v4.stacks.view;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ConfigStalenessV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.common.api.type.ConfigStalenessState;

@ExtendWith(MockitoExtension.class)
class ClusterApiViewToClusterViewV4ResponseConverterTest {

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private HostGroupViewToHostGroupViewV4ResponseConverter hostGroupViewToHostGroupViewV4ResponseConverter;

    @InjectMocks
    private ClusterApiViewToClusterViewV4ResponseConverter clusterApiViewToClusterViewV4ResponseConverterUnderTest;

    @Test
    public void testWhenBlueprintIdIsnull() {
        ClusterApiView testSource = new ClusterApiView();

        ClusterViewV4Response result = clusterApiViewToClusterViewV4ResponseConverterUnderTest.convert(testSource, Set.of());

        assertThat(result.getConfigStaleness())
                .returns(ConfigStalenessState.UP_TO_DATE.name(), ConfigStalenessV4Response::getState)
                .returns(null, ConfigStalenessV4Response::getDetails);
    }

    @Test
    void testConfigStaleness() {
        ClusterApiView testSource = new ClusterApiView();
        testSource.setConfigStalenessState(ConfigStalenessState.STALE);
        testSource.setConfigStalenessDetails("stale");

        ClusterViewV4Response result = clusterApiViewToClusterViewV4ResponseConverterUnderTest.convert(testSource, Set.of());

        assertThat(result.getConfigStaleness())
            .returns(ConfigStalenessState.STALE.name(), ConfigStalenessV4Response::getState)
            .returns("stale", ConfigStalenessV4Response::getDetails);
    }
}