package com.sequenceiq.cloudbreak.cloud.aws;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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


public class CloudFormationTemplateBuilderTest {

    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder = new CloudFormationTemplateBuilder();

    private CloudStack cloudStack;
    private String name;
    private List<Group> groups;
    private CloudInstance instance;
    private List<Volume> volumes;
    private List<SecurityRule> rules;
    private Security security;
    private Map<InstanceGroupType, String> userData;
    private Image image;
    private CloudFormationTemplateBuilder.ModelContext modelContext;
    private String awsCloudFormationTemplatePath;
    private String snapshotId;
    private AuthenticatedContext authenticatedContext;
    private String existingSubnetCidr;

    @Before
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(cloudFormationTemplateBuilder, "freemarkerConfiguration", configuration);

        awsCloudFormationTemplatePath = "templates/aws-cf-stack.ftl";
        snapshotId = "";
        authenticatedContext = authenticatedContext();
        existingSubnetCidr = "testSubnet";

        name = "master";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>());
        instance = new CloudInstance("SOME_ID", instanceTemplate);
        rules = Collections.singletonList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        security = new Security(rules);
        userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        image = new Image("cb-centos66-amb200-2015-05-25", userData);
        groups = new ArrayList<>();
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security));
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        cloudStack = new CloudStack(groups, network, image, parameters);



    }

    @Test
    public void buildTestInstanceGroups() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = true;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("AmbariNodes" + name));
        assertThat(templateString, containsString("AmbariNodeLaunchConfig" + name));
        assertThat(templateString, containsString("ClusterNodeSecurityGroup" + name));

    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = true;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndRoleWithoutInstanceProfile() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = true;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileWithoutRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = true;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchWithoutInstanceProfileAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = true;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndInstanceProfileAndRoleWithoutPublicIpOnLaunch() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWAndRoleWithoutPublicIpOnLaunchAndInstanceProfile() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWAndInstanceProfileWithoutPublicIpOnLaunchAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWWithoutPublicIpOnLaunchAndInstanceProfileAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = true;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndInstanceProfileAndRoleWithoutIGWAndPublicIpOnLaunch() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndRoleWithoutIGWAndPublicIpOnLaunchAndInstanceProfile() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndInstanceProfileWithoutIGWAndPublicIpOnLaunchAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCWithoutIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = true;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithInstanceProfileAndRoleWithoutVPCAndIGWAndPublicIpOnLaunch() throws Exception {
        //GIVEN
        boolean existingVPC = false;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithInstanceProfileWithoutVPCAndIGWAndPublicIpOnLaunchAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = false;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = true;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithRoleWithoutVPCAndIGWAndPublicIpOnLaunchAndInstanceProfile() throws Exception {
        //GIVEN
        boolean existingVPC = false;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = true;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, containsString("RoleName"));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithoutVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() throws Exception {
        //GIVEN
        boolean existingVPC = false;
        boolean existingIGW = false;
        boolean mapPublicIpOnLaunch = false;
        boolean enableInstanceProfile = false;
        boolean s3RoleAvailable = false;
        //WHEN
        modelContext = new CloudFormationTemplateBuilder.ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withSnapshotId(snapshotId)
                .withExistingVpc(existingVPC)
                .withExistingIGW(existingIGW)
                .withExistingSubnetCidr(Lists.newArrayList(existingSubnetCidr))
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .withEnableInstanceProfile(enableInstanceProfile)
                .withS3RoleAvailable(s3RoleAvailable)
                .withTemplatePath(awsCloudFormationTemplatePath);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(templateString, not(containsString("RoleName")));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "owner", "variant", location);
        CloudCredential cc = new CloudCredential(1L, null, null, null);
        return new AuthenticatedContext(cloudContext, cc);
    }

}