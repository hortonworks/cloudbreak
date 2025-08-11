package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStorageView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.util.Version;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.model.CloudIdentityType;

import freemarker.template.Configuration;

/**
 * Validates that ARM template can be created from Free Marker Template {@code .ftl} files.
 * <p>
 * This checks that:
 * <ul>
 *     <li>{@code .ftl} files are properly formatted</li>
 *     <li>Valid Json is produced</li>
 *     <li>Certain values are contained in the Json</li>
 * </ul>
 * <p>
 * This class is parameterized, so a single test will be used to run against multiple template files.
 * The template files are in {@code main/resources} and {@code test/resources}.
 * <p>
 * To opt out of running a test for <em>older</em> versions of the template, place the {@code assumeTrue()} method at
 * the beginning of your test.
 * <pre>{@code
 *     assumeTrue(isTemplateVersionGreaterOrEqualThan("2.7.3.0"));
 * }</pre>
 * <p>
 * It's useful to capture the JSON strings printed to the console, then provide them to the Azure CLI's template deployment
 * validation commands.
 * Example:
 * <pre>{@code
 * az deployment group validate --resource-group bderriso-rg --template-file arm-template-from-test
 * }</pre>
 */
@ExtendWith(MockitoExtension.class)
public class AzureTemplateBuilderPart1Test {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilderPart1Test.class);

    @InjectMocks
    private final AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private final Gson gson = new Gson();

    private final Map<String, String> tags = new HashMap<>();

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AzurePlatformResources platformResources;

    private String stackName;

    private AzureCredentialView azureCredentialView;

    private List<Group> groups;

    private String name;

    private InstanceAuthentication instanceAuthentication;

    private CloudInstance instance;

    private Security security;

    private Image image;

    private CloudContext cloudContext;

    private CloudStack cloudStack;

    private AzureStorageView azureStorageView;

    private AzureSubnetStrategy azureSubnetStrategy;

    private AzureStackView azureStackView;

    private AzureMarketplaceImage azureMarketplaceImage;

    public static Iterable<?> templatesPathDataProvider() {
        List<String> templates = Lists.newArrayList(LATEST_TEMPLATE_PATH);
        File[] templateFiles = new File(AzureTemplateBuilderPart1Test.class.getClassLoader().getResource("templates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).filter(File::isFile).map(file -> {
            String[] path = file.getPath().split("/");
            return "templates/" + path[path.length - 1];
        }).collect(Collectors.toList());
        templates.addAll(olderTemplates);
        return templates;
    }

    @BeforeEach
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters.ftl");
        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        groups = new ArrayList<>();
        stackName = "testStack";
        name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        Map<String, Object> params = new HashMap<>();
        params.put(NetworkConstants.SUBNET_ID, "existingSubnet");
        params.put(CloudInstance.ID, 1L);
        instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication, "existingSubnet", null, params);
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(), "2019-10-24",
                1571884856L, null);
        cloudContext = CloudContext.Builder.builder()
                .withId(7899L)
                .withName("thisisaverylongazureresourcenamewhichneedstobeshortened")
                .withCrn("crn")
                .withPlatform("dummy1")
                .withLocation(Location.location(Region.region("westus2"), new AvailabilityZone("availabilityZone")))
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        azureCredentialView = new AzureCredentialView(cloudCredential());
        azureStorageView = new AzureStorageView(azureCredentialView, cloudContext, azureStorage, null);
        azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version", false);

        azureSubnetStrategy = AzureSubnetStrategy.getAzureSubnetStrategy(FILL, Collections.singletonList("existingSubnet"),
                ImmutableMap.of("existingSubnet", 100L));
        when(customVMImageNameProvider.getImageNameFromConnectionString(anyString())).thenCallRealMethod();
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("custom-resource-group-name");
        when(azureUtils.getCustomNetworkId(any())).thenReturn("custom-vnet-id");
        lenient().when(azureAcceleratedNetworkValidator.validate(any(), anySet())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(platformResources.virtualMachinesNonExtended(azureCredentialView.getCloudCredential(), cloudContext.getLocation().getRegion(), null))
                .thenReturn(new CloudVmTypes());
    }

    @ParameterizedTest(name = "testNicDependenciesWhenNoSecurityGroupsNoFirewallRulesAndNoPublicIp {displayName}_{0}")
    @MethodSource("templatesPathDataProvider")
    public void testNicDependenciesWhenNoSecurityGroupsNoFirewallRulesAndNoPublicIp(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");

        groups.add(getGroup("gateway-group", InstanceGroupType.GATEWAY));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer publicLb = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        publicLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(publicLb);
        CloudLoadBalancer privateLb = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        privateLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(privateLb);

        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("custom-resource-group-name");
        when(azureUtils.getCustomNetworkId(any())).thenReturn("custom-vnet-id");
        when(azureUtils.isExistingNetwork(any())).thenReturn(false);
        when(azureUtils.isPrivateIp(any())).thenReturn(true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGatewayAndCore {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGatewayAndCore(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestResourceGroupName {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestResourceGroupName(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("resourceGroupName"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestExistingVNETName {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestExistingVNETName(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("existingVNETName"));
        assertTrue(templateString.contains("existingSubnet"));
        assertTrue(templateString.contains("existingResourceGroup"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestExistingSubnetNameNotInTemplate {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestExistingSubnetNameNotInTemplate(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("existingSubnetName"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestVirtualNetworkNamePrefix {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestVirtualNetworkNamePrefix(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("virtualNetworkNamePrefix"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestSubnet1Prefix {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestSubnet1Prefix(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("subnet1Prefix"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestDisksFor1xVersions {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksFor1xVersions(String templatePath) {
        assumeFalse(isTemplateVersionGreaterOrEqualThan1165(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureAcceleratedNetworkValidator.validate(any(), any())).thenReturn(Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("[concat('datadisk', 'm0-c4ca4238', '0')]");
        assertThat(templateString).contains("[concat('datadisk', 'm0-c4ca4238', '1')]");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestDisksOnAllVersionsAndVerifyOsDisks {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksOnAllVersionsAndVerifyOsDisks(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("[concat(parameters('vmNamePrefix'),'-osDisk', 'm0-c4ca4238')]"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestDisksWhenTheVersion210OrGreater {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksWhenTheVersion210OrGreater(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.GATEWAY));
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("[concat(parameters('vmNamePrefix'),'-osDisk', 'm0-c4ca4238')]"));
        assertFalse(templateString.contains("[concat('datadisk', 'm0', '1')]"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestAvailabilitySetInTemplate(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        Group gatewayGroup = getGroup("gateway", InstanceGroupType.GATEWAY);
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);
        Group coreGroup = getGroup("core", InstanceGroupType.CORE);
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"gatewayAsName\": \"gateway-as\","));
        assertFalse(templateString.contains("coreAsName"));
        assertTrue(templateString.contains("'Microsoft.Compute/availabilitySets', 'gateway-as'"));
        assertFalse(templateString.contains("'Microsoft.Compute/availabilitySets', 'core-as'"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithVmVersion {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithVmVersion(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("                   \"apiVersion\": \"2023-07-01\",\n" +
                "                   \"type\": \"Microsoft.Compute/virtualMachines\",\n");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithNoManagedIdentity {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithNoManagedIdentity(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("\"identity\": {");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithManagedIdentityGiven {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithManagedIdentityGiven(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
        cloudAdlsGen2View.setManagedIdentity("myIdentity");
        groups.add(Group.builder()
                .withName(name)
                .withType(InstanceGroupType.CORE)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .withIdentity(Optional.of(cloudAdlsGen2View))
                .build());
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("\"identity\": {");
        assertThat(templateString).contains("\"type\": \"userAssigned\",");
        assertThat(templateString).contains("                        \"userAssignedIdentities\": {\n" +
                "                            \"myIdentity\": {\n" +
                "                            }\n" +
                "                        }\n");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceRegularImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceRegularImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version", false);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                    "publisher": "cloudera",
                                                    "offer": "my-offer",
                                                    "sku": "my-plan",
                                                    "version": "my-version"
                                           }
                """);
        assertThat(templateString).contains("""
                                   "plan": {
                                        "name": "my-plan",
                                        "product": "my-offer",
                                        "publisher": "cloudera"
                                   },
                """);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceNullImage {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithMarketplaceNullImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = null;
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("\"plan\": {");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceCentosSourceImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceCentosSourceImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("centos", "centos-byos", "centos-lvm88", "8.8.2023053015", true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("""
                                           "imageReference": {
                                                    "publisher": "redhat",
                """);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                   "id": "cloudbreak-image.vhd"
                                           }
                """);
        assertThat(templateString).doesNotContain("""
                                   "plan": {
                """);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceCentosSourceImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceRhelSourceImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("redhat", "rhel-byos", "rhel-lvm88", "8.8.2023053015", true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("""
                                           "imageReference": {
                                                    "publisher": "redhat",
                """);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                   "id": "cloudbreak-image.vhd"
                                           }
                """);
        assertThat(templateString).contains("""
                                   "plan": {
                                        "name": "rhel-lvm88",
                                        "product": "rhel-byos",
                                        "publisher": "redhat"
                                   },
                """);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceClouderaSourceImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceClouderaCdpSourceImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "cdp-7_2_17", "runtime-7_2_17", "700.56405233.1725954227", true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("""
                                           "imageReference": {
                                                    "publisher": "cloudera",
                """);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                   "id": "cloudbreak-image.vhd"
                                           }
                """);
        assertThat(templateString).contains("""
                                   "plan": {
                                        "name": "runtime-7_2_17",
                                        "product": "cdp-7_2_17",
                                        "publisher": "cloudera"
                                   },
                """);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceClouderaFreeipaSourceImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceClouderaFreeipaSourceImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "freeipa-1_0", "runtime-7_2_17", "700.56405233.1725954227", true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("""
                                           "imageReference": {
                                                    "publisher": "cloudera",
                """);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                   "id": "cloudbreak-image.vhd"
                                           }
                """);
        assertThat(templateString).contains("""
                                   "plan": {
                                        "name": "runtime-7_2_17",
                                        "product": "freeipa-1_0",
                                        "publisher": "cloudera"
                                   },
                """);
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithMarketplaceClouderaUnknownSourceImage {0}")
    @MethodSource("templatesPathDataProvider")
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithMarketplaceClouderaUnknownSourceImage(String templatePath) {
        setupMarketplaceTests(templatePath);
        azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "unknown-1_0", "runtime-7_2_17", "700.56405233.1725954227", true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("""
                                           "imageReference": {
                                                    "publisher": "cloudera",
                """);
        assertThat(templateString).contains("""
                                           "imageReference": {
                                                   "id": "cloudbreak-image.vhd"
                                           }
                """);
        assertThat(templateString).doesNotContain("""
                                   "plan": {
                                        "name": "runtime-7_2_17",
                                        "product": "unknown-1_0",
                                        "publisher": "cloudera"
                                   },
                """);
        AzureTestUtils.validateJson(templateString);
    }

    private void setupMarketplaceTests(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
    }

    @ParameterizedTest(name = "buildTestWithNoDiskEncryptionSetId {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithNoDiskEncryptionSetId(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("\"diskEncryptionSet\": {");
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildTestWithDiskEncryptionSetIdGiven {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithDiskEncryptionSetIdGiven(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        instance.getTemplate().putParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, true);
        instance.getTemplate().putParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "myDES");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("\"diskEncryptionSet\": {");
        assertThat(templateString).contains("\"id\": \"myDES\"");
        AzureTestUtils.validateJson(templateString);
    }

    private Group getGroup(String name, InstanceGroupType type) {
        return Group.builder()
                .withName(name)
                .withType(type)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .build();
    }

    private boolean isTemplateVersionGreaterOrEqualThan2100(String templatePath) {
        return isTemplateVersionGreaterOrEqualThan(templatePath, "2.10.0.0");
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential("crn", "test", parameters, "acc");
    }

    private boolean isTemplateVersionGreaterOrEqualThan1165(String templatePath) {
        return isTemplateVersionGreaterOrEqualThan(templatePath, "1.16.5.0");
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }

    private boolean isTemplateVersionGreaterOrEqualThan(String templatePath, String version) {
        if (LATEST_TEMPLATE_PATH.equals(templatePath)) {
            return true;
        }
        String[] splitName = templatePath.split("-");
        String templateVersion = splitName[splitName.length - 1].replaceAll("\\.ftl", "");
        return Version.versionCompare(templateVersion, version) > -1;
    }
}