package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.handler;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaProviderTemplateUpdateHandlerTest {

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaProviderTemplateUpdateHandler underTest;

    private FreeIpaProviderTemplateUpdateHandlerRequest request;

    @Mock
    private Stack stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        String selector = EventSelectorUtil.selector(FreeIpaProviderTemplateUpdateHandlerRequest.class);
        request = new FreeIpaProviderTemplateUpdateHandlerRequest(selector, 1L, "test", cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testUpdateLaunchTemplateHandlerSuccess() throws Exception {
        when(stack.getId()).thenReturn(1L);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(cloudContext.getPlatformVariant()).thenReturn(new CloudPlatformVariant("AWS", "AWS"));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
        assertEquals(1L, response.getResourceId());
        assertEquals(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.selector(), response.getSelector());
        verify(resourceConnector).update(eq(authenticatedContext), eq(cloudStack), anyList(), eq(UpdateType.PROVIDER_TEMPLATE_UPDATE),
                eq(Optional.empty()));
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(FreeIpaProviderTemplateUpdateHandlerRequest.class), underTest.selector());
    }
}
