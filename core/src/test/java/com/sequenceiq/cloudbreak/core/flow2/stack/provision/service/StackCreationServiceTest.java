package com.sequenceiq.cloudbreak.core.flow2.stack.provision.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
class StackCreationServiceTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private StackCreationService underTest;

    @BeforeEach
    void setUp() {
        CloudConnector<Object> cloudConnector = Mockito.mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        MetadataCollector metadataCollector = Mockito.mock(MetadataCollector.class);
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        Authenticator authenticator = Mockito.mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        InstanceStoreMetadata instanceStoreMetadata = getInstanceStoreMetadata();
        when(metadataCollector.collectInstanceStorageCount(any(), any())).thenReturn(instanceStoreMetadata);
    }

    @Test
    void setInstanceStoreCountOnAWS() {
        StackContext context = getStackContext(AwsDiskType.Standard.value(), CloudPlatform.AWS.name());
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(AwsDiskType.Standard.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    @Test
    void setInstanceStoreCountOnClusterWithEphemeralDisksOnlyOnAWS() {
        StackContext context = getStackContext(AwsDiskType.Ephemeral.value(), CloudPlatform.AWS.name());
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES_ONLY, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(AwsDiskType.Ephemeral.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    @Test
    void setInstanceStoreCountOnAzure() {
        StackContext context = getStackContext(AzureDiskType.STANDARD_SSD_LRS.value(), CloudPlatform.AZURE.name());
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(AzureDiskType.STANDARD_SSD_LRS.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    private InstanceStoreMetadata getInstanceStoreMetadata() {
        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata();
        VolumeParameterConfig instanceStorageConfig = new VolumeParameterConfig(
                VolumeParameterType.EPHEMERAL, 100, 100, 2, 2);
        instanceStoreMetadata.getInstaceStoreConfigMap().put("vm.type", instanceStorageConfig);
        return instanceStoreMetadata;
    }

    private StackContext getStackContext(String volumeType, String cloudPlatform) {
        Stack stack = new Stack();
        InstanceGroup group = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType("vm.type");
        template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(volumeType);
        template.setVolumeTemplates(new HashSet<>(Set.of(volumeTemplate)));
        group.setTemplate(template);
        stack.setInstanceGroups(Set.of(group));
        stack.setCloudPlatform(cloudPlatform);

        CloudContext cloudContext = CloudContext.Builder.builder().withPlatform(cloudPlatform).build();
        return new StackContext(null, stack, cloudContext, null, null);
    }
}