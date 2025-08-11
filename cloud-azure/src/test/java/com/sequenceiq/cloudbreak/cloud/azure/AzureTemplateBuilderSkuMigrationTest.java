package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
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
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
public class AzureTemplateBuilderSkuMigrationTest {

    public static final Region REGION = Region.region("region");

    private static final String FIELD_ARM_TEMPLATE_REMOVE_PUBLIC_IP_PATH = "templates/arm-remove-public-ip-associations.ftl";

    private static final String FIELD_ARM_TEMPLATE_ATTACH_PUBLIC_IP_PATH = "templates/arm-attach-public-ip-associations.ftl";

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzurePlatformResources platformResources;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private AzureTemplateBuilder azureTemplateBuilder;

    @BeforeEach
    public void setUp() throws TemplateException, IOException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateRemovePublicIpPath", FIELD_ARM_TEMPLATE_REMOVE_PUBLIC_IP_PATH);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateAttachPublicIpPath", FIELD_ARM_TEMPLATE_ATTACH_PUBLIC_IP_PATH);
    }

    @Test
    public void testBuildPublicIpDetachForVMs() {
        TestData testData = getTestData();

        String generatedTemplate = azureTemplateBuilder.buildPublicIpDetachForVMs("stack-name", testData.cloudContext(),
                testData.azureStackView(), testData.cloudStack());
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertFalse(generatedTemplate.contains("\"frontendPort\": 443,"));
        assertFalse(generatedTemplate.contains("\"backendPort\": 443,"));
        assertFalse(generatedTemplate.contains("\"name\": \"port-443-rule\","));
        assertFalse(generatedTemplate.contains("\"name\": \"port-8443-probe\","));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertFalse(generatedTemplate.contains("\"name\": \"Standard\""));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Network/virtualNetworks\""));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Compute/virtualMachines\","));
        assertFalse(generatedTemplate.contains("Microsoft.Compute/availabilitySets"));
        assertFalse(generatedTemplate.contains("Microsoft.Network/networkSecurityGroups"));
        assertTrue(generatedTemplate.contains("ipConfigurations"));
        AzureTestUtils.validateJson(generatedTemplate);
    }

    @Test
    public void testBuildAttachPublicIpsForVMsAndAddLB() {
        TestData testData = getTestData();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        AzureCredentialView armCredentialView = new AzureCredentialView(cloudCredential);
        when(platformResources.virtualMachinesNonExtended(cloudCredential, REGION, null)).thenReturn(new CloudVmTypes(Map.of(), Map.of()));
        when(azureAcceleratedNetworkValidator.validate(eq(testData.azureStackView), any())).thenReturn(Map.of("Standard_D4_v5", true));
        String generatedTemplate = azureTemplateBuilder.buildAttachPublicIpsForVMsAndAddLB("stack-name", testData.cloudContext(), armCredentialView,
                testData.azureStackView(), testData.cloudStack());
        assertTrue(generatedTemplate.contains("\"type\": \"Microsoft.Network/loadBalancers\","));
        assertTrue(generatedTemplate.contains("\"frontendPort\": 443,"));
        assertTrue(generatedTemplate.contains("\"backendPort\": 443,"));
        assertTrue(generatedTemplate.contains("\"name\": \"port-443-rule\","));
        assertTrue(generatedTemplate.contains("\"name\": \"port-8443-probe\","));
        assertTrue(generatedTemplate.contains("\"type\": \"Microsoft.Network/publicIPAddresses\","));
        assertTrue(generatedTemplate.contains("\"name\": \"Standard\""));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Network/virtualNetworks\""));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Compute/virtualMachines\","));
        assertFalse(generatedTemplate.contains("Microsoft.Compute/availabilitySets"));
        assertFalse(generatedTemplate.contains("\"type\": \"Microsoft.Network/networkSecurityGroups\","));
        AzureTestUtils.validateJson(generatedTemplate);
    }

    private TestData getTestData() {
        Network network = mock(Network.class);
        when(azureUtils.isExistingNetwork(network)).thenReturn(true);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn("my-resource-group");
        when(azureUtils.getCustomNetworkId(network)).thenReturn("custom-network-id");
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getLocation()).thenReturn(Location.location(REGION, AvailabilityZone.availabilityZone("az-1")));

        List<Group> groups = new ArrayList<>();

        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));
        InstanceTemplate instanceTemplate = new InstanceTemplate("Standard_D4_v5", "master", 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group master = buildGroup("master", InstanceGroupType.GATEWAY, instanceTemplate, instanceAuthentication);
        Group worker = buildGroup("worker", InstanceGroupType.CORE, instanceTemplate, instanceAuthentication);
        groups.add(master);
        groups.add(worker);
        String coreUserData = "coreUserData";
        String gwUserData = "gwUserData";
        Map<InstanceGroupType, String> userData = Map.of(
                InstanceGroupType.CORE, coreUserData,
                InstanceGroupType.GATEWAY, gwUserData
        );

        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "",
                "default", "default-id", new HashMap<>(), "2019-10-24", 1571884856L, null);

        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        CloudLoadBalancer loadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        loadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 8443), new HashSet<>(groups));
        loadBalancers.add(loadBalancer);

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(network)
                .loadBalancers(loadBalancers)
                .image(image)
                .parameters(Map.of())
                .tags(Map.of())
                .instanceAuthentication(instanceAuthentication)
                .gatewayUserData(gwUserData)
                .coreUserData(coreUserData)
                .build();

        AzureStackView azureStackView = new AzureStackView("mystack", 3, groups, mock(AzureStorageView.class),
                mock(AzureSubnetStrategy.class), Collections.emptyMap());
        return new TestData(cloudContext, cloudStack, azureStackView);
    }

    private Group buildGroup(String name, InstanceGroupType instanceGroupType, InstanceTemplate instanceTemplate,
            InstanceAuthentication instanceAuthentication) {
        return Group.builder()
                .withName(name)
                .withSecurity(new Security(List.of(new SecurityRule("10.0.0.0/0", new PortDefinition[] { new PortDefinition("443", "443")}, "tcp")),
                        new ArrayList<>()))
                .withType(instanceGroupType)
                .withInstances(Collections.singletonList(new CloudInstance("i1", instanceTemplate, instanceAuthentication, "subnet-1", "az-1")))
                .build();
    }

    private record TestData(CloudContext cloudContext, CloudStack cloudStack, AzureStackView azureStackView) {
    }

}
