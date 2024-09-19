package com.sequenceiq.cloudbreak.controller.validation.template;

import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.HostEncryptionProvider;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.ResourceDiskPropertyCalculator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class TemplateValidatorAndUpdaterTest {

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private LocationService locationService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private ResourceDiskPropertyCalculator resourceDiskPropertyCalculator;

    @Mock
    private EmptyVolumeSetFilter emptyVolumeSetFilter;

    @Mock
    private HostEncryptionProvider hostEncryptionProvider;

    @InjectMocks
    private TemplateValidatorAndUpdater templateValidatorAndUpdater;

    @Test
    void testValidateArchitecture() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        template.setInstanceType("m5xlarge");
        template.setVolumeTemplates(Set.of());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder().withArchitecture(Architecture.X86_64).create();
        VmType m5xlarge = vmTypeWithMeta("m5xlarge", vmTypeMeta, true);
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(m5xlarge)));
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.ARM64);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(locationService.location(any(), any())).thenReturn("eu-west-1");
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(template)).thenReturn(template);
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(credential, template, m5xlarge)).thenReturn(template);
        when(hostEncryptionProvider.updateWithHostEncryption(environmentResponse, credential, template, m5xlarge)).thenReturn(template);

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().getFirst(),
                "The 'm5xlarge' instance type's architecture 'x86_64' is not matching requested architecture 'arm64'");
    }

    @Test
    void testValidateCustomInstanceTypeWhenNotSupportShouldComeValidationError() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(vmType("m5xlarge"))));
        PlatformParameters platformParameters = mock(PlatformParameters.class);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                    new CloudCredential(),
                    "MOCK",
                    "",
                    "account",
                    new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getPlatformParameters())
                .thenReturn(Map.of(platform(CloudPlatform.AWS.name()), platformParameters));
        when(platformParameters.specialParameters()).thenReturn(new SpecialParameters(Map.of(PlatformParametersConsts.CUSTOM_INSTANCETYPE, false)));

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().get(0), "Custom instancetype is not supported on AWS platform");
    }

    @Test
    void testValidateCustomInstanceTypeWhenSupportedAndCustomCpuNullShouldDropValidationErrorForCpu() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 4
        )));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(vmType("m5xlarge"))));
        PlatformParameters platformParameters = mock(PlatformParameters.class);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getPlatformParameters())
                .thenReturn(Map.of(platform(CloudPlatform.AWS.name()), platformParameters));
        when(platformParameters.specialParameters()).thenReturn(new SpecialParameters(Map.of(PlatformParametersConsts.CUSTOM_INSTANCETYPE, true)));

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().get(0), "Missing 'cpus' or 'memory' param for custom instancetype on AWS platform");
    }

    @Test
    void testValidateCustomInstanceTypeWhenSupportedAndCustomMemoryNullShouldDropValidationErrorForMemory() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 4
        )));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(vmType("m5xlarge"))));
        PlatformParameters platformParameters = mock(PlatformParameters.class);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getPlatformParameters())
                .thenReturn(Map.of(platform(CloudPlatform.AWS.name()), platformParameters));
        when(platformParameters.specialParameters()).thenReturn(new SpecialParameters(Map.of(PlatformParametersConsts.CUSTOM_INSTANCETYPE, true)));

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().get(0), "Missing 'cpus' or 'memory' param for custom instancetype on AWS platform");
    }

    @Test
    void testValidateCustomInstanceTypeWhenSupportedShouldGetCustomCpuAndMemory() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 5,
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 4
        )));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(vmType("m5xlarge"))));
        PlatformParameters platformParameters = mock(PlatformParameters.class);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getPlatformParameters())
                .thenReturn(Map.of(platform(CloudPlatform.AWS.name()), platformParameters));
        when(platformParameters.specialParameters()).thenReturn(new SpecialParameters(Map.of(PlatformParametersConsts.CUSTOM_INSTANCETYPE, true)));

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateInstanceTypeWhenSupportedAndEphemeralDiskIsTheSameAsMaxShouldNotThrowErrorValidatioShouldBeOk() {
        String instanceType = "m5xlarge";
        String region = "eu-west-1";
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        DiskType ephemeral = DiskType.diskType(AwsDiskType.Ephemeral.value());
        Platform aws = platform(CloudPlatform.AWS.name());
        Credential credential = Credential.builder()
                .build();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("test1");
        Template template = new Template();
        template.setInstanceType(instanceType);
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(AwsDiskType.Ephemeral.value());
        volumeTemplate.setVolumeCount(1);
        volumeTemplate.setVolumeSize(100);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of(region, Set.of(
                vmTypeWithMeta(
                    instanceType,
                    VmTypeMeta.VmTypeMetaBuilder.builder()
                            .withEphemeralConfig(1, 100, 1, 2)
                            .withMaximumPersistentDisksSizeGb(100L)
                            .create(),
                    false
                )
        )));
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getDiskTypes()).thenReturn(
                new PlatformDisks(
                        Map.of(aws, Set.of(ephemeral)),
                        Map.of(aws, ephemeral),
                        Map.of(aws, Map.of("ephemeral", VolumeParameterType.EPHEMERAL)),
                        Map.of(aws, Map.of(ephemeral, displayName("ephemeral")))
                )
        );
        when(locationService.location(any(), any())).thenReturn(region);
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        ValidationResult validationResult = validationBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateInstanceTypeWhenSupportedAndEphemeralDiskIsTheSameAsMaxShouldNotThrowError() {
        String instanceType = "m5xlarge";
        String region = "eu-west-1";
        DiskType ephemeral = DiskType.diskType(AwsDiskType.Ephemeral.value());
        Platform aws = platform(CloudPlatform.AWS.name());
        Credential credential = Credential.builder()
                .build();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("test1");
        Template template = new Template();
        template.setInstanceType(instanceType);
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(AwsDiskType.Ephemeral.value());
        volumeTemplate.setVolumeCount(1);
        volumeTemplate.setVolumeSize(101);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        cloudVmTypes.setCloudVmResponses(Map.of(region, Set.of(
                vmTypeWithMeta(
                        instanceType,
                        VmTypeMeta.VmTypeMetaBuilder.builder()
                                .withEphemeralConfig(1, 100, 1, 2)
                                .withMaximumPersistentDisksSizeGb(100L)
                                .create(),
                        false
                )
        )));
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(
                        new CloudCredential(),
                        "MOCK",
                        "",
                        "account",
                        new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any()))
                .thenReturn(cloudVmTypes);
        when(cloudParameterService.getDiskTypes()).thenReturn(
                new PlatformDisks(
                        Map.of(aws, Set.of(ephemeral)),
                        Map.of(aws, ephemeral),
                        Map.of(aws, Map.of("ephemeral", VolumeParameterType.EPHEMERAL)),
                        Map.of(aws, Map.of(ephemeral, displayName("ephemeral")))
                )
        );
        when(locationService.location(any(), any())).thenReturn(region);
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, validationBuilder);

        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().get(0), "Max allowed volume size for 'm5xlarge': 100 " +
                "GB in case of ephemeral volumes and the current size is 101 GB.");
    }
}