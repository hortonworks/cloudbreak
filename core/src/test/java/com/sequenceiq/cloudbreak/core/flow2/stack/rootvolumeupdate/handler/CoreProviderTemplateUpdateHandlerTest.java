package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.ProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.RootDiskValidationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class CoreProviderTemplateUpdateHandlerTest {

    @Mock
    private RootDiskValidationService rootDiskValidationService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private CoreProviderTemplateUpdateHandler underTest;

    private ProviderTemplateUpdateHandlerRequest request;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setDiskType(DiskType.ADDITIONAL_DISK);
        diskUpdateRequest.setGroup("executor");
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setSize(100);
        request = new ProviderTemplateUpdateHandlerRequest(
                EventSelectorUtil.selector(ProviderTemplateUpdateHandlerRequest.class),
                1L,
                cloudContext,
                cloudCredential,
                cloudStack,
                "gp2",
                "executor",
                100,
                DiskType.ADDITIONAL_DISK.name()
        );
    }

    @Test
    void testUpdateLaunchTemplateHandlerSuccess() throws Exception {
        when(stackDto.getId()).thenReturn(1L);
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        when(cloudContext.getPlatformVariant()).thenReturn(new CloudPlatformVariant("AWS", "AWS"));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
        assertEquals(1L, response.getResourceId());
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.selector(), response.getSelector());
        verify(resourceConnector).update(eq(authenticatedContext), eq(cloudStack), anyList(), eq(UpdateType.PROVIDER_TEMPLATE_UPDATE),
                eq(Optional.empty()));
    }

    @Test
    void testSelector() {
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_EVENT.selector(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        CoreProviderTemplateUpdateFailureEvent result = (CoreProviderTemplateUpdateFailureEvent) underTest.defaultFailureEvent(1L, new Exception("Test"), null);
        assertEquals(1L, result.getResourceId());
        assertEquals("Exception in Launch Template Update Handler", result.getFailedPhase());
        assertEquals("Test", result.getException().getMessage());
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT.selector(), result.getSelector());
    }
}
