package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
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
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

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
public class AzureTemplateBuilderFreeIpaTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH_FREEIPA = "templates/arm-v2-freeipa.ftl";

    private static final String LATEST_TEMPLATE_PATH_FREEIPA_LB = "templates/arm-v2-freeipa-lb.ftl";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final String FIELD_ARM_TEMPLATE_LB_PATH = "armTemplateLbPath";

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

    public static Iterable<?> templatesPathDataProviderFreeIPA() {
        return Lists.newArrayList(LATEST_TEMPLATE_PATH_FREEIPA);
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
        when(platformResources.virtualMachinesNonExtended(azureCredentialView.getCloudCredential(), cloudContext.getLocation().getRegion(), null))
                .thenReturn(new CloudVmTypes());
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProviderFreeIPA")
    public void testLoadBalancer(String templatePath) {
        reset(customVMImageNameProvider);
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_LB_PATH, LATEST_TEMPLATE_PATH_FREEIPA_LB);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters-freeipa.ftl");

        Network network = new Network(new Subnet(SUBNET_CIDR), Map.of("networkId", "network1", "resourceGroupName", "cdp-rg", "subnetId", "existingSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group masterGroup = Group.builder()
                .withName("MASTER")
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(Collections.singletonList(instance))
                .withSecurity(security)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .withNetwork(createGroupNetwork())
                .build();
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = masterGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        masterGroup.putParameter("availabilitySet", asMap);
        groups.add(masterGroup);

        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE, LoadBalancerSku.STANDARD, false);
        loadBalancer.addPortToTargetGroupMapping(
                new TargetGroupPortPair(636, NetworkProtocol.TCP, new HealthProbeParameters("/lb-healthcheck", 5030, NetworkProtocol.HTTPS, 10, 2)),
                Set.of(masterGroup));
        List<CloudLoadBalancer> loadBalancers = List.of(loadBalancer);
        cloudStack = new CloudStack(groups, network, image, parameters, tags, "",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(),
                null, loadBalancers, null, GATEWAY_CUSTOM_DATA, CORE_CUSTOM_DATA, false, null);
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        String templateString = azureTemplateBuilder.buildLoadBalancer(stackName, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.PROVISION);
        ObjectMapper objectMapper = new ObjectMapper();
        assertDoesNotThrow(() -> {
            objectMapper.readTree(templateString);
        });
        assertTrue(templateString.contains("loadBalancers"));
        assertTrue(templateString.contains("\"name\": \"port-636TCP-rule\","));
        assertTrue(templateString.contains("\"name\": \"port-5030-probe\","));
        assertTrue(templateString.contains("{ \"id\": \"[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', " +
                "'LoadBalancertestStackPRIVATE', 'LoadBalancertestStackPRIVATE-pool')]\" }"));
        assertTrue(templateString.contains("\"requestPath\": \"/lb-healthcheck\","));
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProviderFreeIPA")
    public void buildTestAvailabilitySetInFreeIPATemplate(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters-freeipa.ftl");

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");

        Group gatewayGroup = getGatewayGroup(Collections.singletonList(instance));
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);

        Group coreGroup = getCoreGroup(Collections.singletonList(instance));
        coreGroup.setRootVolumeType(AzureDiskType.STANDARD_SSD_LRS.value());
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);

        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .instanceAuthentication(instanceAuthentication)
                .template(azureTemplateBuilder.getTemplateString())
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertTrue(templateString.contains("\"gatewayAsName\": \"gateway-as\","));
        assertTrue(templateString.contains("\"gatewayAsFaultDomainCount\": 2,"));
        assertTrue(templateString.contains("\"gatewayAsUpdateDomainCount\": 20,"));
        assertTrue(templateString.contains("'Microsoft.Compute/availabilitySets', 'gateway-as'"));
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertTrue(strippedTemplateString.contains("\"dependsOn\":[\"[concat('Microsoft.Compute/availabilitySets/','gateway-as')]\""));
        assertTrue(strippedTemplateString.contains("\"properties\":{\"availabilitySet\":{\"id\"" +
                ":\"[resourceId('Microsoft.Compute/availabilitySets','gateway-as')]"));
        assertTrue(templateString.contains("\"storageAccountType\": \"StandardSSD_LRS\""));

    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProviderFreeIPA")
    public void buildTestAvailabilitySetNotInFreeIPATemplateForMultiAz(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters-freeipa.ftl");

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");

        Group gatewayGroup = getGatewayGroup(Collections.singletonList(instance));
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);

        Group coreGroup = getCoreGroup(Collections.singletonList(instance));
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);

        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .instanceAuthentication(instanceAuthentication)
                .template(azureTemplateBuilder.getTemplateString())
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .multiAz(true)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        assertFalse(templateString.contains("\"gatewayAsName\": \"gateway-as\","));
        assertFalse(templateString.contains("\"gatewayAsFaultDomainCount\": 2,"));
        assertFalse(templateString.contains("\"gatewayAsUpdateDomainCount\": 20,"));
        assertFalse(templateString.contains("'Microsoft.Compute/availabilitySets', 'gateway-as'"));
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains("\"dependsOn\":[\"[concat('Microsoft.Compute/availabilitySets/','gateway-as')]\""));
        assertFalse(strippedTemplateString.contains("\"properties\":{\"availabilitySet\":{\"id\"" +
                ":\"[resourceId('Microsoft.Compute/availabilitySets','gateway-as')]"));
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProviderFreeIPA")
    public void buildTestZonesInformationInFreeIPATemplateForMultiAz(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters-freeipa.ftl");

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");

        instance.setAvailabilityZone("1");

        CloudInstance instance1 = new CloudInstance("SOME_ID", instance.getTemplate(), instanceAuthentication, "existingSubnet", "2", instance.getParameters());

        Group gatewayGroup = getGatewayGroup(List.of(instance, instance1));
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);

        Group coreGroup = getCoreGroup(List.of(instance, instance1));
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);

        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .instanceAuthentication(instanceAuthentication)
                .template(azureTemplateBuilder.getTemplateString())
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .multiAz(true)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertTrue(strippedTemplateString.contains("\"zones\":[\"1\"],\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"}"));
        assertTrue(strippedTemplateString.contains("\"zones\":[\"2\"],\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"}"));
        assertTrue(strippedTemplateString.contains("\"zones\":[\"1\"],\"properties\""));
        assertTrue(strippedTemplateString.contains("\"zones\":[\"2\"],\"properties\""));
    }

    @ParameterizedTest(name = "buildTestAvailabilitySetInTemplate {0}")
    @MethodSource("templatesPathDataProviderFreeIPA")
    public void buildTestZonesInformationNotPresentInFreeIPATemplate(String templatePath) {
        //GIVEN
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters-freeipa.ftl");

        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");

        Group gatewayGroup = getGatewayGroup(Collections.singletonList(instance));
        Map<String, Object> asMap = new HashMap<>();
        String availabilitySetName = gatewayGroup.getType().name().toLowerCase(Locale.ROOT) + "-as";
        asMap.put("name", availabilitySetName);
        asMap.put("faultDomainCount", 2);
        asMap.put("updateDomainCount", 20);
        gatewayGroup.putParameter("availabilitySet", asMap);
        groups.add(gatewayGroup);

        Group coreGroup = getCoreGroup(Collections.singletonList(instance));
        coreGroup.putParameter("availabilitySet", null);
        groups.add(coreGroup);

        cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(tags)
                .instanceAuthentication(instanceAuthentication)
                .template(azureTemplateBuilder.getTemplateString())
                .gatewayUserData(GATEWAY_CUSTOM_DATA)
                .coreUserData(CORE_CUSTOM_DATA)
                .build();
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, emptyMap());

        //WHEN
        when(azureAcceleratedNetworkValidator.validate(azureStackView, Set.of())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");
        String templateString =
                azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                        AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        //THEN
        gson.fromJson(templateString, Map.class);
        String strippedTemplateString = templateString.replaceAll("\\s", "");
        assertFalse(strippedTemplateString.contains("\"zones\":[\"1\"],\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"}"));
        assertFalse(strippedTemplateString.contains("\"zones\":[\"2\"],\"sku\":{\"name\":\"Standard\",\"tier\":\"Regional\"}"));
        assertFalse(strippedTemplateString.contains("\"zones\":[\"1\"],\"properties\""));
        assertFalse(strippedTemplateString.contains("\"zones\":[\"2\"],\"properties\""));
    }

    private Group getGatewayGroup(List<CloudInstance> instances) {
        Group gatewayGroup = Group.builder()
                .withName("gateway")
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(instances)
                .withSecurity(security)
                .build();
        return gatewayGroup;
    }

    private Group getCoreGroup(List<CloudInstance> instances) {
        Group coreGroup = Group.builder()
                .withName("core")
                .withType(InstanceGroupType.CORE)
                .withInstances(instances)
                .withSecurity(security)
                .build();
        return coreGroup;
    }

    private CloudCredential cloudCredential() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", "siq-haas");
        return new CloudCredential("crn", "test", parameters, "acc");
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }
}
