package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStorageView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
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
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.util.Version;

import freemarker.template.Configuration;

@RunWith(Parameterized.class)
public class AzureTemplateBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final int ROOT_VOLUME_SIZE = 50;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

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

    private final Gson gson = new Gson();

    private final Map<String, String> tags = new HashMap<>();

    private final String templatePath;

    private final Map<String, String> defaultTags = new HashMap<>();

    public AzureTemplateBuilderTest(String templatePath) {
        this.templatePath = templatePath;
    }

    @Parameters(name = "{0}")
    public static Iterable<?> getTemplatesPath() {
        List<String> templates = Lists.newArrayList(LATEST_TEMPLATE_PATH);
        File[] templateFiles = new File(AzureTemplateBuilderTest.class.getClassLoader().getResource("templates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).map(file -> {
            String[] path = file.getPath().split("/");
            return "templates/" + path[path.length - 1];
        }).collect(Collectors.toList());
        templates.addAll(olderTemplates);
        return templates;
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplatePath", templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters.ftl");
        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        groups = new ArrayList<>();
        stackName = "testStack";
        name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25");
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, "existingSubnet");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication, params);
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
        cloudContext = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "test",
                Location.location(Region.region("EU"), new AvailabilityZone("availabilityZone")), USER_ID, WORKSPACE_ID);
        azureCredentialView = new AzureCredentialView(cloudCredential());
        azureStorageView = new AzureStorageView(azureCredentialView, cloudContext, azureStorage, null);

        azureSubnetStrategy = AzureSubnetStrategy.getAzureSubnetStrategy(FILL, Collections.singletonList("existingSubnet"),
                ImmutableMap.of("existingSubnet", 100));
        defaultTags.put(CloudbreakResourceType.DISK.templateVariable(), CloudbreakResourceType.DISK.key());
        defaultTags.put(CloudbreakResourceType.INSTANCE.templateVariable(), CloudbreakResourceType.INSTANCE.key());
        defaultTags.put(CloudbreakResourceType.IP.templateVariable(), CloudbreakResourceType.IP.key());
        defaultTags.put(CloudbreakResourceType.NETWORK.templateVariable(), CloudbreakResourceType.NETWORK.key());
        defaultTags.put(CloudbreakResourceType.SECURITY.templateVariable(), CloudbreakResourceType.SECURITY.key());
        defaultTags.put(CloudbreakResourceType.STORAGE.templateVariable(), CloudbreakResourceType.STORAGE.key());
        defaultTags.put(CloudbreakResourceType.TEMPLATE.templateVariable(), CloudbreakResourceType.TEMPLATE.key());
        reset(azureUtils);
    }

    @Test
    public void buildNoPublicIpNoFirewall() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("publicIPAddress")));
        assertThat(templateString, not(containsString("networkSecurityGroups")));
    }

    @Test
    public void buildNoPublicIpNoFirewallWithTags() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Map<String, String> userDefinedTags = Maps.newHashMap();
        userDefinedTags.put("testtagkey1", "testtagvalue1");
        userDefinedTags.put("testtagkey2", "testtagvalue2");

        cloudStack = new CloudStack(groups, network, image, parameters, userDefinedTags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("publicIPAddress")));
        assertThat(templateString, not(containsString("networkSecurityGroups")));
        assertThat(templateString, containsString("testtagkey"));
        assertThat(templateString, containsString("testtagvalue"));
    }

    @Test
    public void buildNoPublicIpNoFirewallButExistingNetwork() {
        assumeTrue(isTemplateVersionGreaterOrEqualThan());
        //GIVEN
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("publicIPAddress")));
        assertThat(templateString, not(containsString("networkSecurityGroups")));
    }

    @Test
    public void buildNoPublicIpButFirewall() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> false);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("publicIPAddress")));
        assertThat(templateString, containsString("networkSecurityGroups"));
    }

    @Test
    public void buildWithPublicIpAndFirewall() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> false);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> false);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("publicIPAddress"));
        assertThat(templateString, containsString("networkSecurityGroups"));
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }

    @Test
    public void buildWithInstanceGroupTypeCore() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
    }

    @Test
    public void buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"')));
    }

    @Test
    public void buildWithInstanceGroupTypeGateway() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
    }

    @Test
    public void buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"')));
    }

    @Test
    public void buildWithInstanceGroupTypeGatewayAndCore() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + '"'));
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + '"'));
    }

    @Test
    public void buildTestResourceGroupName() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("resourceGroupName")));
    }

    @Test
    public void buildTestExistingVNETName() {
        //GIVEN
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("existingVNETName"));
        assertThat(templateString, containsString("existingSubnet"));
        assertThat(templateString, containsString("existingResourceGroup"));
    }

    @Test
    public void buildTestExistingSubnetNameNotInTemplate() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("existingSubnetName")));
    }

    @Test
    public void buildTestVirtualNetworkNamePrefix() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("virtualNetworkNamePrefix"));
    }

    @Test
    public void buildTestSubnet1Prefix() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("subnet1Prefix"));
    }

    @Test
    public void buildTestDataDisks() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '0')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '1')]"));
    }

    @Test
    @Ignore
    public void buildTestDataDisksShouldThrowAssertionError() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString(),
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '0')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '1')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '2')]"));
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential(1L, "test", parameters);
    }

    private boolean isTemplateVersionGreaterOrEqualThan() {
        if (LATEST_TEMPLATE_PATH.equals(templatePath)) {
            return true;
        }
        String[] splittedName = templatePath.split("-");
        String templateVersion = splittedName[splittedName.length - 1].replaceAll("\\.ftl", "");
        return Version.versionCompare(templateVersion, "1.16.5") > -1;
    }
}