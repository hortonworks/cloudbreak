package com.sequenceiq.cloudbreak.core.flow2.stack.provision.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupEphemeralVolumeChecker;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
class StackCreationServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private TemplateService templateService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private StackCreationService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "ephemeralVolumeChecker", new InstanceGroupEphemeralVolumeChecker());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        InstanceStoreMetadata instanceStoreMetadata = getInstanceStoreMetadata();
        when(metadataCollector.collectInstanceStorageCount(any(), any())).thenReturn(instanceStoreMetadata);
    }

    @Test
    void setInstanceStoreCountOnAWS() {
        StackView stack = stack(AwsDiskType.Standard.value(), CloudPlatform.AWS.name(), TemporaryStorage.EPHEMERAL_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.AWS.name(), stack);
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
    void setInstanceStoreCountOnGCP() {
        StackView stack = stack(GcpPlatformParameters.GcpDiskType.HDD.value(), CloudPlatform.GCP.name(), TemporaryStorage.EPHEMERAL_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.GCP.name(), stack);
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(GcpPlatformParameters.GcpDiskType.HDD.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    @Test
    void setInstanceStoreCountOnClusterWithEphemeralDisksOnlyOnAWS() {
        StackView stack = stack(AwsDiskType.Ephemeral.value(), CloudPlatform.AWS.name(), TemporaryStorage.EPHEMERAL_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.AWS.name(), stack);
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
    void setInstanceStoreCountOnClusterWithLocalDisksOnlyOnGCP() {
        StackView stack = stack(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value(), CloudPlatform.GCP.name(), TemporaryStorage.EPHEMERAL_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.GCP.name(), stack);
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES_ONLY, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    @Test
    void setTemporaryStorageToEphemeralVolumesIfInstanceStorageIsAvailable() {
        StackView stack = stack(AwsDiskType.Standard.value(), CloudPlatform.AWS.name(), TemporaryStorage.ATTACHED_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.AWS.name(), stack);
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
    void setTemporaryStorageToLocalDisksIfInstanceStorageIsAvailable() {
        StackView stack = stack(GcpPlatformParameters.GcpDiskType.HDD.value(), CloudPlatform.GCP.name(), TemporaryStorage.ATTACHED_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.GCP.name(), stack);
        underTest.setInstanceStoreCount(context);

        ArgumentCaptor<Template> savedTemplate = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(savedTemplate.capture());

        Set<VolumeTemplate> savedVolumeTemplates = savedTemplate.getValue().getVolumeTemplates();
        assertEquals(1, savedVolumeTemplates.size());

        assertEquals(TemporaryStorage.EPHEMERAL_VOLUMES, savedTemplate.getValue().getTemporaryStorage());
        assertEquals(2, savedTemplate.getValue().getInstanceStorageCount());

        Set<String> expectedTypes = new HashSet<>(Set.of(GcpPlatformParameters.GcpDiskType.HDD.value()));
        savedVolumeTemplates.stream().map(VolumeTemplate::getVolumeType).forEach(expectedTypes::remove);
        assertTrue(expectedTypes.isEmpty());
    }

    @Test
    void setInstanceStoreCountOnAzure() {
        StackView stack = stack(AzureDiskType.STANDARD_SSD_LRS.value(), CloudPlatform.AZURE.name(), TemporaryStorage.EPHEMERAL_VOLUMES);
        StackCreationContext context = getStackContext(CloudPlatform.AZURE.name(), stack);
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

    private StackCreationContext getStackContext(String cloudPlatform, StackView stack) {
        CloudContext cloudContext = CloudContext.Builder.builder().withPlatform(cloudPlatform).build();
        return new StackCreationContext(null, stack, cloudPlatform, cloudContext, null, null);
    }

    private StackView stack(String volumeType, String cloudPlatform, TemporaryStorage tempStorage) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setType(StackType.WORKLOAD);
        InstanceGroup group = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType("vm.type");
        template.setTemporaryStorage(tempStorage);
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(volumeType);
        template.setVolumeTemplates(new HashSet<>(Set.of(volumeTemplate)));
        group.setTemplate(template);
        stack.setCloudPlatform(cloudPlatform);
        when(stackDtoService.getInstanceMetadataByInstanceGroup(STACK_ID)).thenReturn(List.of(new InstanceGroupDto(group, List.of())));
        return stack;
    }
}
