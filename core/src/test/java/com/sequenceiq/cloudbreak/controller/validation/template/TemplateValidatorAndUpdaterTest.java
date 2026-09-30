package com.sequenceiq.cloudbreak.controller.validation.template;

import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_FILTERED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_ROOT_VOLUME_TYPE_CHANGED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_VOLUME_SIZE_INCREASED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
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
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class TemplateValidatorAndUpdaterTest {

    private static final Credential CREDENTIAL = Credential.builder().build();

    private static final String EU_WEST_1 = "eu-west-1";

    private static final String EU_WEST_1A = "eu-west-1a";

    private static final String EU_WEST_1B = "eu-west-1b";

    private static final String EU_WEST_1C = "eu-west-1c";

    private static final String GCP_LOCATION = "europe-west1";

    private static final String PD_BALANCED = "pd-balanced";

    private static final String PD_SSD = "pd-ssd";

    private static final String HYPERDISK_BALANCED = "hyperdisk-balanced";

    private static final String LOCAL_SSD = "local-ssd";

    private static final String EPHEMERAL_VOLUME_TYPE = "ephemeral";

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

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

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
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().getFirst(),
                "The 'm5xlarge' instance type's architecture 'x86_64' is not matching requested architecture 'arm64'");
    }

    @Test
    void testValidateHyperVGenerationWhenGen2ImageAndGen1OnlyInstanceType() {
        Credential credential = Credential.builder().build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = azureInstanceGroup("Standard_D2_v2");
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperVGenerations("V1")
                .create();
        Stack stack = azureStack(vmTypeMeta, "Standard_D2_v2", instanceGroup);

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATALAKE, "V2", validationBuilder);

        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("The 'Standard_D2_v2' instance type does not support the selected image's Hyper-V generation 'V2'. Supported generation(s): 'V1'",
                validationResult.getErrors().getFirst());
    }

    @Test
    void testValidateHyperVGenerationWhenGen2ImageAndDualGenerationInstanceType() {
        Credential credential = Credential.builder().build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = azureInstanceGroup("Standard_D2s_v5");
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperVGenerations("V1,V2")
                .create();
        Stack stack = azureStack(vmTypeMeta, "Standard_D2s_v5", instanceGroup);

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATALAKE, "V2", validationBuilder);

        assertFalse(validationBuilder.build().hasError());
    }

    @Test
    void testValidateHyperVGenerationWhenImageIsNotTaggedThenNoError() {
        Credential credential = Credential.builder().build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = azureInstanceGroup("Standard_D2_v2");
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperVGenerations("V1")
                .create();
        Stack stack = azureStack(vmTypeMeta, "Standard_D2_v2", instanceGroup);

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATALAKE, null, validationBuilder);

        assertFalse(validationBuilder.build().hasError());
    }

    @Test
    void testValidateArchitectureAndFallbackInstanceType() {
        Credential credential = Credential.builder()
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        template.setInstanceType("m5xlarge");
        template.setFallbackInstanceTypes(new Json(List.of("m5large")));
        template.setVolumeTemplates(Set.of());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder().withArchitecture(Architecture.X86_64).create();
        VmType m5large = vmTypeWithMeta("m5large", vmTypeMeta, true);
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(m5large)));
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
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(entitlementService.isFallbackInstanceTypeEnabled(any())).thenReturn(true);

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().getFirst(),
                "The 'm5large' instance type's architecture 'x86_64' is not matching requested architecture 'arm64'");
    }

    @Test
    void testValidateNoInstanceTypeFallbackInstanceType() {
        Credential credential = Credential.builder()
                .cloudPlatform(CloudPlatform.AWS.name())
                .build();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AWS.name());
        template.setInstanceType("m5xlarge");
        template.setFallbackInstanceTypes(new Json(List.of("m5large")));
        template.setVolumeTemplates(Set.of());
        instanceGroup.setTemplate(template);
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder().withArchitecture(Architecture.X86_64).create();
        VmType m5xlarge = vmTypeWithMeta("r5xlarge", vmTypeMeta, true);
        cloudVmTypes.setCloudVmResponses(Map.of("eu-west-1", Set.of(m5xlarge)));
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        Stack stack = new Stack();
        stack.setRegion("eu-west-1");
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
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(credential, template, null)).thenReturn(template);
        when(hostEncryptionProvider.updateWithHostEncryption(environmentResponse, credential, template, null)).thenReturn(template);
        when(entitlementService.isFallbackInstanceTypeEnabled(any())).thenReturn(true);

        // Create a ValidationResult.ValidationResultBuilder
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        // Call the validate method
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

        // Assert that there are no errors in the ValidationResult
        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().getFirst(),
                "Our platform currently not supporting the '[m5xlarge, m5large]' instance types for 'AWS' in eu-west-1.");
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
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

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
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

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
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

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
        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

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
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(any())).thenReturn(instanceGroup.getTemplate());
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

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
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(any())).thenReturn(instanceGroup.getTemplate());
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        templateValidatorAndUpdater.validate(environmentResponse, credential, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);

        ValidationResult validationResult = validationBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().get(0), "Max allowed volume size for 'm5xlarge': 100 " +
                "GB in case of ephemeral volumes and the current size is 101 GB.");
    }

    private InstanceGroup azureInstanceGroup(String instanceType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(CloudPlatform.AZURE.name());
        template.setInstanceType(instanceType);
        template.setVolumeTemplates(Set.of());
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    @Test
    void testValidateWhenEveryInstanceTypeIsAvailableThenTemplateIsUntouchedAndNoNotificationIsSent() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("m5xlarge", "m5large"), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenDefaultInstanceTypeIsNotAvailableThenFallbackIsPromotedAndUserIsNotified() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large", "r5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("m5large", "r5large"), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5large", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("r5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_FILTERED);
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED, "master", "m5xlarge", EU_WEST_1, "m5large");
    }

    @Test
    void testValidateWhenOnlyFallbackInstanceTypeIsNotAvailableThenItIsDroppedAndDefaultIsKept() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large", "r5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("m5xlarge", "r5large"), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("r5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FILTERED, "master", EU_WEST_1, "m5large");
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED);
    }

    @Test
    void testValidateWhenInstanceTypeIsRepeatedInTheFallbackListThenItIsDeduplicated() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large", "m5xlarge", "m5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("m5xlarge", "m5large"), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenNoInstanceTypeIsAvailableThenValidationFailsAndTemplateIsUntouched() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("r5xlarge"), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertTrue(validationResult.hasError());
        assertEquals("Our platform currently not supporting the '[m5xlarge, m5large]' instance types for 'AWS' in eu-west-1.",
                validationResult.getErrors().getFirst());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenPromotedInstanceTypeArchitectureDoesNotMatchThenTemplateIsNotUpdated() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        stack.setArchitecture(Architecture.ARM64);
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertTrue(validationResult.hasError());
        assertEquals("n2-standard-8", template.getInstanceType());
        assertEquals(List.of("n4-standard-8"), template.getFallbackInstanceTypesAsList());
        assertEquals(PD_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        assertNull(template.getRootVolumeType());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenFallbackEntitlementIsDisabledThenTemplateIsUntouchedEvenIfFallbackIsUnavailable() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, availableVmTypes("m5xlarge"), false);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenFallbackEntitlementIsDisabledThenRootVolumeTypeIsStillFilled() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdOnlyVmType("n2-standard-8"))), false);

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals(PD_SSD, template.getRootVolumeType());
        assertEquals(List.of("n4-standard-8"), template.getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenProviderReturnsNoInstanceTypeForTheLocationThenTemplateIsUntouched() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        Stack stack = stack();
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of()), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenGcpDefaultChangesToHyperdiskOnlyInstanceTypeThenVolumeTypeIsChanged() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("n4-standard-8", template.getInstanceType());
        assertEquals(HYPERDISK_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        assertEquals(HYPERDISK_BALANCED, template.getRootVolumeType());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED, "master", PD_BALANCED, "n4-standard-8", HYPERDISK_BALANCED);
    }

    @Test
    void testValidateWhenGcpDefaultChangesButVolumeTypeIsSupportedThenVolumeTypeIsKept() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("c3-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdAndHyperdiskVmType("c3-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("c3-standard-8", template.getInstanceType());
        assertEquals(PD_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED);
    }

    @ParameterizedTest
    @CsvSource({LOCAL_SSD + ", 375", EPHEMERAL_VOLUME_TYPE + ", 100"})
    void testValidateWhenGcpDefaultChangesThenNonExchangeableVolumeTypeIsKept(String volumeType, int volumeSize) {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), volumeType, volumeSize);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertTrue(validationResult.hasError());
        assertEquals("n4-standard-8", template.getInstanceType());
        assertEquals(volumeType, template.getVolumeTemplates().iterator().next().getVolumeType());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED);
    }

    @Test
    void testValidateWhenDefaultChangesOnAwsThenVolumeTypeIsKept() {
        InstanceGroup instanceGroup = awsInstanceGroupWithVolume("m5xlarge", List.of("m5large"), AwsDiskType.Gp2.value());
        Stack stack = stack();
        setUpAwsDiskTypes();
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of(vmTypeWithoutDefaultVolumeType("m5large"))), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("m5large", template.getInstanceType());
        assertEquals(AwsDiskType.Gp2.value(), template.getVolumeTemplates().iterator().next().getVolumeType());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED);
    }

    @Test
    void testValidateWhenGcpVolumeTypeChangesThenVolumeSizeIsIncreasedToTheMinimum() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmTypeWithMinimumVolumeSize("n4-standard-8", 2048))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        VolumeTemplate volumeTemplate = instanceGroup.getTemplate().getVolumeTemplates().iterator().next();
        assertEquals(HYPERDISK_BALANCED, volumeTemplate.getVolumeType());
        assertEquals(2048, volumeTemplate.getVolumeSize());
        assertFalse(validationResult.hasError());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_VOLUME_SIZE_INCREASED, "master", HYPERDISK_BALANCED, "100", "2048");
    }

    @Test
    void testValidateWhenGcpRootVolumeTypeIsEmptyAndDefaultInstanceTypeIsKeptThenRootVolumeTypeIsFilled() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdOnlyVmType("n2-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("n2-standard-8", template.getInstanceType());
        assertEquals(PD_SSD, template.getRootVolumeType());
        assertEquals(PD_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenGcpRootVolumeTypeIsAlreadySetAndSupportedThenItIsKept() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n2-standard-16"), PD_BALANCED, 100);
        instanceGroup.getTemplate().setRootVolumeType(PD_SSD);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdOnlyVmType("n2-standard-16"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertEquals("n2-standard-16", instanceGroup.getTemplate().getInstanceType());
        assertEquals(PD_SSD, instanceGroup.getTemplate().getRootVolumeType());
        assertFalse(validationResult.hasError());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_ROOT_VOLUME_TYPE_CHANGED);
    }

    @Test
    void testValidateWhenGcpDefaultChangesToHyperdiskOnlyInstanceTypeThenPresetRootVolumeTypeIsChanged() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8"), PD_BALANCED, 100);
        instanceGroup.getTemplate().setRootVolumeType(PD_SSD);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("n4-standard-8", template.getInstanceType());
        assertEquals(HYPERDISK_BALANCED, template.getRootVolumeType());
        assertEquals(HYPERDISK_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        assertFalse(validationResult.hasError());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_ROOT_VOLUME_TYPE_CHANGED, "master", PD_SSD, "n4-standard-8", HYPERDISK_BALANCED);
    }

    @Test
    void testValidateWhenGcpDefaultSupportsPdOnlyThenHyperdiskOnlyFallbacksAreDropped() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n2-standard-16", "n4-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(
                pdOnlyVmType("n2-standard-8"), pdOnlyVmType("n2-standard-16"), hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("n2-standard-8", template.getInstanceType());
        assertEquals(List.of("n2-standard-16"), template.getFallbackInstanceTypesAsList());
        assertFalse(validationResult.hasError());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "n2-standard-8", PD_SSD + " (root), " + PD_BALANCED,
                "n4-standard-8");
    }

    @Test
    void testValidateWhenGcpDefaultBecomesHyperdiskOnlyThenPdOnlyFallbacksAreDropped() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("n4-standard-8", "n2-standard-16"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"), pdOnlyVmType("n2-standard-16"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("n4-standard-8", template.getInstanceType());
        assertEquals(HYPERDISK_BALANCED, template.getVolumeTemplates().iterator().next().getVolumeType());
        assertEquals(List.of(), template.getFallbackInstanceTypesAsList());
        assertFalse(validationResult.hasError());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "n4-standard-8",
                HYPERDISK_BALANCED + " (root), " + HYPERDISK_BALANCED, "n2-standard-16");
    }

    @Test
    void testValidateWhenGcpFallbackSupportsBothVolumeTypesThenItIsKept() {
        InstanceGroup instanceGroup = gcpInstanceGroup("n2-standard-8", List.of("c3-standard-8"), PD_BALANCED, 100);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdOnlyVmType("n2-standard-8"), pdAndHyperdiskVmType("c3-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("n2-standard-8", template.getInstanceType());
        assertEquals(List.of("c3-standard-8"), template.getFallbackInstanceTypesAsList());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE);
    }

    @Test
    void testValidateWhenAwsFallbacksSupportTheVolumeTypeThenTheyAreKept() {
        InstanceGroup instanceGroup = awsInstanceGroupWithVolume("m5xlarge", List.of("m5large"), AwsDiskType.Gp2.value());
        Stack stack = stack();
        VmTypeMeta ssdMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withSsdConfig(10, 1000, 1, 24)
                .create();
        setUpAwsDiskTypes();
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of(
                vmTypeWithMeta("m5xlarge", ssdMeta, true), vmTypeWithMeta("m5large", ssdMeta, true))), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertEquals("m5xlarge", template.getInstanceType());
        assertEquals(List.of("m5large"), template.getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenGcpInstanceTypesHaveNoCommonRootVolumeTypeThenFallbacksNotSupportingTheRootVolumeTypeAreDropped() {
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.GCP, "n2-standard-8", List.of("n4-standard-8", "n2-standard-16"), Set.of());
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(
                pdOnlyVmType("n2-standard-8"), hyperdiskOnlyVmType("n4-standard-8"), pdOnlyVmType("n2-standard-16"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("n2-standard-8", template.getInstanceType());
        assertEquals(PD_SSD, template.getRootVolumeType());
        assertEquals(List.of("n2-standard-16"), template.getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "n2-standard-8", PD_SSD + " (root)",
                "n4-standard-8");
    }

    @Test
    void testValidateWhenUnavailableInstanceTypeCausedTheMissingCommonRootVolumeTypeThenRootVolumeTypeIsDeterminedForTheAvailableOnes() {
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.GCP, "n4-standard-8", List.of("n2-standard-8", "c3-standard-8"), Set.of());
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(hyperdiskOnlyVmType("n4-standard-8"), pdAndHyperdiskVmType("c3-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("n4-standard-8", template.getInstanceType());
        assertEquals(HYPERDISK_BALANCED, template.getRootVolumeType());
        assertEquals(List.of("c3-standard-8"), template.getFallbackInstanceTypesAsList());
        verifyEventNotFired(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE);
    }

    @Test
    void testValidateWhenGcpRootVolumeTypeIsPresetThenFallbacksNotSupportingItAreDropped() {
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.GCP, "n2-standard-8", List.of("n4-standard-8"), Set.of());
        instanceGroup.getTemplate().setRootVolumeType(PD_SSD);
        Stack stack = gcpStack();
        setUpGcpMocks(instanceGroup, Map.of(GCP_LOCATION, Set.of(pdOnlyVmType("n2-standard-8"), hyperdiskOnlyVmType("n4-standard-8"))));

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals(PD_SSD, template.getRootVolumeType());
        assertEquals(List.of(), template.getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "n2-standard-8", PD_SSD + " (root)",
                "n4-standard-8");
    }

    @ParameterizedTest(name = "volume size: {0}, volume count: {1}")
    @CsvSource({"100, 8, 1000, 4", "2000, 1, 1000, 24"})
    void testValidateWhenFallbackDoesNotSupportTheVolumeSizeOrCountThenItIsDropped(int volumeSize, int volumeCount, int fallbackMaximumSize,
            int fallbackMaximumNumber) {
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.AWS, "m5xlarge", List.of("m5large", "r5large"),
                Set.of(volumeTemplate(AwsDiskType.Gp2.value(), volumeSize, volumeCount)));
        Stack stack = stack();
        setUpAwsDiskTypes();
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of(awsVmTypeWithSsdLimits("m5xlarge", 4000, 24),
                awsVmTypeWithSsdLimits("m5large", fallbackMaximumSize, fallbackMaximumNumber), awsVmTypeWithSsdLimits("r5large", 2000, 8))), true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", template.getInstanceType());
        assertEquals(List.of("r5large"), template.getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "m5xlarge", AwsDiskType.Gp2.value(), "m5large");
    }

    @Test
    void testValidateWhenFirstAvailableFallbackDoesNotSupportTheEphemeralVolumesThenTheFirstCompatibleOneIsPromoted() {
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.AWS, "m5dxlarge", List.of("m5large", "m5dlarge"),
                Set.of(volumeTemplate(AwsDiskType.Ephemeral.value(), 75, 1)));
        Stack stack = stack();
        setUpAwsDiskTypes();
        VmTypeMeta ephemeralMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withSsdConfig(10, 1000, 1, 24)
                .withEphemeralConfig(75, 75, 1, 1)
                .create();
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of(awsVmTypeWithSsdLimits("m5large", 1000, 24), vmTypeWithMeta("m5dlarge", ephemeralMeta, true))),
                true);

        ValidationResult validationResult = validate(instanceGroup, stack);

        Template template = instanceGroup.getTemplate();
        assertFalse(validationResult.hasError());
        assertEquals("m5dlarge", template.getInstanceType());
        assertEquals(List.of(), template.getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED, "master", "m5dxlarge", EU_WEST_1, "m5dlarge");
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, "master", "m5dlarge", AwsDiskType.Ephemeral.value(),
                "m5large");
    }

    @Test
    void testValidateWhenMultiAzGroupThenFallbackAvailableInAnotherZoneOfTheGroupIsKept() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        setAvailabilityZones(instanceGroup, EU_WEST_1A, EU_WEST_1B);
        Stack stack = stack();
        stack.setMultiAz(true);
        setUpMocks(instanceGroup, Map.of(
                        EU_WEST_1A, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B)),
                        EU_WEST_1B, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B), vmTypeInZones("m5large", EU_WEST_1B))),
                true, EU_WEST_1A);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void testValidateWhenMultiAzGroupThenFallbackNotAvailableInAnyZoneOfTheGroupIsDropped() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        setAvailabilityZones(instanceGroup, EU_WEST_1B, EU_WEST_1C);
        Stack stack = stack();
        stack.setMultiAz(true);
        setUpMocks(instanceGroup, Map.of(EU_WEST_1, Set.of(
                        vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B, EU_WEST_1C), vmTypeInZones("m5large", EU_WEST_1A))),
                true, EU_WEST_1);

        ValidationResult validationResult = validate(instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of(), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FILTERED, "master", EU_WEST_1B + ", " + EU_WEST_1C, "m5large");
    }

    @Test
    void testValidateWhenMultiAzStackAndGroupHasNoZonesThenTheZonesOfTheEnvironmentAreUsed() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        Stack stack = stack();
        stack.setMultiAz(true);
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        EnvironmentNetworkResponse network = mock(EnvironmentNetworkResponse.class);
        when(environment.getNetwork()).thenReturn(network);
        when(network.getAvailabilityZones(CloudPlatform.AWS)).thenReturn(Set.of(EU_WEST_1A, EU_WEST_1B));
        setUpMocks(instanceGroup, Map.of(
                        EU_WEST_1A, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B)),
                        EU_WEST_1B, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B), vmTypeInZones("m5large", EU_WEST_1B))),
                true, EU_WEST_1A);

        ValidationResult validationResult = validate(environment, instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    @ParameterizedTest(name = "group has a null zone: {0}")
    @ValueSource(booleans = {false, true})
    void testValidateWhenNotMultiAzAndGroupHasNoValidZonesThenOnlyTheLocationOfTheStackIsChecked(boolean groupHasNullZone) {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        if (groupHasNullZone) {
            setAvailabilityZones(instanceGroup, (String) null);
        }
        Stack stack = stack();
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        setUpMocks(instanceGroup, Map.of(
                        EU_WEST_1A, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B)),
                        EU_WEST_1B, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B), vmTypeInZones("m5large", EU_WEST_1B))),
                true, EU_WEST_1A);

        ValidationResult validationResult = validate(environment, instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of(), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyInstanceGroupEvent(STACK_INSTANCE_TYPE_VALIDATION_FILTERED, "master", EU_WEST_1A, "m5large");
        verifyNoInteractions(environment);
    }

    @Test
    void testValidateWhenMultiAzStackAndEnvironmentZonesContainNullThenTheNullZoneIsIgnored() {
        InstanceGroup instanceGroup = instanceGroup("m5xlarge", List.of("m5large"));
        setAvailabilityZones(instanceGroup, (String) null);
        Stack stack = stack();
        stack.setMultiAz(true);
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        EnvironmentNetworkResponse network = mock(EnvironmentNetworkResponse.class);
        when(environment.getNetwork()).thenReturn(network);
        Set<String> environmentZones = new HashSet<>();
        environmentZones.add(null);
        environmentZones.add(EU_WEST_1B);
        when(network.getAvailabilityZones(CloudPlatform.AWS)).thenReturn(environmentZones);
        setUpMocks(instanceGroup, Map.of(
                        EU_WEST_1A, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B)),
                        EU_WEST_1B, Set.of(vmTypeInZones("m5xlarge", EU_WEST_1A, EU_WEST_1B), vmTypeInZones("m5large", EU_WEST_1B))),
                true, EU_WEST_1A);

        ValidationResult validationResult = validate(environment, instanceGroup, stack);

        assertFalse(validationResult.hasError());
        assertEquals("m5xlarge", instanceGroup.getTemplate().getInstanceType());
        assertEquals(List.of("m5large"), instanceGroup.getTemplate().getFallbackInstanceTypesAsList());
        verifyNoInteractions(cloudbreakEventService);
    }

    private void setAvailabilityZones(InstanceGroup instanceGroup, String... zones) {
        Set<AvailabilityZone> availabilityZones = new HashSet<>();
        for (String zone : zones) {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone(zone);
            availabilityZone.setInstanceGroup(instanceGroup);
            availabilityZones.add(availabilityZone);
        }
        instanceGroup.setAvailabilityZones(availabilityZones);
    }

    private VmType vmTypeInZones(String instanceType, String... zones) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withAvailabilityZones(List.of(zones))
                .create(), true);
    }

    private VmType awsVmTypeWithSsdLimits(String instanceType, int maximumSize, int maximumNumber) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withSsdConfig(1, maximumSize, 1, maximumNumber)
                .create(), true);
    }

    private InstanceGroup instanceGroup(String instanceType, List<String> fallbackInstanceTypes) {
        return instanceGroup(CloudPlatform.AWS, instanceType, fallbackInstanceTypes, Set.of());
    }

    private InstanceGroup awsInstanceGroupWithVolume(String instanceType, List<String> fallbackInstanceTypes, String volumeType) {
        return instanceGroup(CloudPlatform.AWS, instanceType, fallbackInstanceTypes, Set.of(volumeTemplate(volumeType, 100, 1)));
    }

    private InstanceGroup gcpInstanceGroup(String instanceType, List<String> fallbackInstanceTypes, String volumeType, int volumeSize) {
        return instanceGroup(CloudPlatform.GCP, instanceType, fallbackInstanceTypes, Set.of(volumeTemplate(volumeType, volumeSize, 2)));
    }

    private InstanceGroup instanceGroup(CloudPlatform cloudPlatform, String instanceType, List<String> fallbackInstanceTypes,
            Set<VolumeTemplate> volumeTemplates) {
        Template template = new Template();
        template.setAttributes(new Json(Map.of()));
        template.setCloudPlatform(cloudPlatform.name());
        template.setInstanceType(instanceType);
        template.setFallbackInstanceTypes(new Json(fallbackInstanceTypes));
        template.setVolumeTemplates(volumeTemplates);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    private Stack azureStack(VmTypeMeta vmTypeMeta, String instanceType, InstanceGroup instanceGroup) {
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(Map.of("westus2", Set.of(vmTypeWithMeta(instanceType, vmTypeMeta, true))));
        Stack stack = new Stack();
        stack.setArchitecture(Architecture.X86_64);

        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(new CloudCredential(), "AZURE", "", "account", new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(cloudVmTypes);
        when(locationService.location(any(), any())).thenReturn("westus2");
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(instanceGroup.getTemplate())).thenReturn(instanceGroup.getTemplate());
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(instanceGroup.getTemplate());
        return stack;
    }

    private VolumeTemplate volumeTemplate(String volumeType, int volumeSize, int volumeCount) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(volumeType);
        volumeTemplate.setVolumeSize(volumeSize);
        volumeTemplate.setVolumeCount(volumeCount);
        return volumeTemplate;
    }

    private Stack stack() {
        return stack(EU_WEST_1);
    }

    private Stack gcpStack() {
        return stack(GCP_LOCATION);
    }

    private Stack stack(String region) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setRegion(region);
        stack.setArchitecture(Architecture.X86_64);
        stack.setStackStatus(new StackStatus(stack, Status.CREATE_IN_PROGRESS, "", DetailedStackStatus.PROVISION_REQUESTED));
        return stack;
    }

    private Map<String, Set<VmType>> availableVmTypes(String... instanceTypes) {
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder().withArchitecture(Architecture.X86_64).create();
        return Map.of(EU_WEST_1, Arrays.stream(instanceTypes)
                .map(instanceType -> vmTypeWithMeta(instanceType, vmTypeMeta, true))
                .collect(Collectors.toSet()));
    }

    /**
     * Mimics how GcpPlatformResources builds the metadata of a machine family supporting persistent disks only: no hyperdisk config is added.
     */
    private VmType pdOnlyVmType(String instanceType) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withSsdConfig(10, 65536, 1, 16)
                .withBalancedHddConfig(10, 65536, 1, 16)
                .withDefaultDiskType(PD_BALANCED)
                .create(), true);
    }

    /**
     * Mimics how GcpPlatformResources builds the metadata of a hyperdisk only machine family: the persistent disk configs are removed.
     */
    private VmType hyperdiskOnlyVmType(String instanceType) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperdiskBalancedConfig(10, 65536, 1, 16)
                .withBalancedHddConfig(null)
                .withSsdConfig(null)
                .withMagneticConfig(null)
                .withDefaultDiskType(HYPERDISK_BALANCED)
                .create(), true);
    }

    private VmType hyperdiskOnlyVmTypeWithMinimumVolumeSize(String instanceType, int minimumVolumeSize) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperdiskBalancedConfig(minimumVolumeSize, 65536, 1, 16)
                .withDefaultDiskType(HYPERDISK_BALANCED)
                .create(), true);
    }

    private VmType vmTypeWithoutDefaultVolumeType(String instanceType) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .create(), true);
    }

    /**
     * Mimics a machine family such as c3 that supports hyperdisks and persistent disks as well.
     */
    private VmType pdAndHyperdiskVmType(String instanceType) {
        return vmTypeWithMeta(instanceType, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withSsdConfig(10, 65536, 1, 16)
                .withBalancedHddConfig(10, 65536, 1, 16)
                .withHyperdiskBalancedConfig(10, 65536, 1, 16)
                .withDefaultDiskType(HYPERDISK_BALANCED)
                .create(), true);
    }

    private void setUpMocks(InstanceGroup instanceGroup, Map<String, Set<VmType>> vmTypesByLocation, boolean fallbackEnabled) {
        setUpMocks(instanceGroup, vmTypesByLocation, fallbackEnabled, EU_WEST_1);
    }

    private void setUpGcpMocks(InstanceGroup instanceGroup, Map<String, Set<VmType>> vmTypesByLocation) {
        setUpGcpMocks(instanceGroup, vmTypesByLocation, true);
    }

    private void setUpGcpMocks(InstanceGroup instanceGroup, Map<String, Set<VmType>> vmTypesByLocation, boolean fallbackEnabled) {
        Platform gcp = platform(CloudPlatform.GCP.name());
        DiskType balanced = DiskType.diskType(PD_BALANCED);
        DiskType ssd = DiskType.diskType(PD_SSD);
        DiskType hyperdiskBalanced = DiskType.diskType(HYPERDISK_BALANCED);
        DiskType localSsd = DiskType.diskType(LOCAL_SSD);
        DiskType ephemeral = DiskType.diskType(EPHEMERAL_VOLUME_TYPE);
        when(cloudParameterService.getDiskTypes()).thenReturn(new PlatformDisks(
                Map.of(gcp, Set.of(balanced, ssd, hyperdiskBalanced, localSsd, ephemeral)),
                Map.of(gcp, balanced),
                Map.of(gcp, Map.of(
                        PD_BALANCED, VolumeParameterType.BALANCED_HDD,
                        PD_SSD, VolumeParameterType.SSD,
                        HYPERDISK_BALANCED, VolumeParameterType.HYPERDISK_BALANCED,
                        LOCAL_SSD, VolumeParameterType.LOCAL_SSD,
                        EPHEMERAL_VOLUME_TYPE, VolumeParameterType.EPHEMERAL)),
                Map.of(gcp, Map.of(
                        balanced, displayName(PD_BALANCED),
                        ssd, displayName(PD_SSD),
                        hyperdiskBalanced, displayName(HYPERDISK_BALANCED),
                        localSsd, displayName(LOCAL_SSD),
                        ephemeral, displayName(EPHEMERAL_VOLUME_TYPE)))));
        PlatformParameters gcpPlatformParameters = mock(PlatformParameters.class);
        lenient().when(cloudParameterService.getPlatformParameters()).thenReturn(Map.of(gcp, gcpPlatformParameters));
        lenient().when(gcpPlatformParameters.defaultRootDiskType(anyString()))
                .thenAnswer(invocation -> gcpDefaultRootDiskType(List.of(invocation.<String>getArgument(0))));
        lenient().when(gcpPlatformParameters.defaultRootDiskType(anyString(), anyList())).thenAnswer(invocation -> {
            List<String> instanceTypes = new ArrayList<>();
            instanceTypes.add(invocation.getArgument(0));
            instanceTypes.addAll(invocation.getArgument(1));
            return gcpDefaultRootDiskType(instanceTypes);
        });
        setUpMocks(instanceGroup, vmTypesByLocation, fallbackEnabled, GCP_LOCATION);
    }

    /**
     * Mimics GcpPlatformParameters: n4 supports hyperdisks only, c3 supports both, the others support persistent disks only.
     */
    private DiskType gcpDefaultRootDiskType(List<String> instanceTypes) {
        boolean hyperdiskSupportedByAll = instanceTypes.stream().allMatch(type -> type.startsWith("n4") || type.startsWith("c3"));
        boolean persistentDiskSupportedByAll = instanceTypes.stream().noneMatch(type -> type.startsWith("n4"));
        if (hyperdiskSupportedByAll) {
            return DiskType.diskType(HYPERDISK_BALANCED);
        } else if (persistentDiskSupportedByAll) {
            return DiskType.diskType(PD_SSD);
        }
        throw new CloudbreakServiceException("Cannot determine root disk type for " + instanceTypes);
    }

    private void setUpAwsDiskTypes() {
        Platform aws = platform(CloudPlatform.AWS.name());
        DiskType gp2 = DiskType.diskType(AwsDiskType.Gp2.value());
        DiskType ephemeral = DiskType.diskType(AwsDiskType.Ephemeral.value());
        when(cloudParameterService.getDiskTypes()).thenReturn(new PlatformDisks(
                Map.of(aws, Set.of(gp2, ephemeral)),
                Map.of(aws, gp2),
                Map.of(aws, Map.of(AwsDiskType.Gp2.value(), VolumeParameterType.SSD, AwsDiskType.Ephemeral.value(), VolumeParameterType.EPHEMERAL)),
                Map.of(aws, Map.of(gp2, displayName(AwsDiskType.Gp2.value()), ephemeral, displayName(AwsDiskType.Ephemeral.value())))));
    }

    private void setUpMocks(InstanceGroup instanceGroup, Map<String, Set<VmType>> vmTypesByLocation, boolean fallbackEnabled, String location) {
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        cloudVmTypes.setCloudVmResponses(vmTypesByLocation);
        Template template = instanceGroup.getTemplate();
        when(extendedCloudCredentialConverter.convert(any()))
                .thenReturn(new ExtendedCloudCredential(new CloudCredential(), "MOCK", "", "account", new ArrayList<>()));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(cloudVmTypes);
        when(locationService.location(any(), any())).thenReturn(location);
        when(emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(any())).thenReturn(template);
        when(resourceDiskPropertyCalculator.updateWithResourceDiskAttached(any(), any(), any())).thenReturn(template);
        when(hostEncryptionProvider.updateWithHostEncryption(any(), any(), any(), any())).thenReturn(template);
        when(entitlementService.isFallbackInstanceTypeEnabled(any())).thenReturn(fallbackEnabled);
    }

    private ValidationResult validate(InstanceGroup instanceGroup, Stack stack) {
        return validate(mock(DetailedEnvironmentResponse.class), instanceGroup, stack);
    }

    private ValidationResult validate(DetailedEnvironmentResponse environment, InstanceGroup instanceGroup, Stack stack) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        templateValidatorAndUpdater.validate(environment, CREDENTIAL, instanceGroup, stack, CdpResourceType.DATAHUB, null, validationBuilder);
        return validationBuilder.build();
    }

    private void verifyEventNotFired(ResourceEvent resourceEvent) {
        verify(cloudbreakEventService, never()).fireCloudbreakInstanceGroupEvent(anyLong(), anyString(), anyString(), eq(resourceEvent), anyCollection());
    }

    private void verifyInstanceGroupEvent(ResourceEvent resourceEvent, String... eventMessageArgs) {
        verify(cloudbreakEventService).fireCloudbreakInstanceGroupEvent(1L, Status.CREATE_IN_PROGRESS.name(), "master", resourceEvent,
                List.of(eventMessageArgs));
    }
}
