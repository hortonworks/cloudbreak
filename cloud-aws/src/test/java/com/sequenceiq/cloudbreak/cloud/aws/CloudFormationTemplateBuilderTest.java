package com.sequenceiq.cloudbreak.cloud.aws;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
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
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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
import com.sequenceiq.cloudbreak.util.JsonUtil;

import freemarker.template.Configuration;

@RunWith(Parameterized.class)
public class CloudFormationTemplateBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH = "templates/aws-cf-stack.ftl";

    private static final String CIDR = "10.0.0.0/16";

    private static final int ROOT_VOLUME_SIZE = 17;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @InjectMocks
    private final CloudFormationTemplateBuilder cloudFormationTemplateBuilder = new CloudFormationTemplateBuilder();

    private CloudStack cloudStack;

    private String name;

    private ModelContext modelContext;

    private String awsCloudFormationTemplate;

    private AuthenticatedContext authenticatedContext;

    private String existingSubnetCidr;

    private final String templatePath;

    private final Map<String, String> defaultTags = new HashMap<>();

    private Image image;

    private InstanceAuthentication instanceAuthentication;

    private CloudInstance instance;

    public CloudFormationTemplateBuilderTest(String templatePath) {
        this.templatePath = templatePath;
    }

    @Parameters(name = "{0}")
    public static Iterable<?> getTemplatesPath() {
        List<String> templates = Lists.newArrayList(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH);
        File[] templateFiles = new File(CloudFormationTemplateBuilderTest.class.getClassLoader().getResource("templates").getPath()).listFiles();
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
        ReflectionTestUtils.setField(cloudFormationTemplateBuilder, "freemarkerConfiguration", configuration);

        awsCloudFormationTemplate = configuration.getTemplate(templatePath, "UTF-8").toString();
        authenticatedContext = authenticatedContext();
        existingSubnetCidr = "testSubnet";

        name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25");
        instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication);
        List<SecurityRule> rules = singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        Security security = new Security(rules, emptyList());
        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(name, InstanceGroupType.CORE, singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.GATEWAY, singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Network network = new Network(new Subnet("testSubnet"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("persistentStorage", "persistentStorageTest");
        parameters.put("attachedStorageOption", "attachedStorageOptionTest");
        Map<String, String> tags = new HashMap<>();
        tags.put("testtagkey", "testtagvalue");

        defaultTags.put(CloudbreakResourceType.DISK.templateVariable(), CloudbreakResourceType.DISK.key());
        defaultTags.put(CloudbreakResourceType.INSTANCE.templateVariable(), CloudbreakResourceType.INSTANCE.key());
        defaultTags.put(CloudbreakResourceType.IP.templateVariable(), CloudbreakResourceType.IP.key());
        defaultTags.put(CloudbreakResourceType.NETWORK.templateVariable(), CloudbreakResourceType.NETWORK.key());
        defaultTags.put(CloudbreakResourceType.SECURITY.templateVariable(), CloudbreakResourceType.SECURITY.key());
        defaultTags.put(CloudbreakResourceType.STORAGE.templateVariable(), CloudbreakResourceType.STORAGE.key());
        defaultTags.put(CloudbreakResourceType.TEMPLATE.templateVariable(), CloudbreakResourceType.TEMPLATE.key());
        cloudStack = new CloudStack(groups, network, image, parameters, tags, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
    }

    @Test
    public void buildTestInstanceGroupsAndRootVolumeSize() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("AmbariNodes" + name));
        assertThat(templateString, containsString("AmbariNodeLaunchConfig" + name));
        assertThat(templateString, containsString("ClusterNodeSecurityGroup" + name));
        assertThat(templateString, not(containsString("testtagkey")));
        assertThat(templateString, not(containsString("testtagvalue")));
        assertThat(templateString, containsString(Integer.toString(ROOT_VOLUME_SIZE)));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndRoleWithoutInstanceProfile() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileWithoutRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchWithoutInstanceProfileAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, not(containsString("InstanceProfile")));
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
    public void buildTestWithVPCAndIGWAndInstanceProfileAndRoleWithoutPublicIpOnLaunch() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWAndRoleWithoutPublicIpOnLaunchAndInstanceProfile() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWAndInstanceProfileWithoutPublicIpOnLaunchAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndIGWWithoutPublicIpOnLaunchAndInstanceProfileAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, not(containsString("InstanceProfile")));
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
    public void buildTestWithVPCAndInstanceProfileAndRoleWithoutIGWAndPublicIpOnLaunch() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndRoleWithoutIGWAndPublicIpOnLaunchAndInstanceProfile() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCAndInstanceProfileWithoutIGWAndPublicIpOnLaunchAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithVPCWithoutIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, not(containsString("InstanceProfile")));
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
    public void buildTestWithInstanceProfileAndRoleWithoutVPCAndIGWAndPublicIpOnLaunch() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithInstanceProfileWithoutVPCAndIGWAndPublicIpOnLaunchAndRole() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithRoleWithoutVPCAndIGWAndPublicIpOnLaunchAndInstanceProfile() {
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("InstanceProfile"));
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
    public void buildTestWithoutVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() {
        //GIVEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, not(containsString("InstanceProfile")));
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
    public void buildTestWithVPCAndIGWAndSingleSG() {
        //GIVEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group(name, InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        }
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndSingleSGAndMultiGroup() {
        //GIVEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group(name, InstanceGroupType.GATEWAY, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        groups.add(new Group(name, InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
        }
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndMultiSG() {
        //GIVEN
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), List.of("multi-sg-id1", "multi-sg-id2"));
        groups.add(new Group(name, InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withTemplate(awsCloudFormationTemplate);
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);

        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Ivalid JSON: " + templateString, JsonUtil.isValid(templateString));
            // we don't support the multiple security groups in older templates
            assertThat(templateString, containsString("\"multi-sg-id1\",\"multi-sg-id2\""));
        }
        assertThat(templateString, containsString("VPCId"));
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential(1L, null);
        return new AuthenticatedContext(cloudContext, credential);
    }
}