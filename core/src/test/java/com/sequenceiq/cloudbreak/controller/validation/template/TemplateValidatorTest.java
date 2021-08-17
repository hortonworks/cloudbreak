package com.sequenceiq.cloudbreak.controller.validation.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
public class TemplateValidatorTest {

    private Credential credential;

    private User user;

    private Optional<User> optionalUser;

    private CloudVmTypes cloudVmTypes;

    private PlatformDisks platformDisks;

    @Mock
    private ValidationResult.ValidationResultBuilder builder;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private InstanceGroup instanceGroup;

    private Stack stack;

    @InjectMocks
    private TemplateValidator underTest = new TemplateValidator();

    @BeforeEach
    public void setUp() {
        credential = TestUtil.awsCredential();
        user = new User();
        optionalUser = Optional.of(user);
        MockitoAnnotations.initMocks(this);
        stack = TestUtil.stack(Status.AVAILABLE, credential);
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        String location = "fake location";
        VmTypeMeta.VmTypeMetaBuilder vmMetaBuilder = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withCpuAndMemory(Integer.valueOf(8), Float.valueOf(15))
                .withPrice(0.42)
                .withVolumeEncryptionSupport(true)
                .withSsdConfig(1, 17592,
                        1, 24);
        VmType c3VmType = VmType.vmTypeWithMeta("c3.2xlarge", vmMetaBuilder.create(), false);
        VmType i3VmType = VmType.vmTypeWithMeta("i3.2xlarge", vmMetaBuilder.withEphemeralConfig(1, 17592, 1, 24).create(), false);
        Map<String, Set<VmType>> machines = new HashMap<>();
        machines.put(location, Set.of(c3VmType, i3VmType));
        cloudVmTypes = new CloudVmTypes(machines, new HashMap<>());
        when(cloudParameterService.getVmTypesV2(
                isNull(), anyString(), isNull(), any(CdpResourceType.class), any(HashMap.class))).thenReturn(cloudVmTypes);

        Platform platform = Platform.platform("AWS");
        Map<Platform, Map<String, VolumeParameterType>> diskMappings = new HashMap<>();
        Map<String, VolumeParameterType> diskTypeMap = new HashMap<>();
        diskTypeMap.put("standard", VolumeParameterType.SSD);
        diskTypeMap.put(AwsDiskType.Ephemeral.value(), VolumeParameterType.EPHEMERAL);
        diskMappings.put(platform, diskTypeMap);

        platformDisks = new PlatformDisks(new HashMap<>(), new HashMap<>(), diskMappings, new HashMap<>());
        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisks);

        when(locationService.location(anyString(), isNull())).thenReturn(location);
    }

    @Test
    public void validateIDBrokerDataVolumeZeroCountZeroSize() {
        instanceGroup = createInstanceGroup(0, 0, true, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
        verifyIDBrokerVolume(instanceGroup);
    }

    @Test
    public void validateIDBrokerDataVolumeCountOne() {
        instanceGroup = createInstanceGroup(1, 0, true, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateIDBrokerDataVolumeInvalidCount() {
        // volume count is larger than the max value of 24
        instanceGroup = createInstanceGroup(25, 1, true, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(1)).error(anyString());
    }

    @Test
    public void validateIDBrokerDataVolumeDefaultSize() {
        instanceGroup = createInstanceGroup(1, 100, true, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateIDBrokerDataVolumeInvalidSize() {
        instanceGroup = createInstanceGroup(1, 18000, true, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(1)).error(anyString());
    }

    @Test
    public void validateIDBrokerMixedVolumeCountOne() {
        instanceGroup = createInstanceGroup(1, 0, true, true, "i3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateIDBrokerMixedVolumeInvalidCount() {
        // volume count is larger than the max value of 24
        instanceGroup = createInstanceGroup(25, 1, true, true, "i3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(2)).error(anyString());
    }

    @Test
    public void validateIDBrokerMixedVolumeDefaultSize() {
        instanceGroup = createInstanceGroup(1, 100, true, true, "i3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateIDBrokerMixedVolumeInvalidSize() {
        instanceGroup = createInstanceGroup(1, 18000, true, true, "i3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(2)).error(anyString());
    }

    @Test
    public void validateMasterDataVolumeZeroCountZeroSize() {
        instanceGroup = createInstanceGroup(0, 0, false, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(2)).error(anyString());
    }

    @Test
    public void validateMasterDataVolumeCountOne() {
        instanceGroup = createInstanceGroup(1, 1, false, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateMasterDataVolumeInvalidCount() {
        // volume count is larger than the max value of 24
        instanceGroup = createInstanceGroup(25, 1, false, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(1)).error(anyString());
    }

    @Test
    public void validateMasterDataVolumeDefaultSize() {
        instanceGroup = createInstanceGroup(1, 100, false, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(0)).error(anyString());
    }

    @Test
    public void validateMasterDataVolumeInvalidSize() {
        instanceGroup = createInstanceGroup(1, 18000, false, false, "c3.2xlarge");
        underTest.validate(credential, instanceGroup, stack, CdpResourceType.DATALAKE, optionalUser, builder);
        Mockito.verify(builder, Mockito.times(1)).error(anyString());
    }

    private InstanceGroup createInstanceGroup(int dataVolumeCount, int dataVolumeSize, boolean createIDBroker, boolean addEphemeralVolume, String instanceType) {
        Template awsTemplate = TestUtil.awsTemplate(1L, instanceType);
        int volumeCount = dataVolumeCount;
        int volumeSize = dataVolumeSize;
        VolumeTemplate standardVolumeTemplate = TestUtil.volumeTemplate(volumeCount, volumeSize, "standard");
        Set<VolumeTemplate> volumeTemplateSet = Sets.newHashSet(standardVolumeTemplate);
        if (addEphemeralVolume) {
            VolumeTemplate ephemeralVolumeTemplate = TestUtil.volumeTemplate(volumeCount, volumeSize, AwsDiskType.Ephemeral.value());
            volumeTemplateSet.add(ephemeralVolumeTemplate);
        }
        awsTemplate.setVolumeTemplates(volumeTemplateSet);

        if (createIDBroker) {
            instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, awsTemplate);
            instanceGroup.setGroupName(TemplateValidator.GROUP_NAME_ID_BROKER);
        } else {
            instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, awsTemplate);
            instanceGroup.setGroupName("master");
        }

        return instanceGroup;
    }

    private void verifyIDBrokerVolume(InstanceGroup instanceGroup) {
        Template returnedTemplate = instanceGroup.getTemplate();
        Set<VolumeTemplate> returnedVolumeTemplates = returnedTemplate.getVolumeTemplates();
        assertThat(returnedVolumeTemplates.size()).isEqualTo(1);

        VolumeTemplate firstVolumeTemplate = returnedVolumeTemplates.iterator().next();
        assertThat(firstVolumeTemplate.getVolumeCount().intValue()).isEqualTo(0);
        assertThat(firstVolumeTemplate.getVolumeSize().intValue()).isEqualTo(0);
    }
}
