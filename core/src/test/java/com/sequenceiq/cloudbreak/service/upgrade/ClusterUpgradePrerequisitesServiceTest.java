package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePrerequisitesServiceTest {

    private static final String STACK_NAME = "stack-name";

    private static final String DAS_SERVICE_TYPE = "DAS";

    @InjectMocks
    private ClusterUpgradePrerequisitesService underTest;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterApi clusterApi;

    @BeforeEach
    void before() {
        lenient().when(stackDto.getName()).thenReturn(STACK_NAME);
    }

    @Test
    void testRemoveDasServiceIfNecessary() throws Exception {
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(true);

        underTest.removeDasServiceIfNecessary(stackDto, "7.2.18");

        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi).stopClouderaManagerService(DAS_SERVICE_TYPE);
        verify(clusterApi).deleteClouderaManagerService(DAS_SERVICE_TYPE);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheRuntimeVersionIsLowerThanTheLimited() throws Exception {
        underTest.removeDasServiceIfNecessary(stackDto, "7.2.17");
        verifyNoInteractions(clusterApiConnectors, clusterApi);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheRuntimeVersionIsNull() throws Exception {
        underTest.removeDasServiceIfNecessary(stackDto, null);
        verifyNoInteractions(clusterApiConnectors, clusterApi);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheServiceIsNotPresentOnTheCluster() throws Exception {
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(false);

        underTest.removeDasServiceIfNecessary(stackDto, "7.2.18");

        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).stopClouderaManagerService(DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).deleteClouderaManagerService(DAS_SERVICE_TYPE);
    }

}