package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
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
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

import freemarker.template.Configuration;
import freemarker.template.Template;

@ExtendWith(MockitoExtension.class)
public class AzureTemplateBuilderMissingMarketplaceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilderMissingMarketplaceTest.class);

    @InjectMocks
    private final AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private final Gson gson = new Gson();

    private final Map<String, String> tags = new HashMap<>();

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    private Configuration freemarkerConfiguration;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AzurePlatformResources platformResources;

    private Configuration freemarkerConfigurationHelper;

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

    @BeforeEach
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        freemarkerConfiguration = factoryBean.getObject();
        FreeMarkerConfigurationFactoryBean factoryBean1 = new FreeMarkerConfigurationFactoryBean();
        factoryBean1.setPreferFileSystemAccess(false);
        factoryBean1.setTemplateLoaderPath("classpath:/");
        factoryBean1.afterPropertiesSet();
        freemarkerConfigurationHelper = factoryBean1.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", freemarkerConfiguration);
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
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
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
        when(platformResources.virtualMachinesNonExtended(azureCredentialView.getCloudCredential(), cloudContext.getLocation().getRegion(), null))
                .thenReturn(new CloudVmTypes());
    }

    @Test
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithNoMarketplaceTemplateNoLoadBalancer() throws Exception {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, LATEST_TEMPLATE_PATH);

        String noMarketplaceTemplate = "templates/marketplace/arm-without-marketplace.ftl";

        Network network = new Network(new Subnet("testSubnet"));
        when(azureUtils.isPrivateIp(any())).thenReturn(false);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        instance.getTemplate().putParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, true);
        instance.getTemplate().putParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "myDES");

        groups.add(Group.builder()
                .withName(name)
                .withType(InstanceGroupType.CORE)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .build());
        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .template(freemarkerConfigurationHelper.getTemplate(noMarketplaceTemplate, "UTF-8").toString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);

        //WHEN
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);

        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("""
                                   "imageReference": {
                                            "publisher": "cloudera",
        """);
        assertThat(templateString).doesNotContain("""
                                   "imageReference": {
                                           "id": "cloudbreak-image.vhd"
                                   }
        """);
        assertThat(templateString).contains("""
                           "plan": {
                                "name": "my-plan",
                                "product": "my-offer",
                                "publisher": "cloudera"
                           },
        """);
        assertThat(templateString).doesNotContain("\"type\": \"Microsoft.Network/loadBalancers\"");
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(templateCaptor.capture(), any(Map.class));
        assertEquals(freemarkerConfigurationHelper.getTemplate(LATEST_TEMPLATE_PATH, "UTF-8").toString(), templateCaptor.getValue().toString());
        AzureTestUtils.validateJson(templateString);
    }

    @Test
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithNoMarketplaceTemplateMultipleLoadBalancer() throws Exception {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, LATEST_TEMPLATE_PATH);
        String noMarketplaceTemplate = "templates/marketplace/arm-without-marketplace.ftl";
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(getGatewayGroup());

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
                .loadBalancers(loadBalancers)
                .tags(tags)
                .template(freemarkerConfigurationHelper.getTemplate(noMarketplaceTemplate, "UTF-8").toString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertThat(templateString).contains("""
                                   "imageReference": {
                                            "publisher": "cloudera",
        """);
        assertThat(templateString).doesNotContain("""
                                   "imageReference": {
                                           "id": "cloudbreak-image.vhd"
                                   }
        """);
        assertThat(templateString).contains("""
                           "plan": {
                                "name": "my-plan",
                                "product": "my-offer",
                                "publisher": "cloudera"
                           },
        """);
        assertThat(templateString).doesNotContain("\"Basic\"");
        assertThat(templateString).contains("\"Standard\"");
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(templateCaptor.capture(), any(Map.class));
        assertEquals(freemarkerConfigurationHelper.getTemplate(LATEST_TEMPLATE_PATH, "UTF-8").toString(), templateCaptor.getValue().toString());
        AzureTestUtils.validateJson(templateString);
    }

    @Test
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void buildTestWithNoMarketplaceTemplateButVhdImageMultipleLoadBalancer() throws Exception {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, LATEST_TEMPLATE_PATH);
        String noMarketplaceTemplate = "templates/marketplace/arm-without-marketplace.ftl";
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(getGatewayGroup());

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
                .loadBalancers(loadBalancers)
                .tags(tags)
                .template(freemarkerConfigurationHelper.getTemplate(noMarketplaceTemplate, "UTF-8").toString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
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
                                "name": "my-plan",
                                "product": "my-offer",
                                "publisher": "cloudera"
                           },
        """);
        assertThat(templateString).doesNotContain("\"Basic\"");
        assertThat(templateString).contains("\"Standard\"");
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(templateCaptor.capture(), any(Map.class));
        assertEquals(freemarkerConfigurationHelper.getTemplate(LATEST_TEMPLATE_PATH, "UTF-8").toString(), templateCaptor.getValue().toString());
        AzureTestUtils.validateJson(templateString);
    }

    @Test
    public void buildTestWithMarketplaceTemplateShouldUseOriginalImage() throws Exception {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, LATEST_TEMPLATE_PATH);
        String marketplaceTemplate = "templates/marketplace/arm-with-marketplace.ftl";
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        groups.add(getGatewayGroup());

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
                .template(freemarkerConfigurationHelper.getTemplate(marketplaceTemplate, "UTF-8").toString())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, null);
        //THEN
        gson.fromJson(templateString, Map.class);
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(templateCaptor.capture(), any(Map.class));
        assertEquals(new Template(LATEST_TEMPLATE_PATH, cloudStack.getTemplate(), freemarkerConfiguration).toString(), templateCaptor.getValue().toString());
        AzureTestUtils.validateJson(templateString);
    }

    private Group getGatewayGroup() {
        return Group.builder()
                .withName("gateway-group")
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .build();
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential("crn", "test", parameters, "acc");
    }
}
