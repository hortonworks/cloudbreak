package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.refreshdns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateDnsHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Mock
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private UpdateDnsHandler underTest;

    @Test
    public void testDoAccept() {
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        UpdateDnsRequest request = new UpdateDnsRequest(stackView, mock(CloudContext.class), mock(CloudCredential.class), mock(CloudConnector.class),
                mock(CloudStack.class));
        HandlerEvent<UpdateDnsRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertEquals(SkuMigrationFinished.class, selectable.getClass());
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForLoadBalancers(stackDto);
        verify(clusterPublicEndpointManagementService, times(1)).registerLoadBalancerWithFreeIPA(stackDto.getStack());
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(gatewayPublicEndpointManagementService).updateDnsEntryForLoadBalancers(any());

        UpdateDnsRequest request = new UpdateDnsRequest(mock(StackView.class), mock(CloudContext.class), mock(CloudCredential.class), mock(CloudConnector.class),
                mock(CloudStack.class));
        HandlerEvent<UpdateDnsRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);
        assertNotNull(result);
        assertEquals(SkuMigrationFailedEvent.class, result.getClass());
        assertEquals(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), result.getSelector());
    }

}