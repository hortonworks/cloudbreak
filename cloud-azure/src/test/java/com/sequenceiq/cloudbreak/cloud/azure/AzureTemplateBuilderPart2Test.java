package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
import com.google.common.collect.Maps;
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
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.util.Version;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

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
public class AzureTemplateBuilderPart2Test {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final String ZONE_REDUNDANT = "\"zones\":[\"1\",\"2\",\"3\"]";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final boolean LOADBALANCER_TARGET_STICKY_SESSION = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilderPart2Test.class);

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
        File[] templateFiles = new File(AzureTemplateBuilderPart2Test.class.getClassLoader().getResource("templates").getPath()).listFiles();
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

    @ParameterizedTest(name = "buildNoPublicIpFirewallWithTags {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpFirewallWithTags(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup(name, InstanceGroupType.CORE));
        Map<String, String> userDefinedTags = Maps.newHashMap();
        userDefinedTags.put("testtagkey1", "testtagvalue1");
        userDefinedTags.put("testtagkey2", "testtagvalue2");
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(userDefinedTags)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("\"testtagkey1\": \"testtagvalue1\""));
        assertTrue(templateString.contains("\"testtagkey2\": \"testtagvalue2\""));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildNoPublicIpNoFirewallButExistingNetwork {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpNoFirewallButExistingNetwork(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan1165(templatePath));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
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
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("existingNetworkName"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildNoPublicIpButFirewall {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpButFirewall(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
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
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("networkSecurityGroups"));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithPublicIpAndFirewall {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithPublicIpAndFirewall(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> false);
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
        assertTrue(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("networkSecurityGroups"));
        AzureTestUtils.validateJson(templateString);
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeCore {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeCore(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
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
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
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
        assertFalse(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGateway {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGateway(String templatePath) {
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
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup("gateway-group", InstanceGroupType.GATEWAY));
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .loadBalancers(loadBalancers)
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
                        AzureInstanceTemplateOperation.PROVISION, null);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndStandardLoadBalancerAndExistingNetworkAndUpscaleWithNoInstances {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndStandardLoadBalancerAndExistingNetworkAndUpscaleWithNoInstances(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(Group.builder()
                .withName("gateway-group")
                .withType(InstanceGroupType.GATEWAY)
                .withSecurity(security)
                .build());
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.STANDARD, LOADBALANCER_TARGET_STICKY_SESSION);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .loadBalancers(loadBalancers)
                .template(azureTemplateBuilder.getTemplateString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"Standard\""));
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains(ZONE_REDUNDANT));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndStandardLoadBalancerAndNonExistingNetworkAndUpscaleWithNoInstances {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndMultipleLoadBalancerAndNonExistingNetworkAndUpscaleWithNoInstances(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(Group.builder()
                .withName("gateway-group")
                .withType(InstanceGroupType.GATEWAY)
                .withSecurity(security)
                .build());
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
                .loadBalancers(loadBalancers)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        when(azureUtils.isExistingNetwork(any())).thenReturn(false);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"Standard\""));
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains(ZONE_REDUNDANT));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndStandardLoadBalancerAndNonExistingNetworkAndUpscaleWithNoInstances {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeWithoutLoadBalancerAndNonExistingNetworkAndUpscaleWithNoInstances(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(Group.builder()
                .withName("gateway-group")
                .withType(InstanceGroupType.GATEWAY)
                .withSecurity(security)
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
        when(azureUtils.isExistingNetwork(any())).thenReturn(false);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        gson.fromJson(templateString, Map.class);
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains(ZONE_REDUNDANT));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndStandardLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndStandardLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(getGroup("gateway-group", InstanceGroupType.GATEWAY));
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.STANDARD, LOADBALANCER_TARGET_STICKY_SESSION);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .loadBalancers(loadBalancers)
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
                        AzureInstanceTemplateOperation.PROVISION, null);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"Standard\""));
        assertTrue(templateString.contains("\"tier\": \"Regional\""));
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains(ZONE_REDUNDANT));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithStandardLoadBalancerOnlyTargetGroupsUpdated {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithStandardLoadBalancerOnlyTargetGroupsUpdated(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        String lbGroupExpectedBlob =
                "\"tags\":{},\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"},\"properties\":{\"publicIPAllocationM" +
                        "ethod\":\"Static\"}},{\"apiVersion\":\"2023-06-01\",\"type\":\"Microsoft.Network/networkInterfa" +
                        "ces\",\"name\":\"[concat(parameters('nicNamePrefix'),'m0-c4ca4238')]\",\"location\":\"[parameters('regio" +
                        "n')]\",\"tags\":{},\"dependsOn\":[\"[concat('Microsoft.Network/networkSecurityGroups/',variable" +
                        "s('gateway-groupsecGroupName'))]\"";
        String nonLbGroupExpectedBlob =
                "\"tags\":{},\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"},\"properties\":{\"publicIPAllocationMethod\":\"Static\"}}," +
                        "{\"apiVersion\":\"2023-06-01\",\"type\":\"Microsoft.Network/networkInterfaces\",\"name\":\"[concat(parameters('nicNamePrefix'),'" +
                        "m0-c4ca4238')]\",\"location\":\"[parameters('region')]\",\"tags\":{},\"dependsOn\":[\"[concat('Microsoft" +
                        ".Network/networkSecurityGroups/',variables('core-groupsecGroupName'))]\"";
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        Map<String, Object> lbParams = new HashMap<>();
        Map<String, Object> lbAsName = new HashMap<>();
        lbAsName.put("name", "gateway-group-as");
        lbParams.put("availabilitySet", lbAsName);
        Group lbGroup = getGroup("gateway-group", InstanceGroupType.GATEWAY);
        Group nonLbGroup = getGroup("core-group", InstanceGroupType.CORE);
        groups.add(lbGroup);
        groups.add(nonLbGroup);
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.STANDARD, LOADBALANCER_TARGET_STICKY_SESSION);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), Set.of(lbGroup));
        loadBalancers.add(loadBalancer);
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .loadBalancers(loadBalancers)
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
                        AzureInstanceTemplateOperation.PROVISION, null);
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertTrue(strippedTemplateString.contains(lbGroupExpectedBlob));
        assertTrue(strippedTemplateString.contains(nonLbGroupExpectedBlob));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndMultipleLoadBalancers {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndMultipleLoadBalancers(String templatePath) {
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
                .loadBalancers(loadBalancers)
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
                        AzureInstanceTemplateOperation.PROVISION, null);
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertEquals(2, StringUtils.countMatches(templateString, "\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"" + LoadBalancerSku.getDefault().getTemplateName() + "\""));
        assertEquals(2, StringUtils.countMatches(templateString,
                "\"[resourceId('Microsoft.Network/loadBalancers'"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', 'LoadBalancertestStackPUBLIC', 'gateway-group-pool')]"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', 'LoadBalancertestStackPRIVATE', 'gateway-group-pool')]"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "\"type\": \"Microsoft.Network/loadBalancers\","));
        assertEquals(1, StringUtils.countMatches(templateString,
                "\"id\": \"[resourceId('Microsoft.Network/publicIPAddresses', 'LoadBalancertestStackPUBLIC-publicIp')]\""));
        assertFalse(StringUtils.contains(templateString, "\"name\": \"group-gateway-group-outbound-rule\","));
        AzureTestUtils.validateJson(templateString);
    }

    @ParameterizedTest(name = "testNicDependenciesAreValidJson {0}")
    @MethodSource("templatesPathDataProvider")
    public void testNicDependenciesAreValidJson(String templatePath) {
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
        // This test is checking that valid json is created when using an existing network and no public IP.
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.isPrivateIp(any())).thenReturn(true);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        AzureTestUtils.validateJson(templateString);
    }

    private boolean isTemplateVersionGreaterOrEqualThan2100(String templatePath) {
        return isTemplateVersionGreaterOrEqualThan(templatePath, "2.10.0.0");
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential("crn", "test", parameters, "acc");
    }

    private Group getGroup(String name, InstanceGroupType type) {
        return Group.builder()
                .withName(name)
                .withType(type)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .build();
    }

    private boolean isTemplateVersionGreaterOrEqualThan1165(String templatePath) {
        return isTemplateVersionGreaterOrEqualThan(templatePath, "1.16.5.0");
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
