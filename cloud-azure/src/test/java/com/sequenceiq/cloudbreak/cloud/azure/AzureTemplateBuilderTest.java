package com.sequenceiq.cloudbreak.cloud.azure;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
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

import freemarker.template.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class AzureTemplateBuilderTest {

    public static final String CORE_CUSTOM_DATA = "CORE";

    public static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    public static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private Configuration freemarkerConfiguration;

    @InjectMocks
    private AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private String stackName;

    private AzureCredentialView azureCredentialView;

    private List<Group> groups;

    private String name;

    private List<Volume> volumes;

    private CloudInstance instance;

    private List<SecurityRule> rules;

    private Security security;

    private Map<InstanceGroupType, String> userData;

    private Image image;

    private CloudContext cloudContext;

    private CloudStack cloudStack;

    private AzureStorageView azureStorageView;

    private AzureStackView azureStackView;

    private Gson gson = new Gson();

    private Map<String, String> tags = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplatePath", "templates/arm-v2.ftl");
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters.ftl");
        userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        groups = new ArrayList<>();
        stackName = "testStack";
        name = "master";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>());
        instance = new CloudInstance("SOME_ID", instanceTemplate);
        rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, null);
        image = new Image("cb-centos66-amb200-2015-05-25", userData);
        Map<String, String> parameters = new HashMap<>();
        cloudContext = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2", "test",
                Location.location(Region.region("EU"), new AvailabilityZone("availabilityZone")));
        azureCredentialView = new AzureCredentialView(cloudCredential("siq-haas"));
        azureStorageView = new AzureStorageView(azureCredentialView, cloudContext, azureStorage, null);
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
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));

        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        Map<String, String> userDefinedTags = Maps.newHashMap();
        userDefinedTags.put("testtagkey1", "testtagvalue1");
        userDefinedTags.put("testtagkey2", "testtagvalue2");
        cloudStack = new CloudStack(groups, network, image, parameters, userDefinedTags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        //GIVEN
        when(azureUtils.isExistingNetwork(any())).thenReturn(true);
        when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("existingResourceGroup");
        when(azureUtils.getCustomSubnetId(any())).thenReturn("existingSubnet");
        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).then(invocation -> true);
        when(azureUtils.isNoSecurityGroups(any())).then(invocation -> true);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
    public void buildWithInstanceGroupTypeCore() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + "\""));
    }

    @Test
    public void buildWithInstanceGroupTypeCoreShouldNotContainsGatewayCustomData() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + "\"")));
    }

    @Test
    public void buildWithInstanceGroupTypeGateway() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + "\""));
    }

    @Test
    public void buildWithInstanceGroupTypeGatewayShouldNotContainsCoreCustomData() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + "\"")));
    }

    @Test
    public void buildWithInstanceGroupTypeGatewayAndCore() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(CORE_CUSTOM_DATA) + "\""));
        assertThat(templateString, containsString("\"customData\": \"" + base64EncodedUserData(GATEWAY_CUSTOM_DATA) + "\""));
    }

    @Test
    public void buildTestResourceGroupName() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        when(azureUtils.getCustomSubnetId(any())).thenReturn("existingSubnet");
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '0')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '1')]"));
    }

    @Test(expected = AssertionError.class)
    public void buildTestDataDisksShouldThrowAssertionError() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security, null));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null));
        cloudStack = new CloudStack(groups, network, image, parameters, tags, azureTemplateBuilder.getTemplateString());
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView);
        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), Mockito.anyString(),
                any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '0')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '1')]"));
        assertThat(templateString, containsString("[concat('datadisk', 'm0', '2')]"));
    }

    private CloudCredential cloudCredential(String projectId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        return new CloudCredential(1L, "test", "sshkey", "cloudbreak", parameters);
    }
}