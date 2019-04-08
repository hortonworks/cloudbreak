package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
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
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import freemarker.template.Configuration;

@RunWith(Parameterized.class)
public class CloudFormationTemplateBuilderTest {

    private static final String V16 = "1.16";

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String CIDR = "10.0.0.0/16";

    private static final int ROOT_VOLUME_SIZE = 17;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private final CloudFormationTemplateBuilder cloudFormationTemplateBuilder = new CloudFormationTemplateBuilder();

    private CloudStack cloudStack;

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

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        awsCloudFormationTemplate = configuration.getTemplate(templatePath, "UTF-8").toString();
        authenticatedContext = authenticatedContext();
        existingSubnetCidr = "testSubnet";

        InstanceTemplate instanceTemplate = createDefaultInstanceTemplate();
        instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication);
        Security security = getDefaultCloudStackSecurity();
        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, security));

        defaultTags.put(CloudbreakResourceType.DISK.templateVariable(), CloudbreakResourceType.DISK.key());
        defaultTags.put(CloudbreakResourceType.INSTANCE.templateVariable(), CloudbreakResourceType.INSTANCE.key());
        defaultTags.put(CloudbreakResourceType.IP.templateVariable(), CloudbreakResourceType.IP.key());
        defaultTags.put(CloudbreakResourceType.NETWORK.templateVariable(), CloudbreakResourceType.NETWORK.key());
        defaultTags.put(CloudbreakResourceType.SECURITY.templateVariable(), CloudbreakResourceType.SECURITY.key());
        defaultTags.put(CloudbreakResourceType.STORAGE.templateVariable(), CloudbreakResourceType.STORAGE.key());
        defaultTags.put(CloudbreakResourceType.TEMPLATE.templateVariable(), CloudbreakResourceType.TEMPLATE.key());
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
    }

    @Test
    public void buildTestInstanceGroupsAndRootVolumeSize() throws IOException {
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString("AmbariNodesmaster"));
        assertThat(templateString, containsString("AmbariNodeLaunchConfigmaster"));
        assertThat(templateString, containsString("ClusterNodeSecurityGroupmaster"));
        assertThat(templateString, containsString("AmbariNodesgateway"));
        assertThat(templateString, containsString("AmbariNodeLaunchConfiggateway"));
        assertThat(templateString, containsString("ClusterNodeSecurityGroupgateway"));
        assertThat(templateString, not(containsString("testtagkey")));
        assertThat(templateString, not(containsString("testtagvalue")));
        assertThat(templateString, containsString(Integer.toString(ROOT_VOLUME_SIZE)));
        if (!templatePath.contains(V16)) {
            JsonNode jsonNode = JsonUtil.readTree(templateString);
            jsonNode.findValues("Tags").forEach(jsonNode1 -> {
                assertTrue(jsonNode1.findValues("Key").stream().anyMatch(jsonNode2 -> "cb-resource-type".equals(jsonNode2.textValue())));
            });
        }
    }

    @Test
    public void buildTestInstanceGroupsWhenRootVolumeSizeIsSuperLarge() throws IOException {
        //GIVEN
        Integer rootVolumeSize = Integer.MAX_VALUE;
        Security security = getDefaultCloudStackSecurity();
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, rootVolumeSize, security),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, rootVolumeSize, security));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString(Integer.toString(rootVolumeSize)));
        JsonNode firstBlockDeviceMapping = getJsonNode(JsonUtil.readTree(templateString), "BlockDeviceMappings").get(0);
        String volumeSize = getJsonNode(firstBlockDeviceMapping, "VolumeSize").textValue();
        assertEquals(Integer.valueOf(volumeSize), rootVolumeSize);
    }

    @Test
    public void buildTestInstanceGroupsWhenRootVolumeSizeIsSuperSmall() throws IOException {
        //GIVEN
        Integer rootVolumeSize = Integer.MIN_VALUE;
        Security security = getDefaultCloudStackSecurity();
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, rootVolumeSize, security),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, rootVolumeSize, security));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, containsString(Integer.toString(rootVolumeSize)));
        JsonNode firstBlockDeviceMapping = getJsonNode(JsonUtil.readTree(templateString), "BlockDeviceMappings").get(0);
        String volumeSize = getJsonNode(firstBlockDeviceMapping, "VolumeSize").textValue();
        assertEquals(Integer.valueOf(volumeSize), rootVolumeSize);

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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
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
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        }
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndSingleSGAndMultiGroup() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group("gateway", InstanceGroupType.GATEWAY, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        }
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndMultiSG() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), List.of("multi-sg-id1", "multi-sg-id2"));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        // older templates are invalids
        if ("templates/aws-cf-stack.ftl".equals(templatePath)) {
            Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
            // we don't support the multiple security groups in older templates
            assertThat(templateString, containsString("\"multi-sg-id1\",\"multi-sg-id2\""));
        }
        assertThat(templateString, containsString("VPCId"));
    }

    @Test
    public void buildTestInstanceGroupsWithSpotInstances() throws IOException {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = getDefaultCloudStackSecurity();
        groups.add(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security));
        InstanceTemplate spotInstanceTemplate = createDefaultInstanceTemplate();
        spotInstanceTemplate.putParameter("spotPrice", "0.1");
        CloudInstance spotInstance = new CloudInstance("SOME_ID", spotInstanceTemplate, instanceAuthentication);
        groups.add(new Group("compute", InstanceGroupType.CORE, singletonList(spotInstance), security, spotInstance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE));
        groups.add(createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, security));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());

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
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, stringContainsInOrder("SpotPrice", "0.1"));
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential(1L, null);
        return new AuthenticatedContext(cloudContext, credential);
    }

    private CloudStack createDefaultCloudStack(Collection<Group> groups, Map<String, String> parameters, Map<String, String> tags) {
        Network network = new Network(new Subnet("testSubnet"));
        return new CloudStack(groups, network, image, parameters, tags, null, instanceAuthentication,
                instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
    }

    private Group createDefaultGroup(String name, InstanceGroupType type, int rootVolumeSize, Security security) {
        return new Group(name, type, singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), rootVolumeSize);
    }

    private InstanceTemplate createDefaultInstanceTemplate() {
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        return new InstanceTemplate("m1.medium", "master", 0L, volumes, InstanceStatus.CREATE_REQUESTED, new HashMap<>(), 0L,
                "cb-centos66-amb200-2015-05-25");
    }

    private Map<String, String> getDefaultCloudStackParameters() {
        return Map.of("persistentStorage", "persistentStorageTest", "attachedStorageOption", "attachedStorageOptionTest");
    }

    private Map<String, String> getDefaultCloudStackTags() {
        return Map.of("testtagkey", "testtagvalue");
    }

    private Security getDefaultCloudStackSecurity() {
        return new Security(getDefaultSecurityRules(), emptyList());
    }

    private List<SecurityRule> getDefaultSecurityRules() {
        return singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
    }

    private JsonNode getJsonNode(JsonNode node, String value) {
        if (node == null) {
            throw new RuntimeException("No Json node provided for seeking value!");
        }
        return Optional.ofNullable(node.findValue(value)).orElseThrow(() -> new RuntimeException("No value find in json with the name of: \"" + value + "\""));
    }

}
