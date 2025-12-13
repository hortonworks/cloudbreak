package com.sequenceiq.cloudbreak.converter.spi;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.marketplace.AzureMarketplaceTermsClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.loadbalancer.TargetGroupPortProvider;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StackToCloudStackConverterTest {

    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final Long TEST_STACK_ID = 1L;

    private static final String TEST_USERNAME_VALUE = "username";

    private static final String TEST_PUBLICKEY_VALUE = "0123456789";

    private static final String TEST_STRING_ID = "1";

    private static final String TEST_NAME = "name";

    private static final String[] EMPTY_STRING = new String[0];

    private static final String BLUEPRINT_TEXT = "blueprintText";

    private static final String CLOUD_PLATFORM = "MOCK";

    private static final String ENV_CRN = "env-crn";

    private static final String STACK_CRN = "stack-crn";

    private static final String SUBNET_ID = "subnetId";

    private static final String LOCAL_SSD = "local-ssd";

    private static final String PD_STANDARD = "pd-standard";

    private static final String PD_SSD = "pd-ssd";

    private static final String PD_BALANCED = "pd-balanced";

    @InjectMocks
    private StackToCloudStackConverter underTest;

    @Mock
    private SecurityRuleService securityRuleService;

    @Mock
    private ImageService imageService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private Stack stack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private Cluster cluster;

    @Mock
    private Blueprint blueprint;

    @Mock
    private com.sequenceiq.cloudbreak.domain.Network stackNetwork;

    @Mock
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Mock
    private FileSystemConverter fileSystemConverter;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private CloudFileSystemViewProvider cloudFileSystemViewProvider;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TargetGroupPortProvider targetGroupPortProvider;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private AzureMarketplaceTermsClientService azureMarketplaceTermsClientService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private ResourceService resourceService;

    @BeforeEach
    public void setUp() {
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
        when(cluster.getExtendedBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(mock(Map.class));
        when(userDataService.getUserData(anyLong())).thenReturn(mock(Map.class));
        when(cloudFileSystemViewProvider.getCloudFileSystemView(any(), any(), any())).thenReturn(Optional.empty());
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Collections.emptySet());
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Collections.emptySet());
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemInClusterThenCloudFileSystemShouldBeNull() {
        when(cluster.getFileSystem()).thenReturn(null);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertFalse(result.getFileSystem().isPresent());
    }

    @Test
    public void testConvertWhenThereIsAFileSystemInClusterThenExpectedSpiFileSystemShouldPlacedInCloudStack() {
        FileSystem fileSystem = new FileSystem();
        SpiFileSystem expected = mock(SpiFileSystem.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(fileSystemConverter.fileSystemToSpi(fileSystem)).thenReturn(expected);
        when(stack.getStack()).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getFileSystem().isPresent());
        assertEquals(expected, result.getFileSystem().get());
        verify(fileSystemConverter, times(1)).fileSystemToSpi(fileSystem);
    }

    @Test
    public void testConvertWhenInstanceGroupAreContainsOnlyNullTemplatesThenStoredInstanceGroupListShouldBeEmpty() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup1 = mock(InstanceGroupView.class);
        InstanceGroupView instanceGroup2 = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup1, emptyList()));
        instanceGroups.add(new InstanceGroupDto(instanceGroup2, emptyList()));

        when(instanceGroup1.getTemplate()).thenReturn(null);
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(instanceGroup2.getTemplate()).thenReturn(null);
        when(instanceGroup2.getGroupName()).thenReturn("group2");
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateButThereIsNoNotDeletedInstanceMetaThenInstancesShouldBeEmpty() {
        List<String> az1 = List.of("us-west2-a", "us-west2-b");
        Json attributes1 = mock(Json.class);
        when(attributes1.getMap()).thenReturn(Map.of(NetworkConstants.AVAILABILITY_ZONES, az1));
        InstanceGroupNetwork instanceGroupNetwork1 = mock(InstanceGroupNetwork.class);
        when(instanceGroupNetwork1.getAttributes()).thenReturn(attributes1);


        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork1);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(template.getRootVolumeType()).thenReturn("gp2");
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().getFirst().getInstances().isEmpty());
        assertEquals(new HashSet<>(az1), result.getGroups().getFirst().getNetwork().getAvailabilityZones());
        assertEquals("gp2", result.getGroups().getFirst().getRootVolumeType());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndThereIsANotDeletedInstanceMetaThenInstancesShouldContainExpectedAmountOfElements() {

        InstanceMetadataView instanceMetaData = spy(InstanceMetadataView.class);
        String fqdnParsedName = "test1-m-1-20180605095019";
        when(instanceMetaData.getId()).thenReturn(1L);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(String.format("%s.project.id", fqdnParsedName));
        when(instanceMetaData.getSubnetId()).thenReturn(TEST_STRING_ID);
        when(instanceMetaData.getInstanceName()).thenReturn(TEST_NAME);
        when(instanceMetaData.getTerminationDate()).thenReturn(null);
        when(instanceMetaData.getUserdataSecretResourceId()).thenReturn(null);

        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, List.of(instanceMetaData)));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());

        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getInstances().size());
        assertEquals(fqdnParsedName, result.getGroups().getFirst().getInstances().getFirst().getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(instanceMetaData.getSubnetId(), result.getGroups().getFirst().getInstances().getFirst().getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(instanceMetaData.getInstanceName(),
                result.getGroups().getFirst().getInstances().getFirst().getParameters().get(CloudInstance.INSTANCE_NAME));
    }

    @Test
    public void testConvertWhenProvidingStackAuthenticationThenItsDataShouldBeStoredInMetaData() {
        StackAuthentication auth = createStackAuthentication();
        List<InstanceMetadataView> notDeletedMetas = new ArrayList<>();
        InstanceMetadataView instanceMetadata = mock(InstanceMetadataView.class);
        when(instanceMetadata.getUserdataSecretResourceId()).thenReturn(null);
        notDeletedMetas.add(instanceMetadata);
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, notDeletedMetas));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(stack.getStackAuthentication()).thenReturn(auth);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getInstances().size());
        assertEquals(auth.getLoginUserName(), result.getGroups().getFirst().getInstances().getFirst().getAuthentication().getLoginUserName());
        assertEquals(auth.getPublicKey(), result.getGroups().getFirst().getInstances().getFirst().getAuthentication().getPublicKey());
        assertEquals(auth.getPublicKeyId(), result.getGroups().getFirst().getInstances().getFirst().getAuthentication().getPublicKeyId());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndNodeCountIsNotZeroThenObtainSkeleton() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadata1 = mock(InstanceMetadataView.class);
        InstanceMetadataView instanceMetadata2 = mock(InstanceMetadataView.class);
        when(instanceMetadata1.getUserdataSecretResourceId()).thenReturn(null);
        when(instanceMetadata2.getUserdataSecretResourceId()).thenReturn(null);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, List.of(instanceMetadata1, instanceMetadata2)));
        String groupName = TEST_NAME;
        Template template = mock(Template.class);
        when(instanceMetadata1.isDeletedOnProvider()).thenReturn(true);
        when(instanceMetadata2.isDeletedOnProvider()).thenReturn(true);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Sets.newHashSet());
        when(instanceGroup.getGroupName()).thenReturn(groupName);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(groupName, result.getGroups().getFirst().getReferenceInstanceConfiguration().getTemplate().getGroupName());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndNodeCountIsZeroThenSkeletonShouldBeReturnOnReferenceInstanceConfiguration() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertNotNull(result.getGroups().getFirst().getReferenceInstanceConfiguration());
    }

    @Test
    public void testConvertWhenInstanceGroupAttributesIsNullThenEmptyMapShouldBeStoredInGroupInstance() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getAttributes()).thenReturn(null);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().getFirst().getParameters().isEmpty());
    }

    @Test
    public void testConvertWhenInstanceGroupAttributesIsNotNullThenExpectedMapShouldBeStoredInGroupInstance() {
        Json attributes = mock(Json.class);
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        Map<String, Object> expected = createMap("", Object.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(expected);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Sets.newHashSet());
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stack.getStack()).thenReturn(stack);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().getFirst().getParameters());
    }

    @Test
    public void testConvertWhenRootVolumeSizeIsNullThenDefaultRootVolumeSizeProviderShouldGiveIt() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        int expected = Integer.MAX_VALUE;
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(template.getRootVolumeSize()).thenReturn(null);
        when(defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform("AWS", true)).thenReturn(expected);
        when(stack.getStack()).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().getFirst().getRootVolumeSize());
    }

    @Test
    public void testConvertWhenRootVolumeSizeNotNullThenItsValueShouldBeStoredInGroup() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        int expected = Integer.MAX_VALUE;
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(template.getRootVolumeSize()).thenReturn(expected);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().getFirst().getRootVolumeSize());
        verify(defaultRootVolumeSizeProvider, times(0)).getDefaultRootVolumeForPlatform(any(String.class), eq(true));
    }

    @Test
    public void testConvertWhenSecurityGroupIsNullThenSecurityShouldContainsEmptyRulesInGroup() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stack.getStack()).thenReturn(stack);
        when(instanceGroup.getSecurityGroup()).thenReturn(null);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().getFirst().getSecurity().getRules().isEmpty());
        assertNull(result.getGroups().getFirst().getSecurity().getCloudSecurityId());
    }

    @Test
    public void testConvertWhenSecurityGroupIsNotNullButSecurityRuleRepositoryCantFindAnyRulesThenSecurityShouldContainsEmptyRulesInGroup() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityGroupIds(Collections.singleton(TEST_STRING_ID));
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(securityRuleService.findAllBySecurityGroupId(securityGroup.getId())).thenReturn(emptyList());

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().getFirst().getSecurity().getRules().isEmpty());
        assertEquals(securityGroup.getFirstSecurityGroupId(), result.getGroups().getFirst().getSecurity().getCloudSecurityId());
    }

    @Test
    public void testConvertWhenSecurityGroupIsNotNullAndSecurityRuleRepositoryCanFindRulesButThereIsNoPortDefinitionThenEmptyPortDefinitionShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        when(securityRule.getPorts()).thenReturn(EMPTY_STRING);
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(stack.getStack()).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getSecurity().getRules().size());
        assertEquals(0L, result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts().length);
    }

    @Test
    public void testConvertWhenThereArePortDefinitionsInSecurityRulesAndSegmentsLengthIsGreaterThanOneThenExpectedPartsShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        String[] ports = new String[1];
        ports[0] = "1234-5678";
        when(securityRule.getPorts()).thenReturn(ports);
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getSecurity().getRules().size());
        assertEquals(1L, result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts().length);
        assertEquals(ports[0].split("-")[0], result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts()[0].getFrom());
        assertEquals(ports[0].split("-")[1], result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts()[0].getTo());
    }

    @Test
    public void testConvertWhenThereArePortDefinitionsInSecurityRulesButSegmentsLengthIsOneThanOneThenExpectedPartsShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        String[] ports = new String[1];
        ports[0] = "1234";
        when(securityRule.getPorts()).thenReturn(ports);
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getSecurity().getRules().size());
        assertEquals(1L, result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts().length);
        assertEquals(ports[0], result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts()[0].getFrom());
        assertEquals(ports[0], result.getGroups().getFirst().getSecurity().getRules().getFirst().getPorts()[0].getTo());
    }

    @Test
    public void testConvertWhenStackNetworkIsNullThenNullNetworkShouldBeStored() {
        when(stack.getNetwork()).thenReturn(null);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertNull(result.getNetwork());
    }

    @Test
    public void testConvertWhenStackNetworkNotNullButStackNetworkAttributesAreNullThenEmptyMapShouldBeSavedInNetworkParams() {
        String subnetCIDR = "testSubnetCIDR";
        when(stack.getNetwork()).thenReturn(stackNetwork);
        when(stack.getStack()).thenReturn(stack);
        when(stackNetwork.getSubnetCIDR()).thenReturn(subnetCIDR);
        when(stackNetwork.getAttributes()).thenReturn(null);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertNotNull(result.getNetwork());
        assertEquals(subnetCIDR, result.getNetwork().getSubnet().getCidr());
        assertTrue(result.getNetwork().getParameters().isEmpty());
    }

    @Test
    public void testConvertWhenStackNetworkNotNullAndStackNetworkAttributesAreNotNullThenExpectedMapShouldBeSavedInNetworkParams() {
        String subnetCIDR = "testSubnetCIDR";
        Json attributes = mock(Json.class);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        Map<String, Object> params = new LinkedHashMap<>();
        when(stack.getNetwork()).thenReturn(stackNetwork);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stackNetwork.getSubnetCIDR()).thenReturn(subnetCIDR);
        when(stackNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(params);

        CloudStack result = underTest.convert(stack);

        assertNotNull(result.getNetwork());
        assertEquals(subnetCIDR, result.getNetwork().getSubnet().getCidr());
        assertEquals(params, result.getNetwork().getParameters());
    }

    @Test
    public void testConvertWhenImageServiceCanProvideImageThenThisShouldBeStored() throws CloudbreakImageNotFoundException {
        Image expected = mock(Image.class);
        Long stackId = 1L;
        when(stack.getStack()).thenReturn(stack);
        when(stack.getId()).thenReturn(stackId);
        when(imageService.getImage(stackId)).thenReturn(expected);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);

        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getImage());
    }

    @Test
    public void testConvertWhenImageServiceCantObtainImageAndThrowsCloudbreakImageNotFoundExceptionThenNullImageShouldBeStored()
            throws CloudbreakImageNotFoundException {
        Long stackId = 1L;
        when(stack.getStack()).thenReturn(stack);
        when(stack.getId()).thenReturn(stackId);
        when(imageService.getImage(stackId)).thenThrow(new CloudbreakImageNotFoundException("not found"));

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertNull(result.getImage());
    }

    @Test
    public void testConvertWhenStackPassingItsParametersThenThoseShouldBeStored() {
        Map<String, String> expected = Map.of(PlatformParametersConsts.RESOURCE_CRN_PARAMETER, STACK_CRN);
        when(stack.getParameters()).thenReturn(expected);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getParameters());
    }

    @Test
    public void testConvertWhenStackTagsParameterIsNullThenEmptyMapShouldByStored() {
        when(stack.getTags()).thenReturn(null);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenStackTagsNotNullButDoesNotContainsNeitherUserDefinedOrDefaultTagsThenEmptyMapsShouldBeStored() throws IOException {
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(Collections.emptyMap());
        when(stackTags.getDefaultTags()).thenReturn(Collections.emptyMap());
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenStackTagsNotNullButBothUserDefinedAndDefaultTagsAreNullThenEmptyMapsShouldBeStored() throws IOException {
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getTags()).thenReturn(tags);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(null);
        when(stackTags.getDefaultTags()).thenReturn(null);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenTagsContainsOnlyUserDefinedTagsThenOnlyThoseWillBeStored() throws IOException {
        Map<String, String> userDefinedTags = createMap("userDefined", String.class);
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(stack.getStack()).thenReturn(stack);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(userDefinedTags);
        when(stackTags.getDefaultTags()).thenReturn(null);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(userDefinedTags.size(), result.getTags().size());
        userDefinedTags.forEach((key, value) -> {
            assertTrue(result.getTags().containsKey(key));
            assertEquals(value, result.getTags().get(key));
        });
    }

    @Test
    public void testConvertWhenTagsContainsOnlyDefaultTagsThenOnlyThoseWillBeSaved() throws IOException {
        Map<String, String> defaultTags = createMap("default", String.class);
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(stack.getStack()).thenReturn(stack);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(null);
        when(stackTags.getDefaultTags()).thenReturn(defaultTags);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(defaultTags.size(), result.getTags().size());
        defaultTags.forEach((key, value) -> {
            assertTrue(result.getTags().containsKey(key));
            assertEquals(value, result.getTags().get(key));
        });
    }

    @Test
    public void testConvertWhenTagsContainsBothUserDefinedAndDefaultTagsThenBothShouldBeSaved() throws IOException {
        Map<String, String> defaultTags = createMap("default", String.class);
        Map<String, String> userDefined = createMap("userDefined", String.class);
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(stack.getStack()).thenReturn(stack);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(userDefined);
        when(stackTags.getDefaultTags()).thenReturn(defaultTags);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(defaultTags.size() + userDefined.size(), result.getTags().size());
        defaultTags.forEach((key, value) -> {
            assertTrue(result.getTags().containsKey(key));
            assertEquals(value, result.getTags().get(key));
        });
        userDefined.forEach((key, value) -> {
            assertTrue(result.getTags().containsKey(key));
            assertEquals(value, result.getTags().get(key));
        });
    }

    @Test
    public void testConvertWhenUnableToParseJsonToStackTagsThenEmptyMapShouldBeStoredAsTags() throws IOException {
        Json tags = mock(Json.class);
        when(stack.getTags()).thenReturn(tags);
        when(stack.getStack()).thenReturn(stack);
        when(tags.get(StackTags.class)).thenThrow(new IOException("failed to parse json to StackTags"));

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenComponentConfigProviderGivesNullStackTemplateThenNullTemplateShouldBeSaved() {
        when(stack.getId()).thenReturn(TEST_STACK_ID);
        when(componentConfigProviderService.getStackTemplate(TEST_STACK_ID)).thenReturn(null);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertNull(result.getTemplate());
    }

    @Test
    public void testConvertWhenComponentConfigProviderGivesValidStackTemplateThenItsTemplateShouldBeSaved() {
        StackTemplate stackTemplate = mock(StackTemplate.class);
        String expected = "template";
        when(stackTemplate.getTemplate()).thenReturn(expected);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getId()).thenReturn(TEST_STACK_ID);
        when(componentConfigProviderService.getStackTemplate(TEST_STACK_ID)).thenReturn(stackTemplate);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getTemplate());
    }

    @Test
    public void testConvertWhenPassingStackAuthenticationThenItsPassedValueShouldBeStoredInInstanceAuthentication() {
        StackAuthentication stackAuthentication = createStackAuthentication();
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(stackAuthentication.getLoginUserName(), result.getInstanceAuthentication().getLoginUserName());
        assertEquals(stackAuthentication.getPublicKeyId(), result.getInstanceAuthentication().getPublicKeyId());
        assertEquals(stackAuthentication.getPublicKey(), result.getInstanceAuthentication().getPublicKey());
    }

    @Test
    public void testConvertWhenProvidingAuthenticationLoginUserNameAndPublicKeyThenTheseValuesShouldBePassed() {
        StackAuthentication authentication = createStackAuthentication();
        when(stack.getStackAuthentication()).thenReturn(authentication);
        when(stack.getStack()).thenReturn(stack);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(authentication.getLoginUserName(), result.getLoginUserName());
        assertEquals(authentication.getPublicKey(), result.getPublicKey());
    }

    @Test
    void testBuildInstanceTemplateVolumeTemplatesAreOrderedByVolumeTemplateId() {
        Template template = new Template();
        template.setInstanceType("m5.xlarge");

        VolumeTemplate vol1 = new VolumeTemplate();
        vol1.setId(1L);
        vol1.setVolumeCount(1);
        vol1.setVolumeType("gp3");
        vol1.setVolumeSize(100);
        vol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate vol3 = new VolumeTemplate();
        vol3.setId(3L);
        vol3.setVolumeCount(1);
        vol3.setVolumeType("gp3");
        vol3.setVolumeSize(300);
        vol3.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate vol2 = new VolumeTemplate();
        vol2.setId(2L);
        vol2.setVolumeCount(2);
        vol2.setVolumeType("gp3");
        vol2.setVolumeSize(200);
        vol2.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(vol1, vol3, vol2));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "master", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(4, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(200, volumes.get(1).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "3", volumes.get(2).getMount());
        assertEquals(200, volumes.get(2).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "4", volumes.get(3).getMount());
        assertEquals(300, volumes.get(3).getSize());
    }

    @Test
    void testBuildInstanceTemplateWithMultipleVolumesWithSameType() {
        Template template = new Template();
        template.setInstanceType("m5.xlarge");

        VolumeTemplate vol1 = new VolumeTemplate();
        vol1.setId(1L);
        vol1.setVolumeCount(2);
        vol1.setVolumeType("gp2");
        vol1.setVolumeSize(100);
        vol1.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(vol1));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "master", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(2, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(100, volumes.get(1).getSize());
    }

    @Test
    void testBuildInstanceTemplateEphemeralVolumesArePlacedLast() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate regularVol1 = new VolumeTemplate();
        regularVol1.setId(1L);
        regularVol1.setVolumeCount(1);
        regularVol1.setVolumeType(PD_STANDARD);
        regularVol1.setVolumeSize(100);
        regularVol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol = new VolumeTemplate();
        ephemeralVol.setId(2L);
        ephemeralVol.setVolumeCount(1);
        ephemeralVol.setVolumeType(LOCAL_SSD);
        ephemeralVol.setVolumeSize(375);
        ephemeralVol.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate regularVol2 = new VolumeTemplate();
        regularVol2.setId(3L);
        regularVol2.setVolumeCount(1);
        regularVol2.setVolumeType(PD_SSD);
        regularVol2.setVolumeSize(200);
        regularVol2.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(regularVol1, ephemeralVol, regularVol2));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(3, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(PD_STANDARD, volumes.get(0).getType());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(PD_SSD, volumes.get(1).getType());
        assertEquals(200, volumes.get(1).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "3", volumes.get(2).getMount());
        assertEquals(LOCAL_SSD, volumes.get(2).getType());
        assertEquals(375, volumes.get(2).getSize());
    }

    @Test
    void testBuildInstanceTemplateMultipleEphemeralVolumesAreSortedById() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate regularVol = new VolumeTemplate();
        regularVol.setId(1L);
        regularVol.setVolumeCount(1);
        regularVol.setVolumeType(PD_STANDARD);
        regularVol.setVolumeSize(100);
        regularVol.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol2 = new VolumeTemplate();
        ephemeralVol2.setId(4L);
        ephemeralVol2.setVolumeCount(1);
        ephemeralVol2.setVolumeType(LOCAL_SSD);
        ephemeralVol2.setVolumeSize(375);
        ephemeralVol2.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol1 = new VolumeTemplate();
        ephemeralVol1.setId(2L);
        ephemeralVol1.setVolumeCount(2);
        ephemeralVol1.setVolumeType(LOCAL_SSD);
        ephemeralVol1.setVolumeSize(375);
        ephemeralVol1.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(regularVol, ephemeralVol2, ephemeralVol1));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(4, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(PD_STANDARD, volumes.get(0).getType());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(LOCAL_SSD, volumes.get(1).getType());
        assertEquals(375, volumes.get(1).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "3", volumes.get(2).getMount());
        assertEquals(LOCAL_SSD, volumes.get(2).getType());
        assertEquals(375, volumes.get(2).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "4", volumes.get(3).getMount());
        assertEquals(LOCAL_SSD, volumes.get(3).getType());
        assertEquals(375, volumes.get(3).getSize());
    }

    @Test
    void testBuildInstanceTemplateOnlyEphemeralVolumes() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate ephemeralVol1 = new VolumeTemplate();
        ephemeralVol1.setId(1L);
        ephemeralVol1.setVolumeCount(1);
        ephemeralVol1.setVolumeType(LOCAL_SSD);
        ephemeralVol1.setVolumeSize(375);
        ephemeralVol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol2 = new VolumeTemplate();
        ephemeralVol2.setId(2L);
        ephemeralVol2.setVolumeCount(1);
        ephemeralVol2.setVolumeType(LOCAL_SSD);
        ephemeralVol2.setVolumeSize(375);
        ephemeralVol2.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(ephemeralVol1, ephemeralVol2));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(2, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(LOCAL_SSD, volumes.get(0).getType());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(LOCAL_SSD, volumes.get(1).getType());
    }

    @Test
    void testBuildInstanceTemplateEphemeralVolumesWithMultipleCountsArePlacedLast() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate regularVol1 = new VolumeTemplate();
        regularVol1.setId(1L);
        regularVol1.setVolumeCount(2);
        regularVol1.setVolumeType(PD_STANDARD);
        regularVol1.setVolumeSize(100);
        regularVol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol = new VolumeTemplate();
        ephemeralVol.setId(2L);
        ephemeralVol.setVolumeCount(3);
        ephemeralVol.setVolumeType(LOCAL_SSD);
        ephemeralVol.setVolumeSize(375);
        ephemeralVol.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate regularVol2 = new VolumeTemplate();
        regularVol2.setId(3L);
        regularVol2.setVolumeCount(1);
        regularVol2.setVolumeType(PD_SSD);
        regularVol2.setVolumeSize(200);
        regularVol2.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(regularVol1, ephemeralVol, regularVol2));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(6, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(PD_STANDARD, volumes.get(0).getType());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(PD_STANDARD, volumes.get(1).getType());
        assertEquals(100, volumes.get(1).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "3", volumes.get(2).getMount());
        assertEquals(PD_SSD, volumes.get(2).getType());
        assertEquals(200, volumes.get(2).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "4", volumes.get(3).getMount());
        assertEquals(LOCAL_SSD, volumes.get(3).getType());
        assertEquals(375, volumes.get(3).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "5", volumes.get(4).getMount());
        assertEquals(LOCAL_SSD, volumes.get(4).getType());
        assertEquals(375, volumes.get(4).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "6", volumes.get(5).getMount());
        assertEquals(LOCAL_SSD, volumes.get(5).getType());
        assertEquals(375, volumes.get(5).getSize());
    }

    @Test
    void testBuildInstanceTemplateMixedVolumesWithNonSequentialIds() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate regularVol1 = new VolumeTemplate();
        regularVol1.setId(5L);
        regularVol1.setVolumeCount(1);
        regularVol1.setVolumeType(PD_BALANCED);
        regularVol1.setVolumeSize(150);
        regularVol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol1 = new VolumeTemplate();
        ephemeralVol1.setId(10L);
        ephemeralVol1.setVolumeCount(1);
        ephemeralVol1.setVolumeType(LOCAL_SSD);
        ephemeralVol1.setVolumeSize(375);
        ephemeralVol1.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate regularVol2 = new VolumeTemplate();
        regularVol2.setId(3L);
        regularVol2.setVolumeCount(1);
        regularVol2.setVolumeType(PD_STANDARD);
        regularVol2.setVolumeSize(100);
        regularVol2.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate ephemeralVol2 = new VolumeTemplate();
        ephemeralVol2.setId(7L);
        ephemeralVol2.setVolumeCount(1);
        ephemeralVol2.setVolumeType(LOCAL_SSD);
        ephemeralVol2.setVolumeSize(375);
        ephemeralVol2.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate regularVol3 = new VolumeTemplate();
        regularVol3.setId(8L);
        regularVol3.setVolumeCount(1);
        regularVol3.setVolumeType(PD_SSD);
        regularVol3.setVolumeSize(200);
        regularVol3.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(regularVol1, ephemeralVol1, regularVol2, ephemeralVol2, regularVol3));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(5, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(PD_STANDARD, volumes.get(0).getType());
        assertEquals(100, volumes.get(0).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(1).getMount());
        assertEquals(PD_BALANCED, volumes.get(1).getType());
        assertEquals(150, volumes.get(1).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "3", volumes.get(2).getMount());
        assertEquals(PD_SSD, volumes.get(2).getType());
        assertEquals(200, volumes.get(2).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "4", volumes.get(3).getMount());
        assertEquals(LOCAL_SSD, volumes.get(3).getType());
        assertEquals(375, volumes.get(3).getSize());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "5", volumes.get(4).getMount());
        assertEquals(LOCAL_SSD, volumes.get(4).getType());
        assertEquals(375, volumes.get(4).getSize());
    }

    @Test
    void testBuildInstanceTemplateDatabaseVolumesNotAffectedByEphemeralOrdering() {
        Template template = new Template();
        template.setInstanceType("n2-standard-8");

        VolumeTemplate regularVol = new VolumeTemplate();
        regularVol.setId(1L);
        regularVol.setVolumeCount(1);
        regularVol.setVolumeType(PD_STANDARD);
        regularVol.setVolumeSize(100);
        regularVol.setUsageType(VolumeUsageType.GENERAL);

        VolumeTemplate databaseVol = new VolumeTemplate();
        databaseVol.setId(2L);
        databaseVol.setVolumeCount(1);
        databaseVol.setVolumeType(PD_SSD);
        databaseVol.setVolumeSize(500);
        databaseVol.setUsageType(VolumeUsageType.DATABASE);

        VolumeTemplate ephemeralVol = new VolumeTemplate();
        ephemeralVol.setId(3L);
        ephemeralVol.setVolumeCount(1);
        ephemeralVol.setVolumeType(LOCAL_SSD);
        ephemeralVol.setVolumeSize(375);
        ephemeralVol.setUsageType(VolumeUsageType.GENERAL);

        template.setVolumeTemplates(Set.of(regularVol, databaseVol, ephemeralVol));

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(template, "worker", 1L, InstanceStatus.CREATE_REQUESTED, "image-id");

        List<Volume> volumes = instanceTemplate.getVolumes();
        assertEquals(3, volumes.size());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "1", volumes.get(0).getMount());
        assertEquals(PD_STANDARD, volumes.get(0).getType());
        assertEquals(VolumeUtils.DATABASE_VOLUME, volumes.get(1).getMount());
        assertEquals(PD_SSD, volumes.get(1).getType());
        assertEquals(VolumeUtils.VOLUME_PREFIX + "2", volumes.get(2).getMount());
        assertEquals(LOCAL_SSD, volumes.get(2).getType());
    }

    @Test
    public void testConvertWithKnoxLoadBalancer() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup1 = mock(InstanceGroupView.class);
        InstanceGroupView instanceGroup2 = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup1, emptyList()));
        instanceGroups.add(new InstanceGroupDto(instanceGroup2, emptyList()));
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(instanceGroup2.getGroupName()).thenReturn("group2");
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(instanceGroup1.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(instanceGroup2.getTemplate()).thenReturn(template);
        TargetGroup targetGroup = mock(TargetGroup.class);
        when(targetGroup.getType()).thenReturn(TargetGroupType.KNOX);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        when(loadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(loadBalancer.getId()).thenReturn(1L);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Set.of(targetGroup));
        when(instanceGroupService.findByTargetGroupId(anyLong())).thenReturn(List.of(instanceGroup1, instanceGroup2));
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 8443);
        when(targetGroupPortProvider.getTargetGroupPortPairs(any(TargetGroup.class))).thenReturn(Set.of(targetGroupPortPair));

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1, result.getLoadBalancers().size());
        CloudLoadBalancer cloudLoadBalancer = result.getLoadBalancers().iterator().next();
        assertEquals(LoadBalancerType.PRIVATE, cloudLoadBalancer.getType());
        assertEquals(Set.of(targetGroupPortPair), cloudLoadBalancer.getPortToTargetGroupMapping().keySet());
        Set<String> groupNames = cloudLoadBalancer.getPortToTargetGroupMapping().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .stream()
                .map(Group::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("group1", "group2"), groupNames);
    }

    @Test
    public void testConvertWithMultipleKnoxLoadBalancers() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup1 = mock(InstanceGroupView.class);
        InstanceGroupView instanceGroup2 = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup1, emptyList()));
        instanceGroups.add(new InstanceGroupDto(instanceGroup2, emptyList()));
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(instanceGroup2.getGroupName()).thenReturn("group2");
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(instanceGroup1.getTemplate()).thenReturn(template);
        when(instanceGroup2.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        TargetGroup targetGroup = mock(TargetGroup.class);
        when(targetGroup.getType()).thenReturn(TargetGroupType.KNOX);
        LoadBalancer internalLoadBalancer = mock(LoadBalancer.class);
        when(internalLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(internalLoadBalancer.getId()).thenReturn(1L);
        LoadBalancer externalLoadBalancer = mock(LoadBalancer.class);
        when(externalLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(externalLoadBalancer.getId()).thenReturn(2L);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(internalLoadBalancer, externalLoadBalancer));
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Set.of(targetGroup));
        when(instanceGroupService.findByTargetGroupId(anyLong())).thenReturn(List.of(instanceGroup1, instanceGroup2));
        when(targetGroupPortProvider.getTargetGroupPortPairs(any(TargetGroup.class))).thenReturn(Set.of(new TargetGroupPortPair(443, 8443)));
        when(stack.getStack()).thenReturn(stack);

        CloudStack result = underTest.convert(stack);

        assertEquals(2, result.getLoadBalancers().size());
        Optional<CloudLoadBalancer> internalCloudLoadBalancer = result.getLoadBalancers().stream()
                .filter(lb -> lb.getType() == LoadBalancerType.PRIVATE)
                .findFirst();
        assertTrue(internalCloudLoadBalancer.isPresent());
        Optional<CloudLoadBalancer> externalCloudLoadBalancer = result.getLoadBalancers().stream()
                .filter(lb -> lb.getType() == LoadBalancerType.PUBLIC)
                .findFirst();
        assertTrue(externalCloudLoadBalancer.isPresent());
    }

    @Test
    public void testConvertWithLoadBalancerSkuSet() {
        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup1 = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup1, emptyList()));
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        TargetGroup targetGroup = mock(TargetGroup.class);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 8443);

        when(stack.getStack()).thenReturn(stack);
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(instanceGroup1.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(targetGroup.getType()).thenReturn(TargetGroupType.KNOX);
        when(loadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(loadBalancer.getId()).thenReturn(1L);
        when(loadBalancer.getSku()).thenReturn(LoadBalancerSku.STANDARD);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Set.of(targetGroup));
        when(instanceGroupService.findByTargetGroupId(anyLong())).thenReturn(List.of(instanceGroup1));
        when(targetGroupPortProvider.getTargetGroupPortPairs(any(TargetGroup.class))).thenReturn(Set.of(targetGroupPortPair));

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1, result.getLoadBalancers().size());
        CloudLoadBalancer cloudLoadBalancer = result.getLoadBalancers().iterator().next();
        assertEquals(LoadBalancerSku.STANDARD, cloudLoadBalancer.getSku());
    }

    @Test
    public void testConvertWithTerminatedInstances() {
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());

        InstanceMetadataView instanceMetaData = mock(InstanceMetadataView.class);
        String fqdnParsedName = "test1-m-1-20180605095019";
        when(instanceMetaData.getId()).thenReturn(1L);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(String.format("%s.project.id", fqdnParsedName));
        when(instanceMetaData.getSubnetId()).thenReturn(TEST_STRING_ID);
        when(instanceMetaData.getInstanceName()).thenReturn(TEST_NAME);
        when(instanceMetaData.getShortHostname()).thenReturn(fqdnParsedName);
        when(instanceMetaData.getUserdataSecretResourceId()).thenReturn(null);

        InstanceMetadataView terminatedMetaData = mock(InstanceMetadataView.class);
        String terminatedFqdnParsedName = "test1-m-1-20200401095019";
        when(terminatedMetaData.getId()).thenReturn(2L);
        when(terminatedMetaData.getDiscoveryFQDN()).thenReturn(String.format("%s.project.id", terminatedFqdnParsedName));
        when(terminatedMetaData.getSubnetId()).thenReturn(TEST_STRING_ID);
        when(terminatedMetaData.getInstanceName()).thenReturn("terminated-" + TEST_NAME);
        when(terminatedMetaData.isDeletedOnProvider()).thenReturn(true);
        when(terminatedMetaData.getShortHostname()).thenReturn(terminatedFqdnParsedName);
        when(terminatedMetaData.getUserdataSecretResourceId()).thenReturn(null);
        List<InstanceMetadataView> metas = List.of(instanceMetaData, terminatedMetaData);

        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        instanceGroups.add(new InstanceGroupDto(instanceGroup, metas));

        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().getFirst().getInstances().size());
        assertEquals(1L, result.getGroups().getFirst().getDeletedInstances().size());
        assertEquals(fqdnParsedName,
                result.getGroups().getFirst().getInstances().getFirst().getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(instanceMetaData.getSubnetId(),
                result.getGroups().getFirst().getInstances().getFirst().getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(instanceMetaData.getInstanceName(),
                result.getGroups().getFirst().getInstances().getFirst().getParameters().get(CloudInstance.INSTANCE_NAME));
        assertEquals(terminatedFqdnParsedName,
                result.getGroups().getFirst().getDeletedInstances().getFirst().getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(terminatedMetaData.getSubnetId(),
                result.getGroups().getFirst().getDeletedInstances().getFirst().getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(terminatedMetaData.getInstanceName(),
                result.getGroups().getFirst().getDeletedInstances().getFirst().getParameters().get(CloudInstance.INSTANCE_NAME));
    }

    static Object[][] buildInstanceTestWhenInstanceMetaDataPresentAndSubnetAndAvailabilityZoneDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone
                {"subnetId=null, availabilityZone=null", null, null},
                {"subnetId=\"subnet-1\", availabilityZone=null", "subnet-1", null},
                {"subnetId=\"subnet-1\", availabilityZone=\"az-1\"", "subnet-1", "az-1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buildInstanceTestWhenInstanceMetaDataPresentAndSubnetAndAvailabilityZoneDataProvider")
    void buildInstanceTestWhenInstanceMetaDataPresentAndSubnetAndAvailabilityZone(String testCaseName, String subnetId, String availabilityZone) {
        InstanceMetadataView instanceMetaData = mock(InstanceMetadataView.class);
        when(instanceMetaData.getInstanceId()).thenReturn("i-1234");
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("vm.empire.com");
        when(instanceMetaData.getInstanceName()).thenReturn("worker3");
        when(instanceMetaData.getSubnetId()).thenReturn(subnetId);
        when(instanceMetaData.getAvailabilityZone()).thenReturn(availabilityZone);
        when(instanceMetaData.getShortHostname()).thenReturn("vm");
        when(instanceMetaData.getUserdataSecretResourceId()).thenReturn(null);

        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        Template template = mock(Template.class);
        when(template.getCloudPlatform()).thenReturn("AWS");
        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(instanceGroup.getTemplate()).thenReturn(template);

        when(instanceGroup.getGroupName()).thenReturn("worker");
        when(stack.getStack()).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(instanceMetadataToImageIdConverter.convert(instanceMetaData)).thenReturn("image-12");

        CloudInstance cloudInstance = underTest.buildInstance(instanceMetaData, instanceGroup, stack, 12L, InstanceStatus.CREATED,
                new DetailedEnvironmentResponse());

        verifyCloudInstanceWhenInstanceMetaDataPresent(cloudInstance, subnetId, availabilityZone);
    }

    private void verifyCloudInstanceWhenInstanceMetaDataPresent(CloudInstance cloudInstance, String subnetIdExpected, String availabilityZoneExpected) {
        assertThat(cloudInstance).isNotNull();
        assertThat(cloudInstance.getInstanceId()).isEqualTo("i-1234");

        InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
        assertThat(instanceTemplate).isNotNull();
        assertThat(instanceTemplate.getPrivateId()).isEqualTo(12L);
        assertThat(instanceTemplate.getGroupName()).isEqualTo("worker");
        assertThat(instanceTemplate.getStatus()).isEqualTo(InstanceStatus.CREATED);
        assertThat(instanceTemplate.getImageId()).isEqualTo("image-12");

        assertThat(cloudInstance.getAuthentication()).isNotNull();

        assertThat(cloudInstance.getSubnetId()).isEqualTo(subnetIdExpected);
        assertThat(cloudInstance.getAvailabilityZone()).isEqualTo(availabilityZoneExpected);

        Map<String, Object> parameters = cloudInstance.getParameters();
        assertThat(parameters).isNotNull();
        assertThat(parameters.get(CloudInstance.DISCOVERY_NAME)).isEqualTo("vm");
        assertThat(parameters.get(SUBNET_ID)).isEqualTo(subnetIdExpected);
        assertThat(parameters.get(CloudInstance.INSTANCE_NAME)).isEqualTo("worker3");
        assertThat(parameters.get(CloudInstance.FQDN)).isEqualTo("vm.empire.com");
    }

    @Test
    void testUpdateWithVerticalScaleRequest() {
        InstanceTemplate instanceTemplate1 = new InstanceTemplate("small", null, 1L, Set.of(), null, null, 0L, null, null, 0L);
        InstanceTemplate instanceTemplate2 = new InstanceTemplate("small", null, 2L, Set.of(), null, null, 0L, null, null, 0L);
        CloudInstance instance1 = new CloudInstance("instance1", instanceTemplate1, null, null, null);
        CloudInstance instance2 = new CloudInstance("instance2", instanceTemplate2, null, null, null);
        Group group1 = Group.builder()
                .withName("group1")
                .withInstances(Set.of(instance1, instance2))
                .withRootVolumeSize(100)
                .build();
        Group group2 = Group.builder()
                .withName("group2")
                .withRootVolumeSize(100)
                .build();
        CloudStack cloudStack = CloudStack.builder()
                .groups(Set.of(group1, group2))
                .build();
        StackVerticalScaleV4Request verticalScaleRequest = getStackVerticalScaleV4Request();

        CloudStack result = underTest.updateWithVerticalScaleRequest(cloudStack, verticalScaleRequest);

        assertEquals("very_large", instanceTemplate1.getFlavor());
        assertEquals("very_large", instanceTemplate2.getFlavor());
        assertEquals(200, group1.getRootVolumeSize());
        assertEquals(100, group2.getRootVolumeSize());
    }

    @Test
    void testUpdateWithVerticalScaleRequestWithZeroInstanceGroup() {
        InstanceTemplate skeletonTemplate = new InstanceTemplate("small", null, 0L, Set.of(), null, null, 0L, null, null, 0L);
        CloudInstance skeleton = new CloudInstance("skeleton", skeletonTemplate, null, null, null);
        Group group1 = Group.builder()
                .withName("group1")
                .withSkeleton(skeleton)
                .withRootVolumeSize(100)
                .build();
        Group group2 = Group.builder()
                .withName("group2")
                .withRootVolumeSize(100)
                .build();
        CloudStack cloudStack = CloudStack.builder()
                .groups(Set.of(group1, group2))
                .build();
        StackVerticalScaleV4Request verticalScaleRequest = getStackVerticalScaleV4Request();

        CloudStack result = underTest.updateWithVerticalScaleRequest(cloudStack, verticalScaleRequest);

        assertEquals("very_large", skeletonTemplate.getFlavor());
        assertEquals(200, group1.getRootVolumeSize());
        assertEquals(100, group2.getRootVolumeSize());
    }

    private static StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        StackVerticalScaleV4Request verticalScaleRequest = new StackVerticalScaleV4Request();
        verticalScaleRequest.setGroup("group1");
        InstanceTemplateV4Request instanceTemplateRequest = new InstanceTemplateV4Request();
        instanceTemplateRequest.setInstanceType("very_large");
        RootVolumeV4Request rootVolumeRequest = new RootVolumeV4Request();
        rootVolumeRequest.setSize(200);
        instanceTemplateRequest.setRootVolume(rootVolumeRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }

    private StackAuthentication createStackAuthentication() {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(TEST_USERNAME_VALUE);
        stackAuthentication.setPublicKey(TEST_PUBLICKEY_VALUE);
        stackAuthentication.setPublicKeyId("key-id");
        return stackAuthentication;
    }

    private <T> Map<String, T> createMap(String keyPrefix, Class<T> clazz) {
        Map<String, T> map = new LinkedHashMap<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            map.put(String.format("%s-key-%s", keyPrefix, i), clazz.cast(String.format("key-%s", i)));
        }
        return map;
    }
}
