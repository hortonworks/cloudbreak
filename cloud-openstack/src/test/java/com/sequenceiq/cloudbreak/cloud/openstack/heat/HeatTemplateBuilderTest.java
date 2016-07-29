package com.sequenceiq.cloudbreak.cloud.openstack.heat;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class HeatTemplateBuilderTest {

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private OpenStackUtils openStackUtil;

    @InjectMocks
    private HeatTemplateBuilder heatTemplateBuilder = new HeatTemplateBuilder();

    private String stackName;
    private List<Group> groups;
    private String name;
    private List<Volume> volumes;
    private CloudInstance instance;
    private List<SecurityRule> rules;
    private Security security;
    private Map<InstanceGroupType, String> userData;
    private Image image;

    @Before
    public void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(heatTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(heatTemplateBuilder, "openStackHeatTemplatePath", "templates/openstack-heat.ftl");

        stackName = "testStack";
        groups = new ArrayList<>();
        name = "master";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>());
        instance = new CloudInstance("SOME_ID", instanceTemplate);
        rules = Collections.singletonList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        security = new Security(rules);
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        image = new Image("cb-centos66-amb200-2015-05-25", userData);
    }

    @Test
    public void buildTestWithExistingNetworkAndExistingSubnetAndAssignFloatingIp() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = false;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = false;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = false;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = false;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
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
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, containsString("name: cb-sec-group_" + "t"));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, containsString("subnet_id"));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, containsString("network_id"));
        assertThat(templateString, containsString("public_net_id"));
    }

    @Test(expected = AssertionError.class)
    public void buildTestWithExistingNetworkAndExistingSubnetAndAssignFloatingIpShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = true;
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + "t")));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test(expected = AssertionError.class)
    public void buildTestWithExistingSubnetAndAssignFloatingIpWithoutExistingNetworkShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = false;
        boolean existingSubnet = true;
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + "t")));
        assertThat(templateString, containsString("app_net_id"));
        assertThat(templateString, not(containsString("app_network")));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, containsString("app_subnet"));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }

    @Test(expected = AssertionError.class)
    public void buildTestWithExistingNetworkAndAssignFloatingIpWithoutExistingSubnetShouldThrowAssertionException() throws Exception {
        //GIVEN
        boolean existingNetwork = true;
        boolean existingSubnet = false;
        boolean assignFloatingIp = true;
        //WHEN
        when(openStackUtil.adjustStackNameLength(Mockito.anyString())).thenReturn("t");
        String templateString = heatTemplateBuilder.build(stackName, groups, image, existingNetwork, existingSubnet, assignFloatingIp);
        //THEN
        assertThat(templateString, not(containsString("name: cb-sec-group_" + "t")));
        assertThat(templateString, not(containsString("app_net_id")));
        assertThat(templateString, containsString("app_network"));
        assertThat(templateString, not(containsString("subnet_id")));
        assertThat(templateString, not(containsString("app_subnet")));
        assertThat(templateString, not(containsString("network_id")));
        assertThat(templateString, not(containsString("public_net_id")));
    }
}