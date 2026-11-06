package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.azure.resourcemanager.network.fluent.models.LoadBalancerInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.ProvisioningState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerResourceNames;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureUpscaleLoadBalancerTemplateFilter;
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
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

import freemarker.template.Configuration;

@ExtendWith(MockitoExtension.class)
public class AzureUpscaleLoadBalancerTemplateFilterRenderTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CUSTOM_IMAGE_NAME = "cloudbreak-image.vhd";

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-v2.ftl";

    private static final Map<String, Boolean> ACCELERATED_NETWORK_SUPPORT = Map.of("m1.medium", false);

    private static final String SUBNET_CIDR = "10.0.0.0/24";

    private static final String FIELD_ARM_TEMPLATE_PATH = "armTemplatePath";

    private static final boolean LOADBALANCER_TARGET_STICKY_SESSION = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUpscaleLoadBalancerTemplateFilterRenderTest.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String ACCOUNT_ID = "accountId";

    private static final String LB_TYPE = ArmResourceType.LOAD_BALANCER.getType();

    private static final String PUBLIC_IP_TYPE = ArmResourceType.PUBLIC_IP_ADDRESS.getType();

    private static final String NIC_TYPE = ArmResourceType.NETWORK_INTERFACE.getType();

    private static final Pattern POOL_REF = Pattern.compile(
            "Microsoft\\.Network/loadBalancers/backendAddressPools'\\s*,\\s*'([^']+)'\\s*,\\s*'([^']+)'");

    private static final Pattern LB_DEPENDS_ON = Pattern.compile("resourceId\\('Microsoft\\.Network/loadBalancers','([^']+)'\\)");

    private static final Pattern LB_IP_DEPENDS_ON = Pattern.compile("publicIPAddresses','(LoadBalancer[^']+-publicIp)'\\)");

    private static final List<String> LEGACY_INVALID_JSON_WITHOUT_INSTANCES = List.of(
            "templates/marketplace/arm-with-marketplace.ftl", "templates/marketplace/arm-without-marketplace.ftl");

    @InjectMocks
    private final AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private final Map<String, String> tags = new HashMap<>();

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AzurePlatformResources platformResources;

    @Mock
    private AzureClient azureClient;

    @Mock
    private EntitlementService entitlementService;

    private final AzureUpscaleLoadBalancerTemplateFilter filter = new AzureUpscaleLoadBalancerTemplateFilter();

    private String stackName;

    private AzureCredentialView azureCredentialView;

    private List<Group> groups;

    private InstanceAuthentication instanceAuthentication;

    private CloudInstance instance;

    private Security security;

    private Image image;

    private CloudContext cloudContext;

    private CloudStack cloudStack;

    private AzureStorageView azureStorageView;

    private AzureSubnetStrategy azureSubnetStrategy;

    private AzureStackView azureStackView;

    @BeforeEach
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(filter, "skipExistingLoadBalancer", true);
        ReflectionTestUtils.setField(filter, "entitlementService", entitlementService);
        lenient().when(entitlementService.isAzureUpscaleSkipExistingLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);
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
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", "master", 0L, volumes, InstanceStatus.CREATE_REQUESTED,
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

        azureSubnetStrategy = AzureSubnetStrategy.getAzureSubnetStrategy(FILL, Collections.singletonList("existingSubnet"),
                ImmutableMap.of("existingSubnet", 100L));
        when(customVMImageNameProvider.getImageNameFromConnectionString(anyString())).thenCallRealMethod();
        when(azureUtils.getCustomResourceGroupName(any())).thenReturn("custom-resource-group-name");
        when(azureUtils.getCustomNetworkId(any())).thenReturn("custom-vnet-id");
        lenient().when(azureAcceleratedNetworkValidator.validate(any(), anySet())).thenReturn(ACCELERATED_NETWORK_SUPPORT);
        when(platformResources.virtualMachinesNonExtended(azureCredentialView.getCloudCredential(), cloudContext.getLocation().getRegion(), null))
                .thenReturn(new CloudVmTypes());
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

    // Renders real UPSCALE templates (the latest one and old stored ones) and checks that leaving out the existing load balancers keeps the
    // template valid: nothing dangles, the NIC pool memberships are untouched and nothing else changes. Setup copied from AzureTemplateBuilderPart2Test.

    enum Scenario {
        PRIVATE_EXISTING_VNET,
        PRIVATE_PUBLIC_NEW_VNET_SKIP_BOTH,
        PRIVATE_PUBLIC_EXISTING_VNET_SKIP_PRIVATE_ONLY,
        OUTBOUND_EXISTING_VNET,
        PRIVATE_GATEWAY_EXISTING_VNET,
        NO_INSTANCES_EXISTING_VNET,
        COMPUTE_ONLY_EXISTING_VNET
    }

    static Stream<Arguments> stripCases() {
        List<Arguments> args = new ArrayList<>();
        for (String template : List.of(LATEST_TEMPLATE_PATH, "templates/marketplace/arm-with-marketplace.ftl",
                "templates/marketplace/arm-without-marketplace.ftl")) {
            for (Scenario scenario : Scenario.values()) {
                args.add(Arguments.of(template, scenario));
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("stripCases")
    void stripSkippableLoadBalancersFromUpscaleTemplate(String templatePath, Scenario scenario) throws Exception {
        String rendered = renderUpscaleTemplate(templatePath, scenario);

        if (scenario == Scenario.PRIVATE_GATEWAY_EXISTING_VNET && LATEST_TEMPLATE_PATH.equals(templatePath)) {
            // legacy fixtures predate GATEWAY_PRIVATE support and render a plain PRIVATE LB for this scenario
            assertTrue(rendered.contains("-frontend-gateway") && rendered.contains("gatewaySubnet"), "gateway frontend should be rendered");
        }
        if (scenario == Scenario.NO_INSTANCES_EXISTING_VNET && LEGACY_INVALID_JSON_WITHOUT_INSTANCES.contains(templatePath)) {
            // Pre-existing: legacy stored templates render invalid JSON when there are no new instances. The SDK's readTree fails the same way
            // today; the filter must not make it worse and must return the original template.
            assertThrows(JsonProcessingException.class, () -> OBJECT_MAPPER.readTree(rendered));
            assertEquals(Set.of(), filter.decide(rendered, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
            assertEquals(rendered, filter.apply(rendered, Set.of("LoadBalancertestStackPRIVATE")));
            return;
        }

        JsonNode original = OBJECT_MAPPER.readTree(rendered);
        Map<String, List<String>> lbPools = loadBalancerPools(original);
        assertFalse(lbPools.isEmpty(), "rendered upscale template should contain a load balancer");

        // invariant: pools referenced by NICs are a subset of the pools declared on the rendered LB (scoped to NICs; LB rules reference pools too)
        Matcher nicRefs = POOL_REF.matcher(resourcesOfType(original, NIC_TYPE));
        int nicPoolRefs = 0;
        while (nicRefs.find()) {
            nicPoolRefs++;
            assertTrue(lbPools.getOrDefault(nicRefs.group(1), List.of()).contains(nicRefs.group(2)),
                    "NIC references pool not declared on the LB: " + nicRefs.group(1) + "/" + nicRefs.group(2));
        }

        // every load balancer exists in Azure with its pools, except the PUBLIC one in the partial case, which misses its pools
        Set<String> expectedSkippable = new HashSet<>(lbPools.keySet());
        if (scenario == Scenario.PRIVATE_PUBLIC_EXISTING_VNET_SKIP_PRIVATE_ONLY) {
            expectedSkippable.removeIf(name -> !name.endsWith("PRIVATE"));
        }
        lbPools.forEach((lbName, pools) -> mockLiveLoadBalancer(lbName, expectedSkippable.contains(lbName) ? pools : List.of()));
        Set<String> skippable = filter.decide(rendered, azureClient, RESOURCE_GROUP, ACCOUNT_ID);
        assertEquals(expectedSkippable, skippable);

        String filtered = filter.apply(rendered, skippable);
        assertNotEquals(rendered, filtered, "post-condition must not abort for a well-formed template");
        JsonNode result = OBJECT_MAPPER.readTree(filtered);

        // removed resources are gone, retained LBs (and their public IPs) are kept
        for (JsonNode resource : result.path("resources")) {
            String type = resource.path("type").asText();
            String name = resource.path("name").asText();
            assertFalse(LB_TYPE.equals(type) && skippable.contains(name), "skipped LB still present: " + name);
            assertFalse(PUBLIC_IP_TYPE.equals(type) && skippable.stream().anyMatch(lb -> AzureLoadBalancerResourceNames.publicIpName(lb).equals(name)),
                    "LB public IP still present: " + name);
        }
        Set<String> retained = new HashSet<>(lbPools.keySet());
        retained.removeAll(skippable);
        assertEquals(retained, loadBalancerPools(result).keySet());

        // no dangling dependsOn to an LB or LB public IP
        Set<String> present = new HashSet<>();
        result.path("resources").forEach(r -> present.add(r.path("type").asText() + "|" + r.path("name").asText()));
        for (JsonNode resource : result.path("resources")) {
            for (JsonNode dependency : resource.path("dependsOn")) {
                String normalized = dependency.asText().replaceAll("\\s+", "");
                Matcher lb = LB_DEPENDS_ON.matcher(normalized);
                if (lb.find()) {
                    assertTrue(present.contains(LB_TYPE + "|" + lb.group(1)), "dangling dependsOn: " + normalized);
                }
                Matcher ip = LB_IP_DEPENDS_ON.matcher(normalized);
                if (ip.find()) {
                    assertTrue(present.contains(PUBLIC_IP_TYPE + "|" + ip.group(1)), "dangling dependsOn: " + normalized);
                }
            }
        }

        // NIC pool memberships are untouched
        Matcher nicRefsAfter = POOL_REF.matcher(resourcesOfType(result, NIC_TYPE));
        int nicPoolRefsAfter = 0;
        while (nicRefsAfter.find()) {
            nicPoolRefsAfter++;
        }
        assertEquals(nicPoolRefs, nicPoolRefsAfter, "NIC pool references changed");

        // nothing outside dependsOn changed, apart from the removed resources
        ObjectNode expected = original.deepCopy();
        ArrayNode expectedResources = (ArrayNode) expected.path("resources");
        for (int i = expectedResources.size() - 1; i >= 0; i--) {
            if (isRemovable(expectedResources.get(i), skippable)) {
                expectedResources.remove(i);
            }
        }
        assertEquals(withoutDependsOn(expected), withoutDependsOn(result));

        if (scenario == Scenario.NO_INSTANCES_EXISTING_VNET) {
            assertEquals(0, result.path("resources").size(), "flow-restart upscale should become an empty, valid deployment");
        }
        LOGGER.info("{} {}: lbs={} skip={} resources {}->{} nicPoolRefs={}", templatePath, scenario, lbPools, skippable,
                original.path("resources").size(), result.path("resources").size(), nicPoolRefs);
    }

    private String renderUpscaleTemplate(String templatePath, Scenario scenario) {
        ReflectionTestUtils.setField(azureTemplateBuilder, FIELD_ARM_TEMPLATE_PATH, templatePath);
        Network network = new Network(new Subnet(SUBNET_CIDR));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        lenient().when(azureUtils.isExistingNetwork(any())).thenReturn(scenario != Scenario.PRIVATE_PUBLIC_NEW_VNET_SKIP_BOTH);
        lenient().when(azureUtils.getCustomNetworkId(any())).thenReturn("existingNetworkName");
        lenient().when(azureUtils.getCustomSubnetIds(any())).thenReturn(Collections.singletonList("existingSubnet"));
        if (scenario == Scenario.PRIVATE_GATEWAY_EXISTING_VNET) {
            lenient().when(azureUtils.getCustomEndpointGatewaySubnetIds(any())).thenReturn(List.of("gatewaySubnet"));
        }
        lenient().when(azureStorage.getImageStorageName(any(AzureCredentialView.class), any(CloudContext.class), any(CloudStack.class))).thenReturn("test");
        lenient().when(azureStorage.getDiskContainerName(any(CloudContext.class))).thenReturn("testStorageContainer");

        boolean gatewayWithoutInstances = scenario == Scenario.NO_INSTANCES_EXISTING_VNET || scenario == Scenario.COMPUTE_ONLY_EXISTING_VNET;
        Group gateway = gatewayWithoutInstances
                ? Group.builder().withName("gateway").withType(InstanceGroupType.GATEWAY).withSecurity(security).build()
                : getGroup("gateway", InstanceGroupType.GATEWAY);
        groups.add(gateway);
        if (scenario == Scenario.COMPUTE_ONLY_EXISTING_VNET) {
            groups.add(getGroup("compute", InstanceGroupType.CORE));
        }
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        switch (scenario) {
            case PRIVATE_PUBLIC_NEW_VNET_SKIP_BOTH, PRIVATE_PUBLIC_EXISTING_VNET_SKIP_PRIVATE_ONLY -> {
                loadBalancers.add(loadBalancer(LoadBalancerType.PRIVATE, gateway));
                loadBalancers.add(loadBalancer(LoadBalancerType.PUBLIC, gateway));
            }
            case OUTBOUND_EXISTING_VNET -> loadBalancers.add(loadBalancer(LoadBalancerType.OUTBOUND, gateway));
            case PRIVATE_GATEWAY_EXISTING_VNET -> {
                loadBalancers.add(loadBalancer(LoadBalancerType.PRIVATE, gateway));
                loadBalancers.add(loadBalancer(LoadBalancerType.GATEWAY_PRIVATE, gateway));
            }
            default -> loadBalancers.add(loadBalancer(LoadBalancerType.PRIVATE, gateway));
        }
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
        azureStackView = new AzureStackView("mystack", 3, groups, azureStorageView, azureSubnetStrategy, Collections.emptyMap(), null);
        return azureTemplateBuilder.build(stackName, CUSTOM_IMAGE_NAME, azureCredentialView, azureStackView, cloudContext, cloudStack,
                AzureInstanceTemplateOperation.UPSCALE, null);
    }

    private void mockLiveLoadBalancer(String lbName, List<String> pools) {
        LoadBalancer liveLoadBalancer = mock(LoadBalancer.class);
        LoadBalancerInner inner = mock(LoadBalancerInner.class);
        when(inner.provisioningState()).thenReturn(ProvisioningState.SUCCEEDED);
        when(liveLoadBalancer.innerModel()).thenReturn(inner);
        Map<String, LoadBalancerBackend> backends = new HashMap<>();
        pools.forEach(pool -> backends.put(pool, mock(LoadBalancerBackend.class)));
        when(liveLoadBalancer.backends()).thenReturn(backends);
        when(azureClient.getLoadBalancer(RESOURCE_GROUP, lbName)).thenReturn(Optional.of(liveLoadBalancer));
    }

    private CloudLoadBalancer loadBalancer(LoadBalancerType type, Group target) {
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(type, LoadBalancerSku.STANDARD, LOADBALANCER_TARGET_STICKY_SESSION);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(List.of(target)));
        return loadBalancer;
    }

    private static Map<String, List<String>> loadBalancerPools(JsonNode template) {
        Map<String, List<String>> pools = new HashMap<>();
        for (JsonNode resource : template.path("resources")) {
            if (LB_TYPE.equals(resource.path("type").asText())) {
                List<String> names = new ArrayList<>();
                resource.path("properties").path("backendAddressPools").forEach(pool -> names.add(pool.path("name").asText()));
                pools.put(resource.path("name").asText(), names);
            }
        }
        return pools;
    }

    private static String resourcesOfType(JsonNode template, String type) {
        StringBuilder text = new StringBuilder();
        template.path("resources").forEach(resource -> {
            if (type.equals(resource.path("type").asText())) {
                text.append(resource);
            }
        });
        return text.toString();
    }

    private static boolean isRemovable(JsonNode resource, Set<String> skippable) {
        String type = resource.path("type").asText();
        String name = resource.path("name").asText();
        return (LB_TYPE.equals(type) && skippable.contains(name))
                || (PUBLIC_IP_TYPE.equals(type) && skippable.stream().anyMatch(lb -> AzureLoadBalancerResourceNames.publicIpName(lb).equals(name)));
    }

    private static JsonNode withoutDependsOn(JsonNode template) {
        ObjectNode copy = template.deepCopy();
        copy.path("resources").forEach(resource -> ((ObjectNode) resource).remove("dependsOn"));
        return copy;
    }
}
