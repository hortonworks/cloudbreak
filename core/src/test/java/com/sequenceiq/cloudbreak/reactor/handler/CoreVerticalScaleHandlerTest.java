package com.sequenceiq.cloudbreak.reactor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.CoreVerticalScaleHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
public class CoreVerticalScaleHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackUpscaleService stackUpscaleService;

    @Mock
    private CoreVerticalScaleService coreVerticalScaleService;

    @InjectMocks
    private CoreVerticalScaleHandler underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackDto stackDto;

    @Mock
    private InstanceGroupDto instanceGroupDto;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudStack cloudStack;

    @Test
    public void testSuccessfulVerticalScale() throws Exception {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        InstanceTemplateV4Request instanceTemplate = new InstanceTemplateV4Request();
        instanceTemplate.setInstanceType("r5ad.4xlarge");
        stackVerticalScaleV4Request.setGroup("compute");
        stackVerticalScaleV4Request.setTemplate(instanceTemplate);
        InstanceStorageInfo instanceStorageInfo = new InstanceStorageInfo(true, 2, 100);
        CoreVerticalScaleRequest request = new CoreVerticalScaleRequest(stackDto,
                instanceGroupDto,
                Set.of("r5ad.4xlarge"),
                List.of(instanceStorageInfo),
                cloudContext,
                cloudCredential,
                cloudStack,
                List.of(cloudResource),
                stackVerticalScaleV4Request,
                List.of("compute"));

        doReturn("RESOURCE_CRN").when(cloudContext).getCrn();

        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        CloudResourceStatus resourceStatus = mock(CloudResourceStatus.class);
        doReturn(List.of(resourceStatus)).when(stackUpscaleService).verticalScale(authenticatedContext, request, cloudConnector);
        doReturn(false).when(stackDto).isStackInStopPhase();

        underTest.accept(new Event<>(request));

        verify(coreVerticalScaleService).updateClouderaManagerConfigsForComputeGroupAndStartServices(eq(stackDto), any(), any(), any());
        verify(coreVerticalScaleService).startInstances(eq(cloudConnector), eq(List.of(cloudResource)), eq(request.getInstanceGroup()),
                eq(stackDto), eq(authenticatedContext));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(CoreVerticalScaleResult.class)), any());
    }

    @Test
    public void testFailureVerticalScale() throws Exception {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        InstanceTemplateV4Request instanceTemplate = new InstanceTemplateV4Request();
        instanceTemplate.setInstanceType("r5ad.4xlarge");
        stackVerticalScaleV4Request.setGroup("compute");
        stackVerticalScaleV4Request.setTemplate(instanceTemplate);
        InstanceStorageInfo instanceStorageInfo = new InstanceStorageInfo(true, 2, 100);
        CoreVerticalScaleRequest request = new CoreVerticalScaleRequest(stackDto,
                instanceGroupDto,
                Set.of("r5ad.4xlarge"),
                List.of(instanceStorageInfo),
                cloudContext,
                cloudCredential,
                cloudStack,
                List.of(cloudResource),
                stackVerticalScaleV4Request,
                List.of("compute"));

        doReturn("RESOURCE_CRN").when(cloudContext).getCrn();

        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        CloudResourceStatus resourceStatus = mock(CloudResourceStatus.class);
        doReturn(List.of(resourceStatus)).when(stackUpscaleService).verticalScale(authenticatedContext, request, cloudConnector);
        doReturn(false).when(stackDto).isStackInStopPhase();

        doThrow(new Exception("TEST")).when(coreVerticalScaleService).updateClouderaManagerConfigsForComputeGroupAndStartServices(eq(stackDto),
                any(), any(), any());

        underTest.accept(new Event<>(request));

        verify(coreVerticalScaleService).updateClouderaManagerConfigsForComputeGroupAndStartServices(eq(stackDto), any(), any(), any());
        verify(eventBus).notify(eq(CloudPlatformResult.failureSelector(CoreVerticalScaleResult.class)), any());
    }

}
