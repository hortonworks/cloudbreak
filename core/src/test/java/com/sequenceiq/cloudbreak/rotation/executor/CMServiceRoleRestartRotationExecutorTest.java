package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class CMServiceRoleRestartRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String SERVICE_TYPE = "serviceType";

    private static final String ROLE_TYPE = "roleType";

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private ClusterStatusService clusterStatusService;

    @InjectMocks
    private CMServiceRoleRestartRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() {
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        lenient().when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        lenient().when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
    }

    @Test
    public void testRotation() throws Exception {
        underTest.executeRotate(createContext(), null);
        verify(clusterModificationService, times(1))
                .restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);
    }

    @Test
    public void testRotationFailure() {
        doThrow(new RuntimeException("something")).when(clusterModificationService).restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(), null));

        verify(clusterModificationService, times(1))
                .restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);
    }

    @Test
    public void testRollbackFailure() {
        doThrow(new RuntimeException("something")).when(clusterModificationService).restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(), null));

        verify(clusterModificationService, times(1))
                .restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);
    }

    @Test
    public void testFinalization() throws Exception {
        underTest.executeFinalize(createContext(), null);

        verify(clusterModificationService, times(0))
                .restartServiceRoleByType(SERVICE_TYPE, ROLE_TYPE);
    }

    @Test
    public void testPreValidate() {
        when(clusterStatusService.isServiceRunningByType(any(), any())).thenReturn(true);

        underTest.executePreValidation(createContext());
        verify(clusterStatusService).isServiceRunningByType(any(), any());
    }

    @Test
    public void testPreValidateServiceNotRunning() {
        when(clusterStatusService.isServiceRunningByType(any(), any())).thenReturn(false);

        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(createContext()));
        verify(clusterStatusService).isServiceRunningByType(any(), any());
    }

    private RotationContext createContext() {
        return CMServiceRoleRestartRotationContext.builder()
                .withResourceCrn(RESOURCE_CRN)
                .withServiceType(SERVICE_TYPE)
                .withRoleType(ROLE_TYPE)
                .build();
    }
}