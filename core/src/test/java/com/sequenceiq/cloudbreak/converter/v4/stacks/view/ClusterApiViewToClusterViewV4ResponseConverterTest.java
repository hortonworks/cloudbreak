package com.sequenceiq.cloudbreak.converter.v4.stacks.view;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;

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
        assertDoesNotThrow(() -> clusterApiViewToClusterViewV4ResponseConverterUnderTest.convert(testSource, Set.of()));
    }
}