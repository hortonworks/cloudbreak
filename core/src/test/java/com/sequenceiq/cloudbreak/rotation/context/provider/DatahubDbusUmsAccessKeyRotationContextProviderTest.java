package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
public class DatahubDbusUmsAccessKeyRotationContextProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    @Mock
    private StackDtoService stackService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private SecretRotationSaltService saltService;

    @InjectMocks
    private DatahubDbusUmsAccessKeyRotationContextProvider underTest;

    @BeforeEach
    void setup() {
        lenient().when(componentConfigProviderService.getTelemetry(any())).thenReturn(new Telemetry());
        lenient().when(entitlementService.useDataBusCNameEndpointEnabled(any())).thenReturn(Boolean.TRUE);
        lenient().when(dataBusEndpointProvider.getDataBusEndpoint(any(), anyBoolean())).thenReturn("");
    }

    @Test
    void testGetContexts() {
        when(stackService.getByCrn(any())).thenReturn(new StackDto());
        assertEquals(2, underTest.getContexts("").size());
    }

    @Test
    void testCustomJobRotateDatabusCredReadFailure() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabusCredential()).thenReturn("invalidjson");
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts("").get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to read Databus credential from internal database.");

        verifyNoInteractions(saltService);
    }

    @Test
    void testCustomJobRotateRefreshPillarFailure() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabusCredential()).thenReturn("{\"privateKey\":\"anything\"}");
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        doThrow(new CloudbreakOrchestratorFailedException("failed")).when(saltService).updateSaltPillar(any(), any(), any());
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts("").get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to refresh Databus relevant salt pillars.");

        verify(saltService, times(1)).updateSaltPillar(any(), any(), any());
        verify(saltService, never()).executeSaltState(any(), any(), any());
        verify(clusterApiConnectors, never()).getConnector(any(StackDtoDelegate.class));
    }

    @Test
    void testCustomJobRotateExecuteSaltStateFailure() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabusCredential()).thenReturn("{\"privateKey\":\"anything\"}");
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        doNothing().when(saltService).updateSaltPillar(any(), any(), any());
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        doThrow(new CloudbreakOrchestratorFailedException("failed")).when(saltService).executeSaltState(any(), any(), any());
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts("").get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to execute Databus relevant salt states.");

        verify(saltService, times(1)).updateSaltPillar(any(), any(), any());
        verify(saltService, times(1)).executeSaltState(any(), any(), any());
        verify(clusterApiConnectors, never()).getConnector(any(StackDtoDelegate.class));
    }

    @Test
    void testCustomJobRotateMgmtRestartFailure() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabusCredential()).thenReturn("{\"privateKey\":\"anything\"}");
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        doNothing().when(saltService).updateSaltPillar(any(), any(), any());
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        doNothing().when(saltService).executeSaltState(any(), any(), any());
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        doThrow(new CloudbreakServiceException("failed")).when(clusterModificationService).restartMgmtServices();
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts("").get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to restart MGMT services to update them with Dbus credential.");

        verify(saltService, times(1)).updateSaltPillar(any(), any(), any());
        verify(saltService, times(1)).executeSaltState(any(), any(), any());
        verify(clusterModificationService, times(1)).restartMgmtServices();
    }

    @Test
    void testCustomJobRotateAndRollbackSuccess() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabusCredential()).thenReturn("{\"privateKey\":\"anything\"}");
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        doNothing().when(saltService).updateSaltPillar(any(), any(), any());
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        doNothing().when(saltService).executeSaltState(any(), any(), any());
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        doNothing().when(clusterModificationService).restartMgmtServices();
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts("").get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertTrue(customJobRotationContext.getRollbackJob().isPresent());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRollbackJob().get().run());

        verify(saltService, times(2)).updateSaltPillar(any(), any(), any());
        verify(saltService, times(2)).executeSaltState(any(), any(), any());
        verify(clusterModificationService, times(2)).restartMgmtServices();
    }
}
