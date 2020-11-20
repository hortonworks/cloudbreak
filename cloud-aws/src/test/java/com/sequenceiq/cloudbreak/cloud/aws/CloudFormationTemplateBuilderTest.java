package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsTargetGroup;
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
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.model.CloudIdentityType;

import freemarker.template.Configuration;

@ExtendWith(MockitoExtension.class)
public class CloudFormationTemplateBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String CIDR = "10.0.0.0/16";

    private static final int ROOT_VOLUME_SIZE = 17;

    private static final String INSTANCE_PROFILE = "alma";

    @Mock
    private CostTagging costTagging;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private final CloudFormationTemplateBuilder cloudFormationTemplateBuilder = new CloudFormationTemplateBuilder();

    private CloudStack cloudStack;

    private ModelContext modelContext;

    private String awsCloudFormationTemplate;

    private AuthenticatedContext authenticatedContext;

    private String existingSubnetCidr;

    private final Map<String, String> defaultTags = new HashMap<>();

    private Image image;

    private InstanceAuthentication instanceAuthentication;

    private CloudInstance instance;

    @BeforeEach
    public void setUp() throws Exception {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(cloudFormationTemplateBuilder, "freemarkerConfiguration", configuration);

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        awsCloudFormationTemplate = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
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
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security, Optional.empty()),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, security, Optional.empty()));
        cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
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
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .contains("AmbariNodesmaster")
                .matches(template -> template.contains("AmbariNodeLaunchConfigmaster") || template.contains("ClusterManagerNodeLaunchTemplatemaster"))
                .contains("ClusterNodeSecurityGroupmaster")
                .contains("SecurityGroupIngress")
                .contains("AmbariNodesgateway")
                .matches(template -> template.contains("AmbariNodeLaunchConfiggateway") || template.contains("ClusterManagerNodeLaunchTemplategateway"))
                .contains("ClusterNodeSecurityGroupgateway")
                .doesNotContain("testtagkey")
                .doesNotContain("testtagkey")
                .contains(Integer.toString(ROOT_VOLUME_SIZE));
    }

    @Test
    public void buildTestInstanceGroupsWhenRootVolumeSizeIsSuperLarge() throws IOException {
        //GIVEN
        int rootVolumeSize = Integer.MAX_VALUE;
        Security security = getDefaultCloudStackSecurity();
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, rootVolumeSize, security, Optional.empty()),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, rootVolumeSize, security, Optional.empty()));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString(Integer.toString(rootVolumeSize)));
        JsonNode firstBlockDeviceMapping = getJsonNode(JsonUtil.readTree(templateString), "BlockDeviceMappings").get(0);
        String volumeSize = getJsonNode(firstBlockDeviceMapping, "VolumeSize").textValue();
        Assertions.assertThat(Integer.valueOf(volumeSize)).isEqualTo(rootVolumeSize);
    }

    @Test
    public void buildTestInstanceGroupsWhenRootVolumeSizeIsSuperSmall() throws IOException {
        //GIVEN
        int rootVolumeSize = Integer.MIN_VALUE;
        Security security = getDefaultCloudStackSecurity();
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, rootVolumeSize, security, Optional.empty()),
                createDefaultGroup("gateway", InstanceGroupType.GATEWAY, rootVolumeSize, security, Optional.empty()));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString(Integer.toString(rootVolumeSize)));
        JsonNode firstBlockDeviceMapping = getJsonNode(JsonUtil.readTree(templateString), "BlockDeviceMappings").get(0);
        String volumeSize = getJsonNode(firstBlockDeviceMapping, "VolumeSize").textValue();
        Assertions.assertThat(Integer.valueOf(volumeSize)).isEqualTo(rootVolumeSize);
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileAndRole() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndRoleWithoutInstanceProfile() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndPublicIpOnLaunchAndInstanceProfileWithoutRole() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
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
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, not(containsString("InstanceProfile")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, containsString("EIP"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndInstanceProfileAndRoleWithoutPublicIpOnLaunch() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWAndVpcSubnets() {
        String vpcSubnet = "10.0.0.0/24";
        List<String> vpcSubnets = List.of(vpcSubnet);
        Security security = new Security(getDefaultSecurityRules(), List.of(), true);
        CloudS3View logView = new CloudS3View(CloudIdentityType.LOG);
        logView.setInstanceProfile(INSTANCE_PROFILE);
        List<Group> groups = List.of(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security, Optional.of(logView)));
        cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(vpcSubnets)
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
        assertThat(templateString, containsString("{ \"IpProtocol\" : \"icmp\", \"FromPort\" : \"-1\", \"ToPort\" : \"-1\", \"CidrIp\" : "
                + "\"10.0.0.0/24\"} ,{ \"IpProtocol\" : \"tcp\", \"FromPort\" : \"0\", \"ToPort\" : \"65535\", \"CidrIp\" : \"10.0.0.0/24\"} ,{ \"IpProtocol\" "
                + ": \"udp\", \"FromPort\" : \"0\", \"ToPort\" : \"65535\", \"CidrIp\" : \"10.0.0.0/24\"}"));
    }

    @Test
    public void buildTestWithVPCAndIGWAndRoleWithoutPublicIpOnLaunchAndInstanceProfile() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWAndInstanceProfileWithoutPublicIpOnLaunchAndRole() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("SecurityGroupIngress"));
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
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, not(containsString("InstanceProfile")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndInstanceProfileAndRoleWithoutIGWAndPublicIpOnLaunch() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndRoleWithoutIGWAndPublicIpOnLaunchAndInstanceProfile() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndInstanceProfileWithoutIGWAndPublicIpOnLaunchAndRole() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
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
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, not(containsString("InstanceProfile")));
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, not(containsString("SubnetConfig")));
        assertThat(templateString, not(containsString("\"AttachGateway\"")));
        assertThat(templateString, not(containsString("\"InternetGateway\"")));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithInstanceProfileAndRoleWithoutVPCAndIGWAndPublicIpOnLaunch() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithInstanceProfileWithoutVPCAndIGWAndPublicIpOnLaunchAndRole() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithRoleWithoutVPCAndIGWAndPublicIpOnLaunchAndInstanceProfile() {
        CloudStack cloudStack = initCloudStackWithInstanceProfile();
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(false)
                .withExistingIGW(false)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("InstanceProfile"));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
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
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(false)
                .withEnableInstanceProfile(false)
                .withInstanceProfileAvailable(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, not(containsString("InstanceProfile")));
        assertThat(templateString, not(containsString("VPCId")));
        assertThat(templateString, not(containsString("SubnetCIDR")));
        assertThat(templateString, containsString("SubnetId"));
        assertThat(templateString, containsString("SubnetConfig"));
        assertThat(templateString, containsString("\"AttachGateway\""));
        assertThat(templateString, containsString("\"InternetGateway\""));
        assertThat(templateString, containsString("AvailabilitySet"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("EIP")));
    }

    @Test
    public void buildTestWithVPCAndIGWAndSingleSG() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SecurityGroupIngress")));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndSingleSGAndMultiGroup() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), singletonList("single-sg-id"));
        groups.add(new Group("gateway", InstanceGroupType.GATEWAY, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("VPCId"));
        assertThat(templateString, not(containsString("SecurityGroupIngress")));
        assertThat(templateString, containsString("\"single-sg-id\""));
    }

    @Test
    public void buildTestWithVPCAndIGWAndMultiSG() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = new Security(emptyList(), List.of("multi-sg-id1", "multi-sg-id2"));
        groups.add(new Group("master", InstanceGroupType.CORE, emptyList(), security, instance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        CloudStack cloudStack = new CloudStack(groups, new Network(new Subnet(CIDR)), image, emptyMap(), emptyMap(), "template",
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publicKey", null);
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .withStack(cloudStack)
                .mapPublicIpOnLaunch(false)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("\"multi-sg-id1\",\"multi-sg-id2\""));
        assertThat(templateString, not(containsString("SecurityGroupIngress")));
        assertThat(templateString, containsString("VPCId"));
    }

    @Test
    public void buildTestInstanceGroupsWithSpotInstances() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = getDefaultCloudStackSecurity();
        groups.add(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security, Optional.empty()));
        InstanceTemplate spotInstanceTemplate = createDefaultInstanceTemplate();
        spotInstanceTemplate.putParameter(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 60);
        CloudInstance spotInstance = new CloudInstance("SOME_ID", spotInstanceTemplate, instanceAuthentication);
        groups.add(new Group("compute", InstanceGroupType.CORE, singletonList(spotInstance), security, spotInstance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        groups.add(createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, security, Optional.empty()));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, stringContainsInOrder("OnDemandPercentageAboveBaseCapacity", "40"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("SpotMaxPrice")));
    }

    @Test
    public void buildTestInstanceGroupsWithSpotInstancesWithMaxPrice() {
        //GIVEN
        List<Group> groups = new ArrayList<>();
        Security security = getDefaultCloudStackSecurity();
        groups.add(createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, security, Optional.empty()));
        InstanceTemplate spotInstanceTemplate = createDefaultInstanceTemplate();
        spotInstanceTemplate.putParameter(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 60);
        spotInstanceTemplate.putParameter(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, 0.9);
        CloudInstance spotInstance = new CloudInstance("SOME_ID", spotInstanceTemplate, instanceAuthentication);
        groups.add(new Group("compute", InstanceGroupType.CORE, singletonList(spotInstance), security, spotInstance,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), "publickey", ROOT_VOLUME_SIZE, Optional.empty()));
        groups.add(createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, security, Optional.empty()));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        assertThat(templateString, stringContainsInOrder("OnDemandPercentageAboveBaseCapacity", "40"));
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, stringContainsInOrder("SpotMaxPrice", "0.9"));
    }

    @Test
    public void buildTestOutboundInternetTrafficButVpcCidrs() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withVpcCidrs(List.of("vpccidr1", "vpccidr2"))
                .withPrefixListIds(List.of())
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("SecurityGroupEgress")));
    }

    @Test
    public void buildTestOutboundInternetTrafficButPrefixlists() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withVpcCidrs(List.of())
                .withPrefixListIds(List.of("prefix1", "prefix2"))
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("SecurityGroupEgress")));
    }

    @Test
    public void buildTestNoOutboundInternetTrafficJustVpcCidrs() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.DISABLED)
                .withVpcCidrs(List.of("vpccidr1", "vpccidr2"))
                .withPrefixListIds(List.of())
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, stringContainsInOrder("SecurityGroupEgress", "vpccidr1", "vpccidr2"));
    }

    @Test
    public void buildTestNoOutboundInternetTrafficJustPrefixLists() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.DISABLED)
                .withVpcCidrs(List.of())
                .withPrefixListIds(List.of("prefix1", "prefix2"))
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, stringContainsInOrder("SecurityGroupEgress", "prefix1", "prefix2"));
    }

    @Test
    public void buildTestNoOutboundInternetTrafficBothVpcCidrsAndPrefixListsAreGiven() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.DISABLED)
                .withVpcCidrs(List.of("vpccidr1", "vpccidr2"))
                .withPrefixListIds(List.of("prefix1", "prefix2"))
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, stringContainsInOrder("SecurityGroupEgress", "vpccidr1", "vpccidr2", "prefix1", "prefix2"));
    }

    @Test
    public void buildTestNoOutboundInternetTrafficButVpcCidrsAndPrefixListsAreEmpty() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.DISABLED)
                .withVpcCidrs(List.of())
                .withPrefixListIds(List.of())
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString)).overridingErrorMessage("Invalid JSON: " + templateString).isTrue();
        assertThat(templateString, containsString("SecurityGroupIngress"));
        assertThat(templateString, not(containsString("SecurityGroupEgress")));
    }

    @Test
    public void buildTestNoEbsEncryption() {
        //GIVEN
        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .doesNotContain("\"Encrypted\"")
                .contains("SecurityGroupIngress")
                .contains("{ \"Ref\" : \"AMI\" }");
    }

    @Test
    public void buildTestEbsEncryptionWithDefaultKey() {
        //GIVEN
        instance.getTemplate().putParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate)
                .withEncryptedAMIByGroupName(Map.ofEntries(entry("master", "masterAMI"), entry("gateway", "gatewayAMI")));
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .doesNotContain("\"Encrypted\"")
                .contains("\"masterAMI\"")
                .contains("\"gatewayAMI\"")
                .contains("SecurityGroupIngress")
                .doesNotContain("{ \"Ref\" : \"AMI\" }");
    }

    @Test
    public void buildTestEbsEncryptionWithCustomKey() {
        //GIVEN
        instance.getTemplate().putParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "customEncryptionKeyArn");

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate)
                .withEncryptedAMIByGroupName(Map.ofEntries(entry("master", "masterAMI"), entry("gateway", "gatewayAMI")));
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .doesNotContain("\"Encrypted\"")
                .contains("\"masterAMI\"")
                .contains("\"gatewayAMI\"")
                .contains("SecurityGroupIngress")
                .doesNotContain("{ \"Ref\" : \"AMI\" }");
    }

    @Test
    public void buildTestEbsEncryptionWithDefaultKeyAndFastEncryption() {
        //GIVEN
        instance.getTemplate().putParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .contains("\"Encrypted\"")
                .contains("{ \"Ref\" : \"AMI\" }")
                .contains("SecurityGroupIngress")
                .doesNotContain("\"KmsKeyId\"");
    }

    @Test
    public void buildTestEbsEncryptionWithCustomKeyAndFastEncryption() {
        //GIVEN
        instance.getTemplate().putParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED, true);
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        instance.getTemplate().putParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "customEncryptionKeyArn");

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(templateString)
                .matches(JsonUtil::isValid, "Invalid JSON: " + templateString)
                .contains("\"Encrypted\"")
                .contains("{ \"Ref\" : \"AMI\" }")
                .contains("SecurityGroupIngress")
                .contains("\"KmsKeyId\" : \"customEncryptionKeyArn\"");
    }

    @Test
    public void buildTestPlacementGroupWithMixedPlacementGroup() {
        //GIVEN
        InstanceTemplate instanceTemplateMaster = createDefaultInstanceTemplate();
        InstanceTemplate instanceTemplateWorker = createDefaultInstanceTemplate();
        InstanceTemplate instanceTemplateGateway = createDefaultInstanceTemplate();
        InstanceTemplate instanceTemplateCustom = createDefaultInstanceTemplate();

        instanceTemplateMaster.putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.SPREAD.name());
        instanceTemplateWorker.putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.PARTITION.name());
        instanceTemplateGateway.putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.CLUSTER.name());
        instanceTemplateCustom.putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.NONE.name());

        List<Group> groups = List.of(createDefaultGroupWithInstanceTemplate("master", instanceTemplateMaster, InstanceGroupType.CORE),
                createDefaultGroupWithInstanceTemplate("worker", instanceTemplateWorker, InstanceGroupType.CORE),
                createDefaultGroupWithInstanceTemplate("gateway", instanceTemplateGateway, InstanceGroupType.GATEWAY),
                createDefaultGroupWithInstanceTemplate("custom", instanceTemplateCustom, InstanceGroupType.CORE));
        CloudStack cloudStack = createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString))
                .overridingErrorMessage("Invalid JSON: " + templateString)
                .isTrue();

        Assertions.assertThat(templateString)
                .contains("\"PlacementGroupmaster\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"spread\"}}")
                .contains("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupmaster\" } }")
                .contains("\"PlacementGroupworker\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"partition\"}}")
                .contains("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupworker\" } }")
                .contains("\"PlacementGroupgateway\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"cluster\"}}")
                .contains("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupgateway\" } }")
                .doesNotContain("\"PlacementGroupcustom\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"cluster\"}}")
                .doesNotContain("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupcustom\" } }");
    }

    @Test
    public void buildTestPlacementGroupWithNonePlacementGroup() {
        //GIVEN
        instance.getTemplate().putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.NONE.name());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString))
                .overridingErrorMessage("Invalid JSON: " + templateString)
                .isTrue();

        Assertions.assertThat(templateString)
                .doesNotContain("PlacementGroup")
                .doesNotContain("Placement");
    }

    @Test
    public void buildTestPlacementGroupWithPlacementGroupNotSpecified() {
        //GIVEN

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString))
                .overridingErrorMessage("Invalid JSON: " + templateString)
                .isTrue();

        Assertions.assertThat(templateString)
                .doesNotContain("PlacementGroup")
                .doesNotContain("Placement");
    }

    @Test
    public void buildTestPlacementGroupWithPartitionPlacementGroup() {
        //GIVEN
        instance.getTemplate().putParameter(PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.PARTITION.name());

        //WHEN
        modelContext = new ModelContext()
                .withAuthenticatedContext(authenticatedContext)
                .withStack(cloudStack)
                .withExistingVpc(true)
                .withExistingIGW(true)
                .withExistingSubnetCidr(singletonList(existingSubnetCidr))
                .withExistinVpcCidr(List.of(existingSubnetCidr))
                .mapPublicIpOnLaunch(true)
                .withEnableInstanceProfile(true)
                .withInstanceProfileAvailable(true)
                .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assertions.assertThat(JsonUtil.isValid(templateString))
                .overridingErrorMessage("Invalid JSON: " + templateString)
                .isTrue();

        Assertions.assertThat(templateString)
                .contains("\"PlacementGroupmaster\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"partition\"}}")
                .contains("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupmaster\" } }")
                .contains("\"PlacementGroupgateway\" : {\"Type\" : \"AWS::EC2::PlacementGroup\",\"Properties\" : {\"Strategy\" : \"partition\"}}")
                .contains("\"Placement\" : { \"GroupName\" : { \"Ref\" : \"PlacementGroupgateway\" } }");
    }

    @Test
    public void buildTestWithSingleLoadBalancerBeforeUpdate() {
        //GIVEN
        AwsLoadBalancer awsLoadBalancer = setupLoadBalancer(AwsLoadBalancerScheme.PRIVATE, 443, false);

        //WHEN
        modelContext = new ModelContext()
            .withAuthenticatedContext(authenticatedContext)
            .withStack(cloudStack)
            .withExistingVpc(true)
            .withExistingIGW(true)
            .withExistingSubnetCidr(singletonList(existingSubnetCidr))
            .withExistinVpcCidr(List.of(existingSubnetCidr))
            .mapPublicIpOnLaunch(true)
            .withEnableInstanceProfile(true)
            .withInstanceProfileAvailable(true)
            .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
            .withTemplate(awsCloudFormationTemplate)
            .withLoadBalancers(List.of(awsLoadBalancer));
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(templateString)
            .contains("\"LoadBalancerPrivate\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::LoadBalancer\"")
            .contains("\"Scheme\" : \"internal\"")
            .contains("\"TargetGroupPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::TargetGroup\"")
            .contains("\"Targets\" : [{ \"Id\" : \"instance1-443\" },{ \"Id\" : \"instance2-443\" }]}}")
            .doesNotContain("\"ListenerPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::Listener\"");
    }

    @Test
    public void buildTestWithSingleLoadBalancerAfterUpdate() {
        //GIVEN
        AwsLoadBalancer awsLoadBalancer = setupLoadBalancer(AwsLoadBalancerScheme.PRIVATE, 443, true);

        //WHEN
        modelContext = new ModelContext()
            .withAuthenticatedContext(authenticatedContext)
            .withStack(cloudStack)
            .withExistingVpc(true)
            .withExistingIGW(true)
            .withExistingSubnetCidr(singletonList(existingSubnetCidr))
            .withExistinVpcCidr(List.of(existingSubnetCidr))
            .mapPublicIpOnLaunch(true)
            .withEnableInstanceProfile(true)
            .withInstanceProfileAvailable(true)
            .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
            .withTemplate(awsCloudFormationTemplate)
            .withLoadBalancers(List.of(awsLoadBalancer));
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(templateString)
            .contains("\"LoadBalancerPrivate\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::LoadBalancer\"")
            .contains("\"Scheme\" : \"internal\"")
            .contains("\"TargetGroupPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::TargetGroup\"")
            .contains("\"Targets\" : [{ \"Id\" : \"instance1-443\" },{ \"Id\" : \"instance2-443\" }]}}")
            .contains("\"ListenerPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::Listener\"");
    }

    @Test
    public void buildTestWithMultipleLoadBalancers() {
        //GIVEN
        List<AwsLoadBalancer> awsLoadBalancers = List.of(
            setupLoadBalancer(AwsLoadBalancerScheme.PRIVATE, 443, true),
            setupLoadBalancer(AwsLoadBalancerScheme.PUBLIC, 888, true)
        );

        //WHEN
        modelContext = new ModelContext()
            .withAuthenticatedContext(authenticatedContext)
            .withStack(cloudStack)
            .withExistingVpc(true)
            .withExistingIGW(true)
            .withExistingSubnetCidr(singletonList(existingSubnetCidr))
            .withExistinVpcCidr(List.of(existingSubnetCidr))
            .mapPublicIpOnLaunch(true)
            .withEnableInstanceProfile(true)
            .withInstanceProfileAvailable(true)
            .withOutboundInternetTraffic(OutboundInternetTraffic.ENABLED)
            .withTemplate(awsCloudFormationTemplate)
            .withLoadBalancers(awsLoadBalancers);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);

        //THEN
        Assertions.assertThat(templateString)
            .contains("\"LoadBalancerPrivate\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::LoadBalancer\"")
            .contains("\"Scheme\" : \"internal\"")
            .contains("\"TargetGroupPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::TargetGroup\"")
            .contains("\"Targets\" : [{ \"Id\" : \"instance1-443\" },{ \"Id\" : \"instance2-443\" }]}}")
            .contains("\"ListenerPort443Private\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::Listener\"")
            .contains("\"LoadBalancerPublic\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::LoadBalancer\"")
            .contains("\"Scheme\" : \"internet-facing\"")
            .contains("\"TargetGroupPort888Public\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::TargetGroup\"")
            .contains("\"Targets\" : [{ \"Id\" : \"instance1-888\" },{ \"Id\" : \"instance2-888\" }]}}")
            .contains("\"ListenerPort888Public\" : {\"Type\" : \"AWS::ElasticLoadBalancingV2::Listener\"");
    }

    private AwsLoadBalancer setupLoadBalancer(AwsLoadBalancerScheme scheme, int port, boolean setArn) {
        AwsTargetGroup targetGroup = new AwsTargetGroup(port, scheme, 1, List.of("instance1-" + port, "instance2-" + port));
        AwsListener listener = new AwsListener(port, List.of(targetGroup), scheme);
        AwsLoadBalancer loadBalancer = new AwsLoadBalancer(scheme, List.of(listener));
        if (setArn) {
            targetGroup.setArn("arn://targetgroup");
            loadBalancer.setArn("arn://loadbalancer");
            loadBalancer.canCreateListeners();
        }
        return loadBalancer;
    }

    private CloudStack initCloudStackWithInstanceProfile() {
        CloudS3View logView = new CloudS3View(CloudIdentityType.LOG);
        logView.setInstanceProfile(INSTANCE_PROFILE);
        List<Group> groups = List.of(createDefaultGroupGatewayGroup(Optional.of(logView)),
                createDefaultGroupMasterGroup(Optional.of(logView)));
        return createDefaultCloudStack(groups, getDefaultCloudStackParameters(), getDefaultCloudStackTags());
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential("crn", null);
        return new AuthenticatedContext(cloudContext, credential);
    }

    private CloudStack createDefaultCloudStack(Collection<Group> groups, Map<String, String> parameters, Map<String, String> tags) {
        Network network = new Network(new Subnet("testSubnet"));
        return new CloudStack(groups, network, image, parameters, tags, null, instanceAuthentication,
                instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
    }

    private Group createDefaultGroupMasterGroup(Optional<CloudFileSystemView> cloudFileSystemView) {
        return createDefaultGroup("master", InstanceGroupType.CORE, ROOT_VOLUME_SIZE, getDefaultCloudStackSecurity(), cloudFileSystemView);
    }

    private Group createDefaultGroupGatewayGroup(Optional<CloudFileSystemView> cloudFileSystemView) {
        return createDefaultGroup("gateway", InstanceGroupType.GATEWAY, ROOT_VOLUME_SIZE, getDefaultCloudStackSecurity(), cloudFileSystemView);
    }

    private Group createDefaultGroupWithInstanceTemplate(String name, InstanceTemplate instanceTemplate, InstanceGroupType instanceGroupType) {
        Security security = getDefaultCloudStackSecurity();
        CloudInstance cloudInstance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication);
        return new Group(name, instanceGroupType, singletonList(cloudInstance), security, null,
        instanceAuthentication, instanceAuthentication.getLoginUserName(),
        instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty());
    }

    private Group createDefaultGroup(String name, InstanceGroupType type, int rootVolumeSize, Security security,
            Optional<CloudFileSystemView> cloudFileSystemView) {
        return new Group(name, type, singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), rootVolumeSize, cloudFileSystemView);
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
