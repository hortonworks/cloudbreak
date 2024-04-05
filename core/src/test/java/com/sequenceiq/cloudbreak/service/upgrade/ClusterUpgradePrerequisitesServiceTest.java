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

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePrerequisitesServiceTest {

    private static final String STACK_NAME = "stack-name";

    private static final String DAS_SERVICE_TYPE = "DAS";

    @InjectMocks
    private ClusterUpgradePrerequisitesService underTest;

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
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(true);

        underTest.removeDasServiceIfNecessary(stackDto, clusterApi, "7.2.18");

        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi).stopClouderaManagerService(DAS_SERVICE_TYPE, true);
        verify(clusterApi).deleteClouderaManagerService(DAS_SERVICE_TYPE);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheRuntimeVersionIsLowerThanTheLimited() throws Exception {
        underTest.removeDasServiceIfNecessary(stackDto, clusterApi, "7.2.17");
        verifyNoInteractions(clusterApi);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheRuntimeVersionIsNull() throws Exception {
        underTest.removeDasServiceIfNecessary(stackDto, clusterApi,  null);
        verifyNoInteractions(clusterApi);
    }

    @Test
    void testRemoveDasServiceIfNecessaryShouldNotRemoveTheServiceIfTheServiceIsNotPresentOnTheCluster() throws Exception {
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(false);

        underTest.removeDasServiceIfNecessary(stackDto, clusterApi, "7.2.18");

        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).stopClouderaManagerService(DAS_SERVICE_TYPE, true);
        verify(clusterApi, times(0)).deleteClouderaManagerService(DAS_SERVICE_TYPE);
    }

}