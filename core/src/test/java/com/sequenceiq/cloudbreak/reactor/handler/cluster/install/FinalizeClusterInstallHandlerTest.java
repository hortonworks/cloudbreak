package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.FINALIZE_CLUSTER_INSTALL_FINISHED_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.recovery.RdsRecoverySetupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class FinalizeClusterInstallHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @Mock
    private RdsRecoverySetupService rdsRecoverySetupService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @InjectMocks
    private FinalizeClusterInstallHandler underTest;

    @Test
    public void testFinalizeClusterInstall() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Event<FinalizeClusterInstallRequest> request = mock(Event.class);
        when(request.getData()).thenReturn(new FinalizeClusterInstallRequest(STACK_ID, ProvisionType.REGULAR));
        StackDto stack = new StackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stack);

        underTest.accept(request);

        verify(clusterHostServiceRunner).createCronForUserHomeCreation(eq(stack), any());
        verify(clusterBuilderService).finalizeClusterInstall(eq(stack));
        verify(rdsRecoverySetupService, never()).removeRecoverRole(eq(STACK_ID));
        verify(parcelService).removeUnusedParcelComponents(eq(stack));
        verify(eventBus).notify(eq(FINALIZE_CLUSTER_INSTALL_FINISHED_EVENT.event()), any(Event.class));
    }
}