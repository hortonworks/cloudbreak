package com.sequenceiq.cloudbreak.cloud.openstack.heat;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
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
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.heat.HeatTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(Parameterized.class)
public class HeatTemplateBuilderTest {

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private OpenStackUtils openStackUtil;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @InjectMocks
    private final HeatTemplateBuilder heatTemplateBuilder = new HeatTemplateBuilder();

    private String stackName;

    private List<Group> groups;

    private Image image;

    private String templatePath;

    public HeatTemplateBuilderTest(String templatePath) {
        this.templatePath = templatePath;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<? extends Object> getTemplatesPath() {
        List<String> templates = Lists.newArrayList("templates/openstack-heat.ftl");
        File[] templateFiles = new File(HeatTemplateBuilderTest.class.getClassLoader().getResource("templates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).map(file -> {
            String[] path = file.getPath().split("/");
            return "templates/" + path[path.length - 1];
        }).collect(Collectors.toList());
        templates.addAll(olderTemplates);
        return templates;
    }

    @Before
    public void setup() throws IOException, TemplateException {
        initMocks(this);
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(heatTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(heatTemplateBuilder, "openStackHeatTemplatePath", templatePath);

        stackName = "testStack";
        groups = new ArrayList<>(1);
        String name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication);
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        Security security = new Security(rules, null);
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50));
        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        Map<String, String> tags = new HashMap<>();
        tags.put(CloudbreakResourceType.DISK.templateVariable(), CloudbreakResourceType.DISK.key());
        tags.put(CloudbreakResourceType.INSTANCE.templateVariable(), CloudbreakResourceType.INSTANCE.key());
        tags.put(CloudbreakResourceType.IP.templateVariable(), CloudbreakResourceType.IP.key());
        tags.put(CloudbreakResourceType.NETWORK.templateVariable(), CloudbreakResourceType.NETWORK.key());
        tags.put(CloudbreakResourceType.SECURITY.templateVariable(), CloudbreakResourceType.SECURITY.key());
        tags.put(CloudbreakResourceType.STORAGE.templateVariable(), CloudbreakResourceType.STORAGE.key());
        tags.put(CloudbreakResourceType.TEMPLATE.templateVariable(), CloudbreakResourceType.TEMPLATE.key());
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(tags);
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "url", "default", null);
    }

    @Test
    public void buildTestWithExistingNetworkAndExistingSubnetAndAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("cb-sec-group_" + 't'));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test
    public void buildTestWithExistingNetworkAndExistingSubnetAndAssignFloatingIpWithExistingSecurityGroups() throws Exception {
        assumeTrue("Template doesn't support this feature, required version is '2.x' at least", isTemplateMajorVersionGreaterOrEqualThan(2));
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        Group group = groups.get(0);
        groups.clear();
        String cloudSecurityId = "sec-group-id";
        Security security = new Security(Collections.emptyList(), cloudSecurityId);
        Group groupWithSecGroup = new Group(group.getName(), InstanceGroupType.CORE, group.getInstances(), security, null,
                group.getInstanceAuthentication(), group.getInstanceAuthentication().getLoginUserName(), group.getInstanceAuthentication().getPublicKey(), 50);
        groups.add(groupWithSecGroup);

        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("cb-sec-group_" + 't')));
        assertThat(templateString, not(containsString("type: OS::Neutron::SecurityGroup")));
        assertThat(templateString, containsString(cloudSecurityId));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test
    public void buildTestWithExistingSubnetAndAssignFloatingIpWithoutExistingNetwork() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("cb-sec-group_" + 't'));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test
    public void buildTestWithExistingNetworkAndAssignFloatingIpWithoutExistingSubnet() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = false;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test
    public void buildTestWithExistingNetworkAndExistingSubnetWithoutAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView(null);
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    public void buildTestWithoutExistingNetworkAndExistingSubnetAndAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = false;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView(null);
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    public void buildTestWithExistingNetworkWithoutExistingSubnetAndAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = false;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView(null);
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    public void buildTestWithExistingSubnetWithoutExistingNetworkAndAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView(null);
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    public void buildTestWithAssignFloatingIpWithoutExistingNetworkAndExistingSubnet() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = false;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + 't'));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test
    @Ignore
    public void buildTestWithExistingNetworkAndExistingSubnetAndAssignFloatingIpShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + 't')));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    @Ignore
    public void buildTestWithExistingSubnetAndAssignFloatingIpWithoutExistingNetworkShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = true;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + 't')));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test
    @Ignore
    public void buildTestWithExistingNetworkAndAssignFloatingIpWithoutExistingSubnetShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = false;
        NeutronNetworkView neutronNetworkView = createNeutronNetworkView("floating_pool_id");
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnet);
        modelContext.withGroups(groups);
        modelContext.withInstanceUserData(image);
        modelContext.withLocation(location());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(heatTemplateBuilder.getTemplate());

        String templateString = heatTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + 't')));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    private NeutronNetworkView createNeutronNetworkView(String publicNetId) {
        Map<String, Object> parameters = new HashMap<>();
        if (publicNetId != null) {
            parameters.put("publicNetId", publicNetId);
        }
        Network network = new Network(null, parameters);
        return new NeutronNetworkView(network);

    }

    private Location location() {
        Region r = Region.region("local");
        return Location.location(r);
    }

    private boolean isTemplateMajorVersionGreaterOrEqualThan(int majorVersion) {
        String[] splittedName = templatePath.split("-");
        String templateMajorVersion = splittedName[splittedName.length - 1].split("\\.")[0];
        if (StringUtils.isNumeric(templateMajorVersion)) {
            return Integer.parseInt(templateMajorVersion) >= majorVersion;
        }
        // template has no version, we assume it is the latest one
        return true;
    }

}