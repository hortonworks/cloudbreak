package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class GenerateAlternativeCertificateHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @InjectMocks
    private GenerateAlternativeCertificateHandler underTest;

    @Mock
    private StackDto stackDto;

    private UpdateSslConfigEvent event;

    @BeforeEach
    void setUp() {
        event = new UpdateSslConfigEvent(GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.name(), 1L, "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testGenerateAlternativeCertAndSaveForStackHandlerSuccess() {
        when(stackDtoService.getById(event.getResourceId())).thenReturn(stackDto);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackDto.getStack())).thenReturn(true);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(1L, response.getResourceId());
        verify(gatewayPublicEndpointManagementService, times(1)).generateAlternativeCertAndSaveForStack(stackDto);
        assertEquals(FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(), response.getSelector());
    }

    @Test
    void testGenerateAlternativeCertAndSaveForStackHandlerWhenCertIsDisabled() {
        when(stackDtoService.getById(event.getResourceId())).thenReturn(stackDto);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackDto.getStack())).thenReturn(false);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(1L, response.getResourceId());
        verify(gatewayPublicEndpointManagementService, never()).generateAlternativeCertAndSaveForStack(any());
        assertEquals(FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(), response.getSelector());
    }

    @Test
    void testGenerateAlternativeCertAndSaveForStackHandlerFailure() {
        when(stackDtoService.getById(event.getResourceId())).thenReturn(stackDto);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackDto.getStack())).thenReturn(true);
        doThrow(new CloudbreakServiceException("Cert generation failed"))
                .when(gatewayPublicEndpointManagementService).generateAlternativeCertAndSaveForStack(stackDto);

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(event.getResourceId(), selectable.getResourceId());
        assertEquals(FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.name(), selectable.selector());
        assertEquals("Cert generation failed", selectable.getException().getMessage());
    }
}
