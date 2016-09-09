package com.sequenceiq.cloudbreak.cloud.arm;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStackView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStorageView;
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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

import freemarker.template.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class ArmTemplateBuilderTest {
    @Mock
    private ArmUtils armUtils;

    @Mock
    private ArmStorage armStorage;

    @Mock
    private Configuration freemarkerConfiguration;

    @InjectMocks
    private ArmTemplateBuilder armTemplateBuilder = new ArmTemplateBuilder();

    private String stackName;
    private ArmCredentialView armCredentialView;
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
    private ArmStorageView armStorageView;
    private ArmStackView armStackView;
    private Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(armTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(armTemplateBuilder, "armTemplatePath", "templates/arm-v2.ftl");
        ReflectionTestUtils.setField(armTemplateBuilder, "armTemplateParametersPath", "templates/parameters.ftl");
        userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        groups = new ArrayList<>();
        stackName = "testStack";
        name = "master";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>());
        instance = new CloudInstance("SOME_ID", instanceTemplate);
        rules = Collections.singletonList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        security = new Security(rules);
        image = new Image("cb-centos66-amb200-2015-05-25", userData);
        Map<String, String> parameters = new HashMap<>();
        cloudContext = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2", "test",
                Location.location(Region.region("EU"), new AvailabilityZone("availabilityZone")));
        armCredentialView = new ArmCredentialView(cloudCredential("siq-haas"));
        armStorageView = new ArmStorageView(armCredentialView, cloudContext, armStorage, null);
    }

    @Test
    public void buildWithInstanceGroupTypeCore() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Dynamic\""));
    }

    @Test(expected = AssertionError.class)
    public void buildWithInstanceGroupTypeCoreShouldThrowAssertionError() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Static\""));
    }

    @Test
    public void buildWithInstanceGroupTypeGateway() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Static\""));
    }

    @Test(expected = AssertionError.class)
    public void buildWithInstanceGroupTypeGatewayShouldThrowAssertionError() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Dynamic\""));
    }

    @Test
    public void buildWithInstanceGroupTypeGatewayAndCore() throws Exception {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Static\""));
        assertThat(templateString, containsString("\"publicIPAllocationMethod\": \"Dynamic\""));
    }

    @Test
    public void buildTestResourceGroupName() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("resourceGroupName")));
    }

    @Test
    public void buildTestExistingVNETName() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString, not(containsString("existingVNETName")));
    }

    @Test
    public void buildTestExistingSubnetName() {
        //GIVEN
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
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
        groups.add(new Group(name, InstanceGroupType.GATEWAY, Collections.singletonList(instance), security));
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        cloudStack = new CloudStack(groups, network, image, parameters);
        armStackView = new ArmStackView(groups, armStorageView);
        //WHEN
        when(armStorage.getImageStorageName(Mockito.any(ArmCredentialView.class), Mockito.any(CloudContext.class), Mockito.anyString(),
                Mockito.any(ArmAttachedStorageOption.class))).thenReturn("test");
        when(armStorage.getDiskContainerName(Mockito.any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString = armTemplateBuilder.build(stackName, armCredentialView, armStackView, cloudContext, cloudStack);
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