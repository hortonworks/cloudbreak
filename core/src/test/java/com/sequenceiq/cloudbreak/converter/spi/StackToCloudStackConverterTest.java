package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
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

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String DISCOVERY_FQDN = "host.foo.org";

    private static final String DISCOVERY_NAME = "host";

    private static final String SUBNET_ID = "subnetId";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String INSTANCE_NAME = "instanceName";

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
    private ConverterUtil converterUtil;

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
    private EnvironmentClientService environmentClientService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @BeforeEach
    public void setUp() {
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        when(cluster.getExtendedBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Mockito.mock(Map.class));
        when(cloudFileSystemViewProvider.getCloudFileSystemView(any(), any(), any())).thenReturn(Optional.empty());
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Collections.emptySet());
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Collections.emptySet());
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemInClusterThenCloudFileSystemShouldBeNull() {
        when(cluster.getFileSystem()).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertFalse(result.getFileSystem().isPresent());
        verify(converterUtil, times(0)).convert(any(FileSystem.class), eq(SpiFileSystem.class));
    }

    @Test

    public void testConvertWhenThereIsAFileSystemInClusterThenExpectedSpiFileSystemShouldPlacedInCloudStack() {
        FileSystem fileSystem = new FileSystem();
        SpiFileSystem expected = mock(SpiFileSystem.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(fileSystemConverter.fileSystemToSpi(fileSystem)).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getFileSystem().isPresent());
        assertEquals(expected, result.getFileSystem().get());
        verify(fileSystemConverter, times(1)).fileSystemToSpi(fileSystem);
    }

    @Test
    public void testConvertWhenInstanceGroupAreContainsOnlyNullTemplatesThenStoredInstanceGroupListShouldBeEmpty() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup1 = mock(InstanceGroup.class);
        InstanceGroup instanceGroup2 = mock(InstanceGroup.class);
        when(instanceGroup1.getTemplate()).thenReturn(null);
        when(instanceGroup2.getTemplate()).thenReturn(null);
        instanceGroups.add(instanceGroup1);
        instanceGroups.add(instanceGroup2);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateButThereIsNoNotDeletedInstanceMetaThenInstancesShouldBeEmpty() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        instanceGroups.add(instanceGroup);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().get(0).getInstances().isEmpty());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndThereIsANotDeletedInstanceMetaThenInstancesShouldContainExpectedAmountOfElements() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        instanceGroups.add(instanceGroup);

        InstanceMetaData metaData = new InstanceMetaData();
        String fqdnParsedName = "test1-m-1-20180605095019";
        metaData.setId(1L);
        metaData.setDiscoveryFQDN(String.format("%s.project.id", fqdnParsedName));
        metaData.setSubnetId(TEST_STRING_ID);
        metaData.setInstanceName(TEST_NAME);
        Set<InstanceMetaData> notDeletedMetas = new LinkedHashSet<>();
        notDeletedMetas.add(metaData);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(notDeletedMetas);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getInstances().size());
        assertEquals(fqdnParsedName, result.getGroups().get(0).getInstances().get(0).getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(metaData.getSubnetId(), result.getGroups().get(0).getInstances().get(0).getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(metaData.getInstanceName(), result.getGroups().get(0).getInstances().get(0).getParameters().get(CloudInstance.INSTANCE_NAME));
    }

    @Test
    public void testConvertWhenProvidingStackAuthenticationThenItsDataShouldBeStoredInMetaData() {
        StackAuthentication auth = createStackAuthentication();
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        instanceGroups.add(instanceGroup);
        Set<InstanceMetaData> notDeletedMetas = new LinkedHashSet<>();
        notDeletedMetas.add(new InstanceMetaData());
        when(stack.getStackAuthentication()).thenReturn(auth);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(notDeletedMetas);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getInstances().size());
        assertEquals(auth.getLoginUserName(), result.getGroups().get(0).getInstances().get(0).getAuthentication().getLoginUserName());
        assertEquals(auth.getPublicKey(), result.getGroups().get(0).getInstances().get(0).getAuthentication().getPublicKey());
        assertEquals(auth.getPublicKeyId(), result.getGroups().get(0).getInstances().get(0).getAuthentication().getPublicKeyId());
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndNodeCountIsNotZeroThenUnableToObtainSkeletonSinceNoSkeletonAndInstance() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        String groupName = TEST_NAME;
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        instanceGroups.add(instanceGroup);
        when(instanceGroup.getNodeCount()).thenReturn(2);
        when(instanceGroup.getGroupName()).thenReturn(groupName);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> result.getGroups().get(0).getReferenceInstanceConfiguration());
        assertThat(runtimeException).hasMessage("There is no skeleton and instance available for Group -> name:" + groupName);
    }

    @Test
    public void testConvertWhenInstanceGroupContainsTemplateAndNodeCountIsZeroThenSkeletonShouldBeReturnOnReferenceInstanceConfiguration() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        instanceGroups.add(instanceGroup);
        when(instanceGroup.getNodeCount()).thenReturn(0);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertNotNull(result.getGroups().get(0).getReferenceInstanceConfiguration());
    }

    @Test
    public void testConvertWhenInstanceGroupAttributesIsNullThenEmptyMapShouldBeStoredInGroupInstance() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getAttributes()).thenReturn(null);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().get(0).getParameters().isEmpty());
    }

    @Test
    public void testConvertWhenInstanceGroupAttributesIsNotNullThenExpectedMapShouldBeStoredInGroupInstance() {
        Json attributes = mock(Json.class);
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        Map<String, Object> expected = createMap("", Object.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(expected);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().get(0).getParameters());
    }

    @Test
    public void testConvertWhenRootVolumeSizeIsNullThenDefaultRootVolumeSizeProviderShouldGiveIt() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        String platform = "platform";
        int expected = Integer.MAX_VALUE;
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));
        when(template.getRootVolumeSize()).thenReturn(null);
        when(template.cloudPlatform()).thenReturn(platform);
        when(defaultRootVolumeSizeProvider.getForPlatform(platform)).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().get(0).getRootVolumeSize());
    }

    @Test
    public void testConvertWhenRootVolumeSizeNotNullThenItsValueShouldBeStoredInGroup() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        int expected = Integer.MAX_VALUE;
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));
        when(template.getRootVolumeSize()).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(expected, result.getGroups().get(0).getRootVolumeSize());
        verify(defaultRootVolumeSizeProvider, times(0)).getForPlatform(any(String.class));
    }

    @Test
    public void testConvertWhenSecurityGroupIsNullThenSecurityShouldContainsEmptyRulesInGroup() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(instanceGroup.getSecurityGroup()).thenReturn(null);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().get(0).getSecurity().getRules().isEmpty());
        assertNull(result.getGroups().get(0).getSecurity().getCloudSecurityId());
    }

    @Test
    public void testConvertWhenSecurityGroupIsNotNullButSecurityRuleRepositoryCantFindAnyRulesThenSecurityShouldContainsEmptyRulesInGroup() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityGroupIds(Collections.singleton(TEST_STRING_ID));
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));
        when(securityRuleService.findAllBySecurityGroupId(securityGroup.getId())).thenReturn(Collections.emptyList());

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertTrue(result.getGroups().get(0).getSecurity().getRules().isEmpty());
        assertEquals(securityGroup.getFirstSecurityGroupId(), result.getGroups().get(0).getSecurity().getCloudSecurityId());
    }

    @Test
    public void testConvertWhenSecurityGroupIsNotNullAndSecurityRuleRepositoryCanFindRulesButThereIsNoPortDefinitionThenEmptyPortDefinitionShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        when(securityRule.getPorts()).thenReturn(EMPTY_STRING);
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getSecurity().getRules().size());
        assertEquals(0L, result.getGroups().get(0).getSecurity().getRules().get(0).getPorts().length);
    }

    @Test
    public void testConvertWhenThereArePortDefinitionsInSecurityRulesAndSegmentsLengthIsGreaterThanOneThenExpectedPartsShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        String[] ports = new String[1];
        ports[0] = "1234-5678";
        when(securityRule.getPorts()).thenReturn(ports);
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getSecurity().getRules().size());
        assertEquals(1L, result.getGroups().get(0).getSecurity().getRules().get(0).getPorts().length);
        assertEquals(ports[0].split("-")[0], result.getGroups().get(0).getSecurity().getRules().get(0).getPorts()[0].getFrom());
        assertEquals(ports[0].split("-")[1], result.getGroups().get(0).getSecurity().getRules().get(0).getPorts()[0].getTo());
    }

    @Test
    public void testConvertWhenThereArePortDefinitionsInSecurityRulesButSegmentsLengthIsOneThanOneThenExpectedPartsShouldBeStored() {
        Set<SecurityRule> securityRules = new HashSet<>(1);
        SecurityRule securityRule = mock(SecurityRule.class);
        securityRules.add(securityRule);
        String[] ports = new String[1];
        ports[0] = "1234";
        when(securityRule.getPorts()).thenReturn(ports);
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        instanceGroups.add(instanceGroup);
        Template template = mock(Template.class);
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setId(1L);
        securityGroup.setSecurityRules(securityRules);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup.getSecurityGroup()).thenReturn(securityGroup);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getSecurity().getRules().size());
        assertEquals(1L, result.getGroups().get(0).getSecurity().getRules().get(0).getPorts().length);
        assertEquals(ports[0], result.getGroups().get(0).getSecurity().getRules().get(0).getPorts()[0].getFrom());
        assertEquals(ports[0], result.getGroups().get(0).getSecurity().getRules().get(0).getPorts()[0].getTo());
    }

    @Test
    public void testConvertWhenStackNetworkIsNullThenNullNetworkShouldBeStored() {
        when(stack.getNetwork()).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertNull(result.getNetwork());
    }

    @Test
    public void testConvertWhenStackNetworkNotNullButStackNetworkAttributesAreNullThenEmptyMapShouldBeSavedInNetworkParams() {
        String subnetCIDR = "testSubnetCIDR";
        when(stack.getNetwork()).thenReturn(stackNetwork);
        when(stackNetwork.getSubnetCIDR()).thenReturn(subnetCIDR);
        when(stackNetwork.getAttributes()).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertNotNull(result.getNetwork());
        assertEquals(subnetCIDR, result.getNetwork().getSubnet().getCidr());
        assertTrue(result.getNetwork().getParameters().isEmpty());
    }

    @Test
    public void testConvertWhenStackNetworkNotNullAndStackNetworkAttributesAreNotNullThenExpectedMapShouldBeSavedInNetworkParams() {
        String subnetCIDR = "testSubnetCIDR";
        Json attributes = mock(Json.class);
        Map<String, Object> params = new LinkedHashMap<>();
        when(stack.getNetwork()).thenReturn(stackNetwork);
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
        when(stack.getId()).thenReturn(stackId);
        when(imageService.getImage(stackId)).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getImage());
    }

    @Test
    public void testConvertWhenImageServiceCantObtainImageAndThrowsCloudbreakImageNotFoundExceptionThenNullImageShouldBeStored()
            throws CloudbreakImageNotFoundException {
        Long stackId = 1L;
        when(stack.getId()).thenReturn(stackId);
        when(imageService.getImage(stackId)).thenThrow(new CloudbreakImageNotFoundException("not found"));

        CloudStack result = underTest.convert(stack);

        assertNull(result.getImage());
    }

    @Test
    public void testConvertWhenStackPassingItsParametersThenThoseShouldBeStored() {
        Map<String, String> expected = Map.of(PlatformParametersConsts.RESOURCE_CRN_PARAMETER, STACK_CRN);
        when(stack.getParameters()).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getParameters());
    }

    @Test
    public void testConvertWhenStackTagsParameterIsNullThenEmptyMapShouldByStored() {
        when(stack.getTags()).thenReturn(null);

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

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenStackTagsNotNullButBothUserDefinedAndDefaultTagsAreNullThenEmptyMapsShouldBeStored() throws IOException {
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(null);
        when(stackTags.getDefaultTags()).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenTagsContainsOnlyUserDefinedTagsThenOnlyThoseWillBeStored() throws IOException {
        Map<String, String> userDefinedTags = createMap("userDefined", String.class);
        Json tags = mock(Json.class);
        StackTags stackTags = mock(StackTags.class);
        when(stack.getTags()).thenReturn(tags);
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(userDefinedTags);
        when(stackTags.getDefaultTags()).thenReturn(null);

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
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(null);
        when(stackTags.getDefaultTags()).thenReturn(defaultTags);

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
        when(tags.get(StackTags.class)).thenReturn(stackTags);
        when(stackTags.getUserDefinedTags()).thenReturn(userDefined);
        when(stackTags.getDefaultTags()).thenReturn(defaultTags);

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
        when(tags.get(StackTags.class)).thenThrow(new IOException("failed to parse json to StackTags"));

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertWhenComponentConfigProviderGivesNullStackTemplateThenNullTemplateShouldBeSaved() {
        when(stack.getId()).thenReturn(TEST_STACK_ID);
        when(componentConfigProviderService.getStackTemplate(TEST_STACK_ID)).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertNull(result.getTemplate());
    }

    @Test
    public void testConvertWhenComponentConfigProviderGivesValidStackTemplateThenItsTemplateShouldBeSaved() {
        StackTemplate stackTemplate = mock(StackTemplate.class);
        String expected = "template";
        when(stackTemplate.getTemplate()).thenReturn(expected);
        when(stack.getId()).thenReturn(TEST_STACK_ID);
        when(componentConfigProviderService.getStackTemplate(TEST_STACK_ID)).thenReturn(stackTemplate);

        CloudStack result = underTest.convert(stack);

        assertEquals(expected, result.getTemplate());
    }

    @Test
    public void testConvertWhenPassingStackAuthenticationThenItsPassedValueShouldBeStoredInInstanceAuthentication() {
        StackAuthentication stackAuthentication = createStackAuthentication();
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);

        CloudStack result = underTest.convert(stack);

        assertEquals(stackAuthentication.getLoginUserName(), result.getInstanceAuthentication().getLoginUserName());
        assertEquals(stackAuthentication.getPublicKeyId(), result.getInstanceAuthentication().getPublicKeyId());
        assertEquals(stackAuthentication.getPublicKey(), result.getInstanceAuthentication().getPublicKey());
    }

    @Test
    public void testConvertWhenProvidingAuthenticationLoginUserNameAndPublicKeyThenTheseValuesShouldBePassed() {
        StackAuthentication authentication = createStackAuthentication();
        when(stack.getStackAuthentication()).thenReturn(authentication);

        CloudStack result = underTest.convert(stack);

        assertEquals(authentication.getLoginUserName(), result.getLoginUserName());
        assertEquals(authentication.getPublicKey(), result.getPublicKey());
    }

    @Test
    public void testBuildCloudInstanceParametersAWSComplete() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setId(1L);
        metaData.setDiscoveryFQDN(DISCOVERY_FQDN);
        metaData.setSubnetId(SUBNET_ID);
        metaData.setAvailabilityZone(AVAILABILITY_ZONE);
        metaData.setInstanceName(INSTANCE_NAME);

        Map<String, Object> result = underTest.buildCloudInstanceParameters(environment, metaData, CloudPlatform.AWS);

        assertThat(result).hasSize(4);
        assertThat(result).doesNotContainKey(RESOURCE_GROUP_NAME_PARAMETER);
        assertThat(result).doesNotContainKey(RESOURCE_GROUP_USAGE_PARAMETER);
        assertThat(result.get(CloudInstance.FQDN)).isEqualTo(DISCOVERY_FQDN);
        assertThat(result.get(CloudInstance.DISCOVERY_NAME)).isEqualTo(DISCOVERY_NAME);
        assertThat(result.get(NetworkConstants.SUBNET_ID)).isEqualTo(SUBNET_ID);
        assertThat(result.get(CloudInstance.INSTANCE_NAME)).isEqualTo(INSTANCE_NAME);
    }

    @Test
    public void testBuildCloudInstanceParametersAWSWithEmptyInstanceMetaData() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setId(1L);

        Map<String, Object> result = underTest.buildCloudInstanceParameters(environment, metaData, CloudPlatform.AWS);

        assertThat(result).isEmpty();
    }

    @Test
    public void testBuildCloudInstanceParametersAWSWithNullInstanceMetaData() {
        Map<String, Object> result = underTest.buildCloudInstanceParameters(environment, null, CloudPlatform.AWS);

        assertThat(result).isEmpty();
    }

    @Test
    public void testBuildCloudInstanceParametersAzureSingleResourceGroup() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setId(1L);
        metaData.setDiscoveryFQDN(DISCOVERY_FQDN);
        metaData.setSubnetId(SUBNET_ID);
        metaData.setInstanceName(INSTANCE_NAME);

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AZURE");
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withName(RESOURCE_GROUP)
                        .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                        .build())
                .build());

        Map<String, Object> result = underTest.buildCloudInstanceParameters(environmentResponse, metaData, CloudPlatform.AZURE);

        assertEquals(RESOURCE_GROUP, result.get(RESOURCE_GROUP_NAME_PARAMETER).toString());
        assertEquals(ResourceGroupUsage.SINGLE.name(), result.get(RESOURCE_GROUP_USAGE_PARAMETER).toString());
        assertEquals(6, result.size());
        assertThat(result.get(CloudInstance.FQDN)).isEqualTo(DISCOVERY_FQDN);
        assertThat(result.get(CloudInstance.DISCOVERY_NAME)).isEqualTo(DISCOVERY_NAME);
        assertThat(result.get(NetworkConstants.SUBNET_ID)).isEqualTo(SUBNET_ID);
        assertThat(result.get(CloudInstance.INSTANCE_NAME)).isEqualTo(INSTANCE_NAME);
    }

    @Test
    public void testBuildCloudInstanceParametersAzureMultipleResourceGroup() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setId(1L);
        metaData.setDiscoveryFQDN(DISCOVERY_FQDN);
        metaData.setSubnetId(SUBNET_ID);
        metaData.setInstanceName(INSTANCE_NAME);

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AZURE");
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);

        Map<String, Object> result = underTest.buildCloudInstanceParameters(environment, metaData, CloudPlatform.AZURE);

        assertFalse(result.containsKey(RESOURCE_GROUP_NAME_PARAMETER));
        assertFalse(result.containsKey(RESOURCE_GROUP_USAGE_PARAMETER));

        assertEquals(4, result.size());
        assertThat(result.get(CloudInstance.FQDN)).isEqualTo(DISCOVERY_FQDN);
        assertThat(result.get(CloudInstance.DISCOVERY_NAME)).isEqualTo(DISCOVERY_NAME);
        assertThat(result.get(NetworkConstants.SUBNET_ID)).isEqualTo(SUBNET_ID);
        assertThat(result.get(CloudInstance.INSTANCE_NAME)).isEqualTo(INSTANCE_NAME);
    }

    @Test
    public void testBuildCloudStackParametersAzureSingleResourceGroup() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AZURE");
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withName(RESOURCE_GROUP)
                        .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                        .build())
                .build());
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(environmentClientService.getByCrnAsInternal(anyString())).thenReturn(environmentResponse);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("key", "value");
        when(stack.getParameters()).thenReturn(expected);

        CloudStack result = underTest.convert(stack);
        Map<String, String> parameters = result.getParameters();

        assertEquals(RESOURCE_GROUP, parameters.get(RESOURCE_GROUP_NAME_PARAMETER));
        assertEquals(ResourceGroupUsage.SINGLE.name(), parameters.get(RESOURCE_GROUP_USAGE_PARAMETER));
        assertEquals(4, parameters.size());
    }

    @Test
    public void testBuildCloudStackParametersAzureMultipleResourceGroup() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AZURE");
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withName(RESOURCE_GROUP)
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("key", "value");
        when(stack.getParameters()).thenReturn(expected);

        CloudStack result = underTest.convert(stack);
        Map<String, String> parameters = result.getParameters();

        assertFalse(parameters.containsKey(RESOURCE_GROUP_NAME_PARAMETER));
        assertFalse(parameters.containsKey(RESOURCE_GROUP_USAGE_PARAMETER));
        assertEquals(2, parameters.size());
    }

    private StackAuthentication createStackAuthentication() {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(TEST_USERNAME_VALUE);
        stackAuthentication.setPublicKey(TEST_PUBLICKEY_VALUE);
        stackAuthentication.setPublicKeyId(TEST_STRING_ID);
        return stackAuthentication;
    }

    private <T> Map<String, T> createMap(String keyPrefix, Class<T> clazz) {
        Map<String, T> map = new LinkedHashMap<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            map.put(String.format("%s-key-%s", keyPrefix, i), clazz.cast(String.format("key-%s", i)));
        }
        return map;
    }

    @Test
    public void testBuildInstanceTemplateWithAttributes() {
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        template.setAttributes(new Json(Map.of("someAttr", "value")));
        template.setSecretAttributes(new Json(Map.of("otherAttr", "value")).getValue());

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(
                template, "name", 0L, InstanceStatus.CREATE_REQUESTED, "instanceImageId");

        assertNotNull(instanceTemplate.getParameters());
        assertEquals("value", instanceTemplate.getParameters().get("someAttr"));
        assertEquals("value", instanceTemplate.getParameters().get("otherAttr"));
    }

    @Test
    public void testBuildInstanceTemplateWithGcpEncryptionAttributes() {
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        template.setAttributes(new Json(Map.of("keyEncryptionMethod", "RAW", InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name())));
        template.setSecretAttributes(new Json(Map.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "myKey")).getValue());

        InstanceTemplate instanceTemplate = underTest.buildInstanceTemplate(
                template, "name", 0L, InstanceStatus.CREATE_REQUESTED, "instanceImageId");

        Map<String, Object> parameters = instanceTemplate.getParameters();
        assertNotNull(parameters);
        assertEquals("RAW", parameters.get("keyEncryptionMethod"));
        assertEquals(EncryptionType.CUSTOM.name(), parameters.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE));
        assertEquals("myKey", parameters.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID));
    }

    @Test
    public void testConvertWithKnoxLoadBalancer() {
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup1 = mock(InstanceGroup.class);
        InstanceGroup instanceGroup2 = mock(InstanceGroup.class);
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(instanceGroup2.getGroupName()).thenReturn("group2");
        instanceGroups.add(instanceGroup1);
        instanceGroups.add(instanceGroup2);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        when(instanceGroup1.getTemplate()).thenReturn(template);
        when(instanceGroup1.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup1.getStack()).thenReturn(stack);
        when(instanceGroup2.getTemplate()).thenReturn(template);
        when(instanceGroup2.getNotDeletedInstanceMetaDataSet()).thenReturn(Collections.emptySet());
        when(instanceGroup2.getStack()).thenReturn(stack);
        TargetGroup targetGroup = mock(TargetGroup.class);
        when(targetGroup.getType()).thenReturn(TargetGroupType.KNOX);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        when(loadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(loadBalancer.getId()).thenReturn(1L);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(targetGroupPersistenceService.findByLoadBalancerId(anyLong())).thenReturn(Set.of(targetGroup));
        when(instanceGroupService.findByTargetGroupId(anyLong())).thenReturn(Set.of(instanceGroup1, instanceGroup2));
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 8443);
        when(loadBalancerConfigService.getTargetGroupPortPairs(any(TargetGroup.class))).thenReturn(Set.of(targetGroupPortPair));

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
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        InstanceGroup instanceGroup1 = mock(InstanceGroup.class);
        InstanceGroup instanceGroup2 = mock(InstanceGroup.class);
        when(instanceGroup1.getGroupName()).thenReturn("group1");
        when(instanceGroup2.getGroupName()).thenReturn("group2");
        instanceGroups.add(instanceGroup1);
        instanceGroups.add(instanceGroup2);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));
        Template template = new Template();
        template.setVolumeTemplates(Set.of());
        when(instanceGroup1.getTemplate()).thenReturn(template);
        when(instanceGroup1.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of());
        when(instanceGroup1.getStack()).thenReturn(stack);
        when(instanceGroup2.getTemplate()).thenReturn(template);
        when(instanceGroup2.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of());
        when(instanceGroup2.getStack()).thenReturn(stack);
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
        when(instanceGroupService.findByTargetGroupId(anyLong())).thenReturn(Set.of(instanceGroup1, instanceGroup2));
        when(loadBalancerConfigService.getTargetGroupPortPairs(any(TargetGroup.class))).thenReturn(Set.of(new TargetGroupPortPair(443, 8443)));

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
    public void testConvertWithTerminatedInstances() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Set<InstanceGroup> instanceGroups = Set.of(instanceGroup);

        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());

        InstanceMetaData metaData = new InstanceMetaData();
        String fqdnParsedName = "test1-m-1-20180605095019";
        metaData.setId(1L);
        metaData.setDiscoveryFQDN(String.format("%s.project.id", fqdnParsedName));
        metaData.setSubnetId(TEST_STRING_ID);
        metaData.setInstanceName(TEST_NAME);
        Set<InstanceMetaData> notDeletedMetas = Set.of(metaData);
        InstanceMetaData terminatedMetaData = new InstanceMetaData();
        String terminatedFqdnParsedName = "test1-m-1-20200401095019";
        terminatedMetaData.setId(2L);
        terminatedMetaData.setDiscoveryFQDN(String.format("%s.project.id", terminatedFqdnParsedName));
        terminatedMetaData.setSubnetId(TEST_STRING_ID);
        terminatedMetaData.setInstanceName("terminated-" + TEST_NAME);
        Set<InstanceMetaData> deletedMetas = Set.of(terminatedMetaData);

        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(notDeletedMetas);
        when(instanceGroup.getDeletedInstanceMetaDataSet()).thenReturn(deletedMetas);
        when(instanceGroup.getStack()).thenReturn(stack);
        when(stack.getInstanceGroupsAsList()).thenReturn(new ArrayList<>(instanceGroups));

        CloudStack result = underTest.convert(stack);

        assertEquals(1L, result.getGroups().size());
        assertEquals(1L, result.getGroups().get(0).getInstances().size());
        assertEquals(1L, result.getGroups().get(0).getDeletedInstances().size());
        assertEquals(fqdnParsedName,
                result.getGroups().get(0).getInstances().get(0).getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(metaData.getSubnetId(),
                result.getGroups().get(0).getInstances().get(0).getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(metaData.getInstanceName(),
                result.getGroups().get(0).getInstances().get(0).getParameters().get(CloudInstance.INSTANCE_NAME));
        assertEquals(terminatedFqdnParsedName,
                result.getGroups().get(0).getDeletedInstances().get(0).getParameters().get(CloudInstance.DISCOVERY_NAME));
        assertEquals(terminatedMetaData.getSubnetId(),
                result.getGroups().get(0).getDeletedInstances().get(0).getParameters().get(NetworkConstants.SUBNET_ID));
        assertEquals(terminatedMetaData.getInstanceName(),
                result.getGroups().get(0).getDeletedInstances().get(0).getParameters().get(CloudInstance.INSTANCE_NAME));
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
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-1234");
        instanceMetaData.setDiscoveryFQDN("vm.empire.com");
        instanceMetaData.setInstanceName("worker3");
        instanceMetaData.setSubnetId(subnetId);
        instanceMetaData.setAvailabilityZone(availabilityZone);

        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setVolumeTemplates(Set.of());
        instanceGroup.setTemplate(template);
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        instanceGroup.setStack(stack);
        instanceGroup.setGroupName("worker");

        when(instanceMetadataToImageIdConverter.convert(instanceMetaData)).thenReturn("image-12");

        CloudInstance cloudInstance = underTest.buildInstance(instanceMetaData, instanceGroup, new StackAuthentication(), 12L, InstanceStatus.CREATED,
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
}