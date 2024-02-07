package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.handler;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateResult;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class StackInstanceMetadataUpdateHandlerTest {

    private static final StackInstanceMetadataUpdateRequest REQUEST = new StackInstanceMetadataUpdateRequest(
            new CloudContext.Builder().withPlatform("AWS").build(), null, null, IMDS_HTTP_TOKEN_REQUIRED);

    @Mock
    private CloudPlatformConnectors connectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private StackDtoService stackService;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private StackInstanceMetadataUpdateHandler underTest;

    @Test
    void testAccept() throws Exception {
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenReturn(
                List.of(new CloudResourceStatus(null, null)));
        mockConnectors(resourceConnector);
        doNothing().when(stackUpdater).updateSupportedImdsVersionIfNecessary(any(), any());

        underTest.accept(new Event<>(REQUEST));

        verify(eventBus).notify(eq(StackInstanceMetadataUpdateResult.class.getSimpleName().toUpperCase()), any());
        verify(resourceConnector).update(any(), any(), any(), eq(UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED), any());
        verify(stackUpdater).updateSupportedImdsVersionIfNecessary(any(), eq(IMDS_HTTP_TOKEN_REQUIRED));
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
        when(cloudResourceConverter.convert(any())).thenReturn(CloudResource.builder()
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withStatus(CommonStatus.CREATED)
                .withName("")
                .withParameters(Map.of())
                .build());
        lenient().doNothing().when(cloudbreakEventService).fireCloudbreakEvent(any(), any(), any());
        lenient().doNothing().when(cloudbreakEventService).fireCloudbreakEvent(any(), any(), any(), any());
        when(cloudbreakMessagesService.getMessage(any())).thenReturn("");
        when(clusterService.updateClusterStatusByStackId(any(), any())).thenReturn(new Cluster());
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getResources()).thenReturn(Set.of(new Resource()));
        when(stackService.getById(any())).thenReturn(stackDto);
    }
}
