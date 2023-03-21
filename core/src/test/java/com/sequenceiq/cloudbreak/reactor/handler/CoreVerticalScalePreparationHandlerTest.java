package com.sequenceiq.cloudbreak.reactor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationResult;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
public class CoreVerticalScalePreparationHandlerTest {

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private CoreVerticalScaleService coreVerticalScaleService;

    @InjectMocks
    private CoreVerticalScalePreparationHandler underTest;

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
    private MetadataCollector metadataCollector;

    @Mock
    private InstanceStoreMetadata instanceStoreMetadata;

    @Test
    public void testSuccessfulPreparation() throws Exception {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        InstanceTemplateV4Request instanceTemplate = new InstanceTemplateV4Request();
        instanceTemplate.setInstanceType("r5ad.4xlarge");
        stackVerticalScaleV4Request.setGroup("compute");
        stackVerticalScaleV4Request.setTemplate(instanceTemplate);
        CoreVerticalScalePreparationRequest request = new CoreVerticalScalePreparationRequest(cloudContext, cloudCredential, null, stackDto,
                instanceGroupDto, List.of(cloudResource), stackVerticalScaleV4Request);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        doReturn(metadataCollector).when(cloudConnector).metadata();
        doReturn(instanceStoreMetadata).when(metadataCollector).collectInstanceStorageCount(authenticatedContext, List.of("r5ad.4xlarge"));
        doReturn(2).when(instanceStoreMetadata).mapInstanceTypeToInstanceStoreCountNullHandled("r5ad.4xlarge");
        doReturn(100).when(instanceStoreMetadata).mapInstanceTypeToInstanceSizeNullHandled("r5ad.4xlarge");
        doReturn(false).when(stackDto).isStackInStopPhase();

        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        InstanceGroupDto instanceGroup = new InstanceGroupDto(instanceGroupView, List.of(instanceMetadataView));
        doReturn(List.of(instanceGroup)).when(stackDto).getInstanceGroupDtos();
        doReturn("compute").when(instanceGroupView).getGroupName();

        Template template = new Template();
        doReturn(template).when(instanceGroupView).getTemplate();

        Blueprint bp = mock(Blueprint.class);
        doReturn(bp).when(stackDto).getBlueprint();
        String blueprintText = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp");
        doReturn(blueprintText).when(bp).getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq(blueprintText));

        underTest.accept(new Event<>(request));

        verify(coreVerticalScaleService).stopClouderaManagerServicesAndUpdateClusterConfigs(eq(stackDto), any(), any());
        verify(coreVerticalScaleService).stopInstances(eq(cloudConnector), eq(List.of(cloudResource)), eq(request.getInstanceGroup()),
                eq(stackDto), eq(authenticatedContext));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(CoreVerticalScalePreparationResult.class)), any());
    }

    @Test
    public void testFailurePreparation() throws Exception {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        InstanceTemplateV4Request instanceTemplate = new InstanceTemplateV4Request();
        instanceTemplate.setInstanceType("r5ad.4xlarge");
        stackVerticalScaleV4Request.setGroup("compute");
        stackVerticalScaleV4Request.setTemplate(instanceTemplate);
        CoreVerticalScalePreparationRequest request = new CoreVerticalScalePreparationRequest(cloudContext, cloudCredential, null, stackDto,
                instanceGroupDto, List.of(cloudResource), stackVerticalScaleV4Request);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        doReturn(metadataCollector).when(cloudConnector).metadata();
        doReturn(instanceStoreMetadata).when(metadataCollector).collectInstanceStorageCount(authenticatedContext, List.of("r5ad.4xlarge"));
        doReturn(2).when(instanceStoreMetadata).mapInstanceTypeToInstanceStoreCountNullHandled("r5ad.4xlarge");
        doReturn(100).when(instanceStoreMetadata).mapInstanceTypeToInstanceSizeNullHandled("r5ad.4xlarge");
        doReturn(false).when(stackDto).isStackInStopPhase();

        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        InstanceGroupDto instanceGroup = new InstanceGroupDto(instanceGroupView, List.of(instanceMetadataView));
        doReturn(List.of(instanceGroup)).when(stackDto).getInstanceGroupDtos();
        doReturn("compute").when(instanceGroupView).getGroupName();

        Template template = new Template();
        doReturn(template).when(instanceGroupView).getTemplate();

        Blueprint bp = mock(Blueprint.class);
        doReturn(bp).when(stackDto).getBlueprint();
        String blueprintText = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp");
        doReturn(blueprintText).when(bp).getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq(blueprintText));

        doThrow(new Exception("TEST")).when(coreVerticalScaleService).stopClouderaManagerServicesAndUpdateClusterConfigs(eq(stackDto), any(), any());
        underTest.accept(new Event<>(request));

        verify(coreVerticalScaleService).stopClouderaManagerServicesAndUpdateClusterConfigs(eq(stackDto), any(), any());

        verify(eventBus).notify(eq(CloudPlatformResult.failureSelector(CoreVerticalScalePreparationResult.class)), any());
    }
}
