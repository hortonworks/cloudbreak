package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaServicesStopServiceTest {

    private static final long DELAY = 10L;

    @Mock
    private StackService stackService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private DelayedExecutorService delayedExecutorService;

    @InjectMocks
    private FreeIpaServicesStopService underTest;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private InstanceMetaData instance1;

    @Mock
    private InstanceMetaData instance2;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(underTest, "delayInSec", DELAY);
    }

    @Test
    public void testStopServicesSingleInstance() throws ExecutionException, InterruptedException, CloudbreakOrchestratorFailedException {
        // Mock data for a single instance
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instance1));

        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(instance1.getDiscoveryFQDN()).thenReturn("instance1.fqdn");

        // Call the method
        underTest.stopServices(1L);

        // Verify that the stack service is called to retrieve the stack
        verify(stackService, times(1)).getByIdWithListsInTransaction(anyLong());

        // Verify that the host orchestrator is called to stop services on the single instance
        verify(hostOrchestrator, times(1))
                .runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("instance1.fqdn")), eq("ipactl stop"));

        // Verify that no delayed executor is used since there's only one instance
        verify(delayedExecutorService, times(0)).runWithDelay(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testStopServicesMultipleInstances() throws ExecutionException, InterruptedException, CloudbreakOrchestratorFailedException {
        // Mock data for two instances
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instance1, instance2));

        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(instance1.getDiscoveryFQDN()).thenReturn("instance1.fqdn");
        when(instance2.getDiscoveryFQDN()).thenReturn("instance2.fqdn");
        doAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return null;
        }).when(delayedExecutorService).runWithDelay(any(Runnable.class), eq(DELAY), eq(TimeUnit.SECONDS));

        // Call the method
        underTest.stopServices(1L);

        // Verify the first instance is stopped immediately
        verify(hostOrchestrator, times(1))
                .runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("instance1.fqdn")), eq("ipactl stop"));

        // Verify that the second instance is stopped after a delay
        verify(delayedExecutorService, times(1))
                .runWithDelay(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testStopServicesNoInstances() throws ExecutionException, InterruptedException, CloudbreakOrchestratorFailedException {
        // Mock data for no instances
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of());

        // Call the method
        underTest.stopServices(1L);

        // Verify that no services are stopped
        verify(hostOrchestrator, times(0)).runCommandOnHosts(any(), any(), anyString());

        // Verify that no delayed executor is used since there are no instances
        verify(delayedExecutorService, times(0)).runWithDelay(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testStopServicesExceptionDuringStop() throws ExecutionException, InterruptedException, CloudbreakOrchestratorFailedException {
        // Mock data for a single instance
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instance1));

        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(instance1.getDiscoveryFQDN()).thenReturn("instance1.fqdn");

        // Simulate an exception when stopping services
        doThrow(new CloudbreakOrchestratorFailedException("Failed")).when(hostOrchestrator)
                .runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("instance1.fqdn")), eq("ipactl stop"));

        // Call the method
        underTest.stopServices(1L);

        // Verify that the stack service is called to retrieve the stack
        verify(stackService, times(1)).getByIdWithListsInTransaction(anyLong());

        // Verify that the host orchestrator is called and throws the exception
        verify(hostOrchestrator, times(1))
                .runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("instance1.fqdn")), eq("ipactl stop"));
    }
}
