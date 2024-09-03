package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

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

    private static final String ZEPPELIN_SERVICE_TYPE = "ZEPPELIN";

    private static final Map<String, String> SERVICES_TO_REMOVE = Map.of(
            DAS_SERVICE_TYPE, "7.2.18",
            ZEPPELIN_SERVICE_TYPE, "7.3.1");

    @InjectMocks
    private ClusterUpgradePrerequisitesService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ServicesToRemoveBeforeUpgrade servicesToRemoveBeforeUpgrade;

    @BeforeEach
    void before() {
        lenient().when(stackDto.getName()).thenReturn(STACK_NAME);
        when(servicesToRemoveBeforeUpgrade.getServicesToRemove()).thenReturn(SERVICES_TO_REMOVE);
    }

    @Test
    void testRemoveIncompatibleServices() throws Exception {
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(true);
        when(clusterApi.isServicePresent(STACK_NAME, ZEPPELIN_SERVICE_TYPE)).thenReturn(true);

        underTest.removeIncompatibleServices(stackDto, clusterApi, "7.3.1");

        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi).isServicePresent(STACK_NAME, ZEPPELIN_SERVICE_TYPE);
        verify(clusterApi).stopClouderaManagerService(DAS_SERVICE_TYPE, true);
        verify(clusterApi).stopClouderaManagerService(ZEPPELIN_SERVICE_TYPE, true);
        verify(clusterApi).deleteClouderaManagerService(DAS_SERVICE_TYPE);
        verify(clusterApi).deleteClouderaManagerService(ZEPPELIN_SERVICE_TYPE);
    }

    @Test
    void testRemoveIncompatibleServicesShouldNotRemoveTheServicesIfTheRuntimeVersionIsLowerThanTheLimited() {
        underTest.removeIncompatibleServices(stackDto, clusterApi, "7.2.17");
        verifyNoInteractions(clusterApi);
    }

    @Test
    void testRemoveIncompatibleServicesShouldOnlyRemoveTheDasServicesIfTheRuntimeVersionIsLowerThanTheLimited() throws Exception {
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(true);

        underTest.removeIncompatibleServices(stackDto, clusterApi, "7.3.0");

        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).isServicePresent(STACK_NAME, ZEPPELIN_SERVICE_TYPE);
        verify(clusterApi).stopClouderaManagerService(DAS_SERVICE_TYPE, true);
        verify(clusterApi, times(0)).stopClouderaManagerService(ZEPPELIN_SERVICE_TYPE, true);
        verify(clusterApi).deleteClouderaManagerService(DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).deleteClouderaManagerService(ZEPPELIN_SERVICE_TYPE);
    }

    @Test
    void testRemoveIncompatibleServicesShouldNotRemoveTheServiceIfTheRuntimeVersionIsNull() {
        underTest.removeIncompatibleServices(stackDto, clusterApi,  null);
        verifyNoInteractions(clusterApi);
    }

    @Test
    void testRemoveIncompatibleServicesShouldNotRemoveTheServiceIfTheServiceIsNotPresentOnTheCluster() throws Exception {
        when(clusterApi.isServicePresent(STACK_NAME, DAS_SERVICE_TYPE)).thenReturn(false);
        when(clusterApi.isServicePresent(STACK_NAME, ZEPPELIN_SERVICE_TYPE)).thenReturn(false);

        underTest.removeIncompatibleServices(stackDto, clusterApi, "7.3.1");

        verify(clusterApi).isServicePresent(STACK_NAME, DAS_SERVICE_TYPE);
        verify(clusterApi).isServicePresent(STACK_NAME, ZEPPELIN_SERVICE_TYPE);
        verify(clusterApi, times(0)).stopClouderaManagerService(DAS_SERVICE_TYPE, true);
        verify(clusterApi, times(0)).stopClouderaManagerService(ZEPPELIN_SERVICE_TYPE, true);
        verify(clusterApi, times(0)).deleteClouderaManagerService(DAS_SERVICE_TYPE);
        verify(clusterApi, times(0)).deleteClouderaManagerService(ZEPPELIN_SERVICE_TYPE);
    }

}