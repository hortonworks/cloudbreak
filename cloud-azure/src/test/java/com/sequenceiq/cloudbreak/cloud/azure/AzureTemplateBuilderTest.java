package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
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
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.util.Version;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.model.CloudIdentityType;

import freemarker.template.Configuration;

/**
 * Validates that ARM template can be created from Free Marker Template {@code .ftl} files.
 *
 * This checks that:
 * <ul>
 *     <li>{@code .ftl} files are properly formatted</li>
 *     <li>Valid Json is produced</li>
 *     <li>Certain values are contained in the Json</li>
 * </ul>
 *
 * This class is parameterized, so a single test will be used to run against multiple template files.
 * The template files are in {@code main/resources} and {@code test/resources}.
 *
 * To opt out of running a test for <em>older</em> versions of the template, place the {@code assumeTrue()} method at
 * the beginning of your test.
 * <pre>{@code
 *     assumeTrue(isTemplateVersionGreaterOrEqualThan("2.7.3.0"));
 * }</pre>
 *
 * It's useful to capture the JSON strings printed to the console, then provide them to the Azure CLI's template deployment
 * validation commands.
 * Example:
 * <pre>{@code
 * az deployment group validate --resource-group bderriso-rg --template-file arm-template-from-test
 * }</pre>
 */
@ExtendWith(MockitoExtension.class)
public class AzureTemplateBuilderTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilderTest.class);

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

    @InjectMocks
    private final AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private String stackName;

    private AzureCredentialView azureCredentialView;

    private List<Group> groups;

    private String name;

    private CloudInstance instance;

    private Security security;

    private Image image;

    private CloudContext cloudContext;

    private CloudStack cloudStack;

    private AzureStorageView azureStorageView;

    private AzureSubnetStrategy azureSubnetStrategy;

    private AzureStackView azureStackView;

    private AzureMarketplaceImage azureMarketplaceImage;

    private final Gson gson = new Gson();

    private final Map<String, String> tags = new HashMap<>();

    public static Iterable<?> templatesPathDataProvider() {
        List<String> templates = Lists.newArrayList(LATEST_TEMPLATE_PATH);
        File[] templateFiles = new File(AzureTemplateBuilderTest.class.getClassLoader().getResource("templates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).map(file -> {
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
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication, "existingSubnet", "az1", params);
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
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

        azureSubnetStrategy = AzureSubnetStrategy.getAzureSubnetStrategy(FILL, Collections.singletonList("existingSubnet"),
                ImmutableMap.of("existingSubnet", 100L));
        when(customVMImageNameProvider.getImageNameFromConnectionString(anyString())).thenCallRealMethod();
    }

    @ParameterizedTest(name = "buildNoPublicIpFirewallWithTags {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpFirewallWithTags(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        Map<String, String> userDefinedTags = Maps.newHashMap();
        userDefinedTags.put("testtagkey1", "testtagvalue1");
        userDefinedTags.put("testtagkey2", "testtagvalue2");

        cloudStack = new CloudStack(groups, network, image, parameters, userDefinedTags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("\"testtagkey1\": \"testtagvalue1\""));
        assertTrue(templateString.contains("\"testtagkey2\": \"testtagvalue2\""));
    }

    @ParameterizedTest(name = "buildNoPublicIpNoFirewallButExistingNetwork {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpNoFirewallButExistingNetwork(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan1165(templatePath));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("existingNetworkName"));
    }

    @ParameterizedTest(name = "buildNoPublicIpButFirewall {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildNoPublicIpButFirewall(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("networkSecurityGroups"));
    }

    @ParameterizedTest(name = "buildWithPublicIpAndFirewall {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithPublicIpAndFirewall(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        when(azureUtils.isPrivateIp(any())).then(invocation -> false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("publicIPAddress"));
        assertTrue(templateString.contains("networkSecurityGroups"));
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeCore {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeCore(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGateway {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGateway(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndBasicLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndBasicLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.BASIC);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"Basic\""));
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndStandardLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndStandardLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.STANDARD);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertTrue(templateString.contains("\"name\": \"port-443-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(templateString.contains("\"name\": \"Standard\""));
        assertTrue(templateString.contains("\"tier\": \"Regional\""));
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndOutboundLoadBalancer {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndOutboundLoadBalancer(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer privateLb = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        privateLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(privateLb);
        CloudLoadBalancer outboundLb = new CloudLoadBalancer(LoadBalancerType.OUTBOUND);
        outboundLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(outboundLb);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(templateString.contains("\"frontendPort\": 443,"));
        assertTrue(templateString.contains("\"backendPort\": 443,"));
        assertEquals(1, StringUtils.countMatches(templateString, "\"name\": \"port-443-rule\","));
        assertEquals(1, StringUtils.countMatches(templateString, "\"name\": \"group-gateway-group-outbound-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-8443-probe\","));
        assertTrue(templateString.contains("\"name\": \"" + LoadBalancerSku.getDefault().getTemplateName() + "\""));
        assertEquals(2, StringUtils.countMatches(templateString,
                "\"[resourceId('Microsoft.Network/loadBalancers'"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', 'LoadBalancertestStackOUTBOUND', 'gateway-group-pool')]"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', 'LoadBalancertestStackPRIVATE', 'gateway-group-pool')]"));
        assertEquals(2, StringUtils.countMatches(templateString,
                "\"type\": \"Microsoft.Network/loadBalancers\","));
        assertEquals(1, StringUtils.countMatches(templateString,
                "\"id\": \"[resourceId('Microsoft.Network/publicIPAddresses', 'LoadBalancertestStackOUTBOUND-publicIp')]\""));
    }

    @ParameterizedTest(name = "buildWithStandardLoadBalancerOnlyTargetGroupsUpdated {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithStandardLoadBalancerOnlyTargetGroupsUpdated(String templatePath) {
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        String lbGroupExpectedBlob =
                "\"tags\":{},\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"},\"properties\":{\"publicIPAllocationM" +
                        "ethod\":\"Static\"}},{\"apiVersion\":\"2016-09-01\",\"type\":\"Microsoft.Network/networkInterfa" +
                        "ces\",\"name\":\"[concat(parameters('nicNamePrefix'),'m0')]\",\"location\":\"[parameters('regio" +
                        "n')]\",\"tags\":{},\"dependsOn\":[\"[concat('Microsoft.Network/networkSecurityGroups/',variable" +
                        "s('gateway-groupsecGroupName'))]\"";
        String nonLbGroupExpectedBlob =
                "\"tags\":{},\"properties\":{\"publicIPAllocationMethod\":\"Dynamic\"}},{\"apiVersion\":\"2016-09-01\",\"" +
                        "type\":\"Microsoft.Network/networkInterfaces\",\"name\":\"[concat(parameters('nicNamePrefix'),'" +
                        "m0')]\",\"location\":\"[parameters('region')]\",\"tags\":{},\"dependsOn\":[\"[concat('Microsoft" +
                        ".Network/networkSecurityGroups/',variables('core-groupsecGroupName'))]\"";

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Map<String, Object> lbParams = new HashMap<>();
        Map<String, Object> lbAsName = new HashMap<>();
        lbAsName.put("name", "gateway-group-as");
        lbParams.put("availabilitySet", lbAsName);
        Group lbGroup = new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                lbParams, instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap());
        Group nonLbGroup = new Group("core-group", InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap());
        groups.add(lbGroup);
        groups.add(nonLbGroup);

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC, LoadBalancerSku.STANDARD);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), Set.of(lbGroup));
        loadBalancers.add(loadBalancer);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertTrue(strippedTemplateString.contains(lbGroupExpectedBlob));
        assertTrue(strippedTemplateString.contains(nonLbGroupExpectedBlob));
    }

    @ParameterizedTest(name = "buildWithGatewayInstanceGroupTypeAndMultipleLoadBalancers {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithGatewayInstanceGroupTypeAndMultipleLoadBalancers(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
            instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer publicLb = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        publicLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(publicLb);
        CloudLoadBalancer privateLb = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        privateLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(privateLb);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
            instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
            azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
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
    }

    @ParameterizedTest(name = "testNicDependenciesAreValidJson {0}")
    @MethodSource("templatesPathDataProvider")
    public void testNicDependenciesAreValidJson(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer publicLb = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        publicLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(publicLb);
        CloudLoadBalancer privateLb = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        privateLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(privateLb);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
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
        //THEN
        validateJson(templateString);
    }

    @ParameterizedTest(name = "testNicDependenciesWhenNoSecurityGroupsNoFirewallRulesAndNoPublicIp {displayName}_{0}")
    @MethodSource("templatesPathDataProvider")
    public void testNicDependenciesWhenNoSecurityGroupsNoFirewallRulesAndNoPublicIp(String templatePath) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan(templatePath, "2.7.3.0"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group("gateway-group", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer publicLb = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        publicLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(publicLb);
        CloudLoadBalancer privateLb = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        privateLb.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(privateLb);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("custom-resource-group-name");
        when(azureUtils.getCustomNetworkId(any())).thenReturn("custom-vnet-id");
        when(azureUtils.isExistingNetwork(any())).thenReturn(false);
        when(azureUtils.isPrivateIp(any())).thenReturn(true);

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.UPSCALE, null);
        //THEN
        validateJson(templateString);
    }

    /**
     * Check that the template string is a valid JSON object.
     * We're using the Jackson ObjectMapper because Gson has looser rules on what "valid" JSON is.
     * For instance, a leading comma in an array is valid according to Gson.
     * <pre>{@code [, "valid"]}</pre>
     *
     * @param templateString the string to validate
     */
    private void validateJson(String templateString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(templateString);
        } catch (JsonProcessingException jpe) {
            int contextLines = 2;
            int lineNumberOfIssue = jpe.getLocation().getLineNr();
            List<String> lines = templateString.lines().collect(Collectors.toList());
            int startingIndex = Math.max(lineNumberOfIssue - (contextLines + 1), 0);
            int endingIndex = Math.min(lineNumberOfIssue + contextLines, lines.size() - 1);

            List<String> context = lines.subList(startingIndex, endingIndex);

            String message = String.join("\n", context);
            LOGGER.warn("Error reading String as JSON at line {}:\n{}", lineNumberOfIssue, message);
            fail("Generated ARM template is not valid JSON.\n" + jpe.getMessage());
        }
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
    }

    @ParameterizedTest(name = "buildWithInstanceGroupTypeGatewayAndCore {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildWithInstanceGroupTypeGatewayAndCore(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
        assertTrue(templateString.contains("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
    }

    @ParameterizedTest(name = "buildTestResourceGroupName {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestResourceGroupName(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("resourceGroupName"));
    }

    @ParameterizedTest(name = "buildTestExistingVNETName {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestExistingVNETName(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("existingVNETName"));
        assertTrue(templateString.contains("existingSubnet"));
        assertTrue(templateString.contains("existingResourceGroup"));
    }

    @ParameterizedTest(name = "buildTestExistingSubnetNameNotInTemplate {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestExistingSubnetNameNotInTemplate(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("existingSubnetName"));
    }

    @ParameterizedTest(name = "buildTestVirtualNetworkNamePrefix {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestVirtualNetworkNamePrefix(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("virtualNetworkNamePrefix"));
    }

    @ParameterizedTest(name = "buildTestSubnet1Prefix {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestSubnet1Prefix(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("subnet1Prefix"));
    }

    @ParameterizedTest(name = "buildTestDisksFor1xVersions {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksFor1xVersions(String templatePath) {
        //GIVEN
        assumeFalse(isTemplateVersionGreaterOrEqualThan1165(templatePath));

        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(Collections.emptyMap());

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("[concat('datadisk', 'm0', '0')]");
        assertThat(templateString).contains("[concat('datadisk', 'm0', '1')]");
    }

    @ParameterizedTest(name = "buildTestDisksOnAllVersionsAndVerifyOsDisks {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksOnAllVersionsAndVerifyOsDisks(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("[concat(parameters('vmNamePrefix'),'-osDisk', 'm0')]"));
    }

    @ParameterizedTest(name = "buildTestDisksWhenTheVersion210OrGreater {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestDisksWhenTheVersion210OrGreater(String templatePath) {
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));

        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("[concat(parameters('vmNamePrefix'),'-osDisk', 'm0')]"));
        assertFalse(templateString.contains("[concat('datadisk', 'm0', '1')]"));
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestAvailabilitySetInTemplate(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group gatewayGroup = new Group("gateway", InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap());
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase() + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);

        Group coreGroup = new Group("core", InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap());
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"gatewayAsName\": \"gateway-as\","));
        assertFalse(templateString.contains("coreAsName"));
        assertTrue(templateString.contains("'Microsoft.Compute/availabilitySets', 'gateway-as'"));
        assertFalse(templateString.contains("'Microsoft.Compute/availabilitySets', 'core-as'"));
    }

    @ParameterizedTest(name = "buildTestWithVmVersion {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithVmVersion(String templatePath) {
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));

        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("                   \"apiVersion\": \"2019-07-01\",\n" +
                "                   \"type\": \"Microsoft.Compute/virtualMachines\",\n");
    }

    @ParameterizedTest(name = "buildTestWithNoManagedIdentity {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithNoManagedIdentity(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("\"identity\": {");
    }

    @ParameterizedTest(name = "buildTestWithManagedIdentityGiven {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithManagedIdentityGiven(String templatePath) {
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));

        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
        cloudAdlsGen2View.setManagedIdentity("myIdentity");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE,
                Optional.of(cloudAdlsGen2View), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("\"identity\": {");
        assertThat(templateString).contains("\"type\": \"userAssigned\",");
        assertThat(templateString).contains("                        \"userAssignedIdentities\": {\n" +
                "                            \"myIdentity\": {\n" +
                "                            }\n" +
                "                        }\n");
    }

    @ParameterizedTest(name = "buildTestWithNoDiskEncryptionSetId {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithNoDiskEncryptionSetId(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).doesNotContain("\"diskEncryptionSet\": {");
    }

    @ParameterizedTest(name = "buildTestWithDiskEncryptionSetIdGiven {0}")
    @MethodSource("templatesPathDataProvider")
    public void buildTestWithDiskEncryptionSetIdGiven(String templatePath) {
        //GIVEN
        assumeTrue(isTemplateVersionGreaterOrEqualThan2100(templatePath));

        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);
        when(azureAcceleratedNetworkValidator.validate(any())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        instance.getTemplate().putParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, true);
        instance.getTemplate().putParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "myDES");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty(), createGroupNetwork(), emptyMap()));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("\"diskEncryptionSet\": {");
        assertThat(templateString).contains("\"id\": \"myDES\"");
    }

    private boolean isTemplateVersionGreaterOrEqualThan2100(String templatePath) {
        return isTemplateVersionGreaterOrEqualThan(templatePath, "2.10.0.0");
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential("crn", "test", parameters, "acc", false);
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

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }
}