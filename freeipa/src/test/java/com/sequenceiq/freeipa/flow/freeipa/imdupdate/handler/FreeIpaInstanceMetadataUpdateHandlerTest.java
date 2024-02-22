package com.sequenceiq.freeipa.flow.freeipa.imdupdate.handler;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateResult;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
public class FreeIpaInstanceMetadataUpdateHandlerTest {

    private static final FreeIpaInstanceMetadataUpdateRequest REQUEST = new FreeIpaInstanceMetadataUpdateRequest(
            new CloudContext.Builder().withPlatform("AWS").build(), null, null, IMDS_HTTP_TOKEN_REQUIRED);

    @Mock
    private CloudPlatformConnectors connectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private FreeIpaInstanceMetadataUpdateHandler underTest;

    @Test
    void testAccept() throws Exception {
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenReturn(
                List.of(new CloudResourceStatus(null, null)));
        mockConnectors(resourceConnector);
        doNothing().when(stackUpdater).updateSupportedImdsVersion(any(), any());

        underTest.accept(new Event<>(REQUEST));

        verify(eventBus).notify(eq(FreeIpaInstanceMetadataUpdateResult.class.getSimpleName().toUpperCase()), any());
        verify(resourceConnector).update(any(), any(), any(), eq(UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED), any());
        verify(stackUpdater).updateSupportedImdsVersion(any(), any());
    }

    @Test
    void testFailure() throws Exception {
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenThrow(new CloudbreakServiceException("fail"));
        mockConnectors(resourceConnector);

        underTest.accept(new Event<>(REQUEST));

        verify(eventBus).notify(eq(STACK_IMDUPDATE_FAILURE_EVENT.event()), any());
        verify(resourceConnector).update(any(), any(), any(), eq(UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED), any());
        verifyNoInteractions(stackUpdater);
    }

    private void mockConnectors(ResourceConnector resourceConnector) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenReturn(new AuthenticatedContext(null, null));
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(connectors.get(any())).thenReturn(cloudConnector);
        doNothing().when(eventBus).notify(any(), any());
        when(resourceService.findAllByStackId(any())).thenReturn(List.of(new Resource()));
        when(cloudResourceConverter.convert(any())).thenReturn(CloudResource.builder()
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withStatus(CommonStatus.CREATED)
                .withName("")
                .withParameters(Map.of())
                .build());
    }
}
