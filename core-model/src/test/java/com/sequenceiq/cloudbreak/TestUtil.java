package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.dto.credential.aws.AwsCredentialAttributes;
import com.sequenceiq.cloudbreak.dto.credential.azure.AzureCredentialAttributes;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AwsDiskType;

public class TestUtil {

    public static final String STACK_CRN = "crn:cdp:cloudbreak:us-west-1:someone:stack:12345";

    private static final String DUMMY_DESCRIPTION = "dummyDescription";

    private static final String DUMMY_SECURITY_GROUP_ID = "dummySecurityGroupId";

    private static final String N2_HIGHCPU_16_INSTANCE = "n2-highcpu-16";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    private static final String DUMMY_NAME = "dummyName";

    private static final AtomicLong UNIQUE_ID = new AtomicLong(0L);

    private TestUtil() {
    }

    public static Path getFilePath(Class<?> clazz, String fileName) {
        try {
            URL resource = clazz.getResource(fileName);
            return Paths.get(resource.toURI());
        } catch (Exception ex) {
            LOGGER.error("{}: {}", ex.getMessage(), ex);
            return null;
        }
    }

    public static CloudbreakUser cbAdminUser() {
        return new CloudbreakUser("userid", "testCrn", "testuser", "email", "testaccount");
    }

    public static CloudbreakUser cbUser() {
        return new CloudbreakUser("userid", "testCrn", "testuser", "email", "testaccount");
    }

    public static Credential awsCredential() {
        return Credential.builder()
                .aws(AwsCredentialAttributes.builder().build())
                .description(DUMMY_DESCRIPTION)
                .name(DUMMY_NAME)
                .crn("credCrn")
                .cloudPlatform(CloudPlatform.AWS.name())
                .build();
    }

    public static Credential gcpCredential() {
        return Credential.builder()
                .azure(AzureCredentialAttributes.builder().build())
                .description(DUMMY_DESCRIPTION)
                .name(DUMMY_NAME)
                .cloudPlatform(CloudPlatform.GCP.name())
                .build();
    }

    public static Credential azureCredential() {
        return Credential.builder()
                .azure(AzureCredentialAttributes.builder().build())
                .description(DUMMY_DESCRIPTION)
                .name(DUMMY_NAME)
                .cloudPlatform(CloudPlatform.AZURE.name())
                .build();
    }

    public static Stack setEphemeral(Stack stack) {
        if (stack.cloudPlatform().equals(AWS)) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                (instanceGroup.getTemplate()).getVolumeTemplates().iterator().next().setVolumeType(AwsDiskType.Ephemeral.value());
            }
        }
        return stack;
    }

    public static StackView stackView(Status stackStatus, Credential credential) {
        return new StackView(1L, "simplestack", credential.cloudPlatform(), new StackStatusView());
    }

    public static Stack stack(Status stackStatus, Credential credential) {
        User user = new User();
        user.setUserId("horton@hortonworks.com");
        user.setUserCrn("testCrn");
        user.setUserName("Alma ur");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        Tenant tenant = new Tenant();
        tenant.setName("testtenant");
        workspace.setTenant(tenant);
        Stack stack = new Stack();
        stack.setCreator(user);
        stack.setWorkspace(workspace);
        stack.setStackStatus(new StackStatus(stack, stackStatus, "statusReason", DetailedStackStatus.UNKNOWN));
        stack.setName("simplestack");
        stack.setId(1L);
        stack.setInstanceGroups(generateGcpInstanceGroups(3));
        stack.setRegion("region");
        stack.setCreated(123L);
        stack.setCloudPlatform(credential.cloudPlatform());
        stack.setOrchestrator(orchestrator());
        stack.setEnvironmentCrn("envCrn");
        stack.setResourceCrn(STACK_CRN);
        stack.setArchitecture(Architecture.X86_64);

        switch (credential.cloudPlatform()) {
            case AWS:
                stack.setInstanceGroups(generateAwsInstanceGroups(3));
                break;
            case GCP:
                stack.setInstanceGroups(generateGcpInstanceGroups(3));
                break;
            default:
                break;
        }
        stack.setSecurityConfig(new SecurityConfig());
        return stack;
    }

    public static Orchestrator orchestrator() {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("DUMMY");
        orchestrator.setApiEndpoint("endpoint");
        orchestrator.setAttributes(new Json("{\"test\": \"test\"}"));
        orchestrator.setId(1L);
        return orchestrator;
    }

    public static Workspace workspace(Long id, String name) {
        Workspace workspace = new Workspace();
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setName(name);
        workspace.setId(id);
        return workspace;
    }

    public static User user(Long id, String userId) {
        User user = new User();
        user.setUserId(userId);
        user.setId(id);
        return user;
    }

    public static User user(Long id, String userId, String userCrn) {
        User user = new User();
        user.setUserId(userId);
        user.setId(id);
        user.setUserCrn(userCrn);
        return user;
    }

    public static SecurityGroup securityGroup(long id) {
        SecurityGroup sg = new SecurityGroup();
        sg.setId(id);
        sg.setName("security-group");
        sg.setSecurityRules(new HashSet<>());
        sg.setStatus(ResourceStatus.DEFAULT);
        return sg;
    }

    public static Set<InstanceGroup> generateAwsInstanceGroups(int count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, awsTemplate(1L, "c3.2xlarge")));
        for (int i = 0; i < count - 1; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, awsTemplate(1L, "c3.2xlarge")));
        }
        return instanceGroups;
    }

    public static Set<InstanceGroup> generateGcpInstanceGroups(int count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, gcpTemplate(1L)));
        for (int i = 2; i < count + 1; i++) {
            instanceGroups.add(instanceGroup(Integer.toUnsignedLong(i), InstanceGroupType.CORE, gcpTemplate(1L)));
        }
        return instanceGroups;
    }

    public static InstanceGroup instanceGroup(Long id, String name, InstanceGroupType instanceGroupType, Template template) {
        return instanceGroup(id, name, instanceGroupType, template, 1);
    }

    public static InstanceGroup instanceGroup(Long id, InstanceGroupType instanceGroupType, Template template) {
        return instanceGroup(id, instanceGroupType, template, 1);
    }

    public static InstanceGroup instanceGroup(Long id, InstanceGroupType instanceGroupType, Template template, int nodeCount) {
        return instanceGroup(id, "is" + id, instanceGroupType, template, nodeCount);
    }

    public static InstanceGroup instanceGroup(Long id, String name, InstanceGroupType instanceGroupType, Template template, int nodeCount) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(id);
        instanceGroup.setGroupName(name);
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setTemplate(template);
        instanceGroup.setSecurityGroup(securityGroup(1L));
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork());
        instanceGroup.setInstanceMetaData(generateInstanceMetaDatas(nodeCount, id, instanceGroup));
        return instanceGroup;
    }

    private static InstanceGroupNetwork instanceGroupNetwork() {
        return new InstanceGroupNetwork();
    }

    public static Network network() {
        return network("10.0.0.1/16");
    }

    public static Network network(String subnet) {
        Network network = new Network();
        network.setSubnetCIDR(subnet);
        //        network.setAddressPrefixCIDR(DUMMY_ADDRESS_PREFIX_CIDR);
        network.setId(1L);
        network.setName(DUMMY_NAME);
        return network;
    }

    public static Network networkWithSubnetId(String subnetId) {
        Network network = new Network();
        network.setId(1L);
        network.setName(DUMMY_NAME);
        network.setAttributes(new Json(Map.of(NetworkConstants.SUBNET_ID, subnetId)));
        return network;
    }

    public static InstanceMetaData instanceMetaData(Long serverNumber, Long instanceGroupId, InstanceStatus instanceStatus, boolean ambariServer,
            InstanceGroup instanceGroup, InstanceMetadataType instanceMetadataType) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setServer(ambariServer);
        instanceMetaData.setSshPort(22);
        instanceMetaData.setDiscoveryFQDN("test-" + instanceGroup.getGroupName() + "-" + instanceGroupId + '-' + serverNumber);
        instanceMetaData.setInstanceId("test-" + instanceGroupId + '-' + serverNumber);
        instanceMetaData.setPrivateIp("1.1." + instanceGroupId + '.' + serverNumber);
        instanceMetaData.setPublicIp("2.2." + instanceGroupId + '.' + serverNumber);
        instanceMetaData.setId(instanceGroupId + serverNumber);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setStartDate(new Date().getTime());
        instanceMetaData.setLifeCycle(InstanceLifeCycle.SPOT);
        instanceMetaData.setProviderInstanceType(instanceGroup.getTemplate() != null ? instanceGroup.getTemplate().getInstanceType() : null);
        boolean gatewayInstanceGroup = instanceGroup.getInstanceGroupType().equals(InstanceGroupType.GATEWAY);
        InstanceMetadataType imType = gatewayInstanceGroup ? InstanceMetadataType.GATEWAY_PRIMARY : instanceMetadataType;
        instanceMetaData.setInstanceMetadataType(imType);
        return instanceMetaData;
    }

    public static InstanceMetaData instanceMetaData(Long serverNumber, Long instanceGroupId, InstanceStatus instanceStatus, boolean ambariServer,
            InstanceGroup instanceGroup) {
        return instanceMetaData(serverNumber, instanceGroupId, instanceStatus, ambariServer, instanceGroup, InstanceMetadataType.CORE);
    }

    public static Set<InstanceMetaData> generateInstanceMetaDatas(int count, Long instanceGroupId, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            instanceMetaDatas.add(instanceMetaData(Integer.toUnsignedLong(i), instanceGroupId, InstanceStatus.SERVICES_RUNNING,
                    instanceGroup.getInstanceGroupType().equals(InstanceGroupType.GATEWAY), instanceGroup));
        }
        return instanceMetaDatas;
    }

    public static Template awsTemplate(Long id, String instanceType) {
        Template awsTemplate = new Template();
        awsTemplate.setInstanceType(instanceType);
        awsTemplate.setId(id);
        awsTemplate.setCloudPlatform(AWS);
        awsTemplate.setId(1L);
        awsTemplate.setName(DUMMY_NAME);
        awsTemplate.setDescription(DUMMY_DESCRIPTION);
        awsTemplate.setVolumeTemplates(
                Sets.newHashSet(volumeTemplate(1, 100, "standard")));
        return awsTemplate;
    }

    public static Template gcpTemplate(Long id) {
        Template gcpTemplate = new Template();
        gcpTemplate.setInstanceType(N2_HIGHCPU_16_INSTANCE);
        gcpTemplate.setId(id);
        gcpTemplate.setCloudPlatform(GCP);
        gcpTemplate.setDescription(DUMMY_DESCRIPTION);
        gcpTemplate.setStatus(ResourceStatus.DEFAULT);
        gcpTemplate.setName(DUMMY_NAME);
        gcpTemplate.setVolumeTemplates(
                Sets.newHashSet(volumeTemplate(1, 100, "pd-ssd")));
        return gcpTemplate;
    }

    public static VolumeTemplate volumeTemplate(Integer volumeCount, Integer volumeSize, String volumeType) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        volumeTemplate.setVolumeSize(volumeSize);
        volumeTemplate.setVolumeType(volumeType);
        volumeTemplate.setId(1L);
        return volumeTemplate;
    }

    public static Stack stack() {
        return stack(AVAILABLE, gcpCredential());
    }

    public static Stack stack(Status status) {
        return stack(status, gcpCredential());
    }

    public static Stack stack(Cluster cluster) {
        Stack stack = stack(AVAILABLE, gcpCredential());
        stack.setCluster(cluster);
        return stack;
    }

    public static StackView stackView() {
        return stackView(AVAILABLE, gcpCredential());
    }

    public static Cluster cluster() {
        return cluster(blueprint(), stack(AVAILABLE, gcpCredential()), 0L);
    }

    public static List<Cluster> generateCluster(int count) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clusters.add(cluster(blueprint(), stack(AVAILABLE, gcpCredential()), (long) i));
        }
        return clusters;
    }

    public static Cluster cluster(Blueprint blueprint, Stack stack, Long id) {
        Cluster cluster = new Cluster();
        cluster.setClusterManagerIp("50.51.52.100");
        cluster.setStack(stack);
        cluster.setId(id);
        cluster.setName("dummyCluster");
        cluster.setClusterManagerIp("10.0.0.1");
        cluster.setBlueprint(blueprint);
        cluster.setUpSince(new Date().getTime());
        cluster.setUserName("admin");
        cluster.setPassword("admin");
        Gateway gateway = new Gateway();
        setGatewayTopology(gateway, "cb");
        cluster.setGateway(gateway);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        Set<RDSConfig> rdsConfigs = new HashSet<>();
        rdsConfigs.add(rdsConfig);
        cluster.setRdsConfigs(rdsConfigs);
        cluster.setHostGroups(hostGroups(cluster));
        cluster.setConfigStrategy(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES);

        Map<String, String> map = new HashMap<>();
        try {
            cluster.setAttributes(new Json(map).getValue());
        } catch (IllegalArgumentException ignored) {
        }

        Workspace workspace = new Workspace();
        workspace.setName("org 1");
        workspace.setId(1L);
        cluster.setWorkspace(workspace);
        cluster.setDatabaseServerCrn("databaseCRN");
        stack.setCluster(cluster);
        return cluster;
    }

    public static NetworkV4Request networkV4RequestForMock() {
        NetworkV4Request r = new NetworkV4Request();
        r.setMock(mockNetworkV4Parameters());
        r.setCloudPlatform(CloudPlatform.MOCK);
        r.setSubnetCIDR("0.0.0.0/0");
        return r;
    }

    public static MockNetworkV4Parameters mockNetworkV4Parameters() {
        MockNetworkV4Parameters p = new MockNetworkV4Parameters();
        p.setSubnetId("someMockSubnet");
        p.setVpcId("someMockVpc");
        p.setInternetGatewayId("someMockInternetGatewayId");
        return p;
    }

    public static NetworkV4Request networkV4RequestForAws() {
        NetworkV4Request r = new NetworkV4Request();
        r.setAws(awsNetworkV4Parameters());
        r.setCloudPlatform(CloudPlatform.AWS);
        r.setSubnetCIDR("0.0.0.0/0");
        return r;
    }

    public static AwsNetworkV4Parameters awsNetworkV4Parameters() {
        AwsNetworkV4Parameters p = new AwsNetworkV4Parameters();
        p.setSubnetId("someAwsSubnet");
        p.setVpcId("someAwsVpc");
        p.setInternetGatewayId("someAwsInternetGatewayId");
        return p;
    }

    public static KerberosConfig kerberosConfigFreeipa() {
        return KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withType(KerberosType.FREEIPA)
                .withPassword("passwd")
                .withVerifyKdcTrust(true)
                .withTcpAllowed(true)
                .build();
    }

    public static KerberosConfig kerberosConfigMit() {
        return KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withType(KerberosType.MIT)
                .withPassword("passwd")
                .withVerifyKdcTrust(true)
                .withTcpAllowed(true)
                .build();
    }

    public static HostGroup hostGroup(String name) {
        return hostGroup(name, 1);
    }

    public static HostGroup hostGroup(String name, int count) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setId(1L);
        hostGroup.setName(name);
        hostGroup.setRecipes(recipes(1));
        InstanceGroup instanceGroup = instanceGroup(1L, name, InstanceGroupType.CORE, gcpTemplate(1L), count);
        hostGroup.setInstanceGroup(instanceGroup);
        hostGroup.setCluster(cluster(blueprint(), stack(), 1L));
        hostGroup.setRecoveryMode(RecoveryMode.MANUAL);
        return hostGroup;
    }

    public static Set<HostGroup> hostGroups(Set<String> names) {
        Set<HostGroup> hostgroups = new HashSet<>();
        for (String name : names) {
            hostgroups.add(hostGroup(name));
        }
        return hostgroups;
    }

    public static HostGroup hostGroup() {
        return hostGroup(DUMMY_NAME);
    }

    public static Set<HostGroup> hostGroups(Cluster cluster) {
        Set<HostGroup> hostGroups = new HashSet<>();
        HostGroup hg = new HostGroup();
        hg.setCluster(cluster);
        hg.setId(1L);
        hg.setName("slave_1");
        hostGroups.add(hg);
        return hostGroups;
    }

    public static Recipe recipe() {
        Set<Recipe> recipes = recipes(1);
        return recipes.stream().findFirst().get();
    }

    public static Set<Recipe> recipes(int count) {
        Set<Recipe> recipes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Recipe recipe = new Recipe();
            recipe.setDescription("description");
            recipe.setId((long) (i + 1));
            recipe.setName("recipe-" + (i + 1));
            recipe.setContent("base64Content");
            recipe.setRecipeType(RecipeType.POST_SERVICE_DEPLOYMENT);
            Workspace workspace = new Workspace();
            workspace.setId(1L);
            workspace.setName("Top Secret FBI");
            recipe.setWorkspace(workspace);
            recipe.setCreator("someCreator");
            recipe.setResourceCrn("someCrn");
            recipes.add(recipe);
        }
        return recipes;
    }

    public static LdapView.LdapViewBuilder ldapConfigBuilder() {
        return LdapView.LdapViewBuilder.aLdapView().
                withUserSearchBase("cn=users,dc=example,dc=org").
                withUserDnPattern("cn={0},cn=users,dc=example,dc=org").
                withGroupSearchBase("cn=groups,dc=example,dc=org").
                withBindDn("cn=admin,dc=example,dc=org").
                withBindPassword("admin").
                withServerHost("localhost").
                withUserNameAttribute("cn=admin,dc=example,dc=org").
                withDomain("ad.hdc.com").
                withServerPort(389).
                withProtocol("ldap").
                withDirectoryType(DirectoryType.LDAP).
                withUserObjectClass("person").
                withGroupObjectClass("groupOfNames").
                withGroupNameAttribute("cn").
                withGroupMemberAttribute("member").
                withAdminGroup("ambariadmins").
                withCertificate("-----BEGIN CERTIFICATE-----certificate-----END CERTIFICATE-----").
                withConnectionURL("ldap://localhost:389");
    }

    public static LdapView ldapConfig() {
        return ldapConfigBuilder().build();
    }

    public static LdapView ldapConfigWithSpecialChars() {
        return ldapConfigBuilder().withBindPassword("admin<>char").build();
    }

    public static LdapView.LdapViewBuilder adConfigBuilder() {
        return ldapConfigBuilder().withDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
    }

    public static LdapView adConfig() {
        return adConfigBuilder().build();
    }

    public static Blueprint blueprint(String name) {
        return blueprint(name, "{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
    }

    public static Blueprint blueprint(Long id, String name, String blueprintText) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(id);
        blueprint.setBlueprintText(blueprintText);
        blueprint.setName(name);
        blueprint.setStackName("multi-node-yarn");
        blueprint.setStatus(ResourceStatus.DEFAULT);
        blueprint.setTags(getEmptyJson());
        blueprint.setResourceCrn("someCrn");
        return blueprint;
    }

    public static Blueprint blueprint(String name, String blueprintText) {
        return blueprint(1L, name, blueprintText);
    }

    public static Blueprint blueprint() {
        return blueprint("multi-node-yarn");
    }

    public static FailurePolicy failurePolicy() {
        FailurePolicy failurePolicy = new FailurePolicy();
        failurePolicy.setId(1L);
        failurePolicy.setThreshold(10L);
        failurePolicy.setAdjustmentType(AdjustmentType.BEST_EFFORT);
        return failurePolicy;
    }

    public static SecurityGroup securityGroup(Set<SecurityRule> securityRules) {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setDescription(DUMMY_DESCRIPTION);
        securityGroup.setId(1L);
        securityGroup.setName(DUMMY_NAME);
        securityGroup.setStatus(ResourceStatus.DEFAULT);
        securityGroup.setSecurityRules(securityRules);
        securityGroup.setSecurityGroupIds(Collections.singleton(DUMMY_SECURITY_GROUP_ID));
        securityGroup.setCloudPlatform(AWS);
        return securityGroup;
    }

    public static Set<InstanceGroup> generateGcpInstanceGroupsByNodeCount(int... count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(0L, InstanceGroupType.GATEWAY, gcpTemplate(1L), count[0]));
        for (int i = 1; i < count.length; i++) {
            instanceGroups.add(instanceGroup((long) i, InstanceGroupType.CORE, gcpTemplate(1L), count[i]));
        }
        return instanceGroups;
    }

    public static List<Resource> generateGcpResources(int count) {
        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            resources.add(gcpResource((long) i, "master"));
        }
        return resources;
    }

    public static Resource gcpResource(Long id, String instanceGroup) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setStack(stack());
        resource.setInstanceGroup(instanceGroup);
        resource.setResourceName("testResource");
        resource.setResourceType(ResourceType.GCP_INSTANCE);
        resource.setAvailabilityZone("az1");
        return resource;
    }

    public static RDSConfig rdsConfig(DatabaseType databaseType, DatabaseVendor databaseVendor) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        rdsConfig.setName(databaseType.name() + rdsConfig.getId());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL(getConneectionUrl(databaseType, databaseVendor));
        rdsConfig.setType(databaseType.name());
        rdsConfig.setConnectionDriver(databaseVendor.connectionDriver());
        rdsConfig.setDatabaseEngine(databaseVendor);
        rdsConfig.setDescription("someDescription");
        rdsConfig.setCreationDate(1234567L);
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setStackVersion("3.2");
        rdsConfig.setConnectorJarUrl("http://somejarurl.com");
        rdsConfig.setClusters(Collections.emptySet());
        return rdsConfig;
    }

    public static RdsConfigWithoutCluster rdsConfigWithoutCluster(DatabaseType databaseType, DatabaseVendor databaseVendor,
        RdsSslMode sslMode) {
        RdsConfigWithoutCluster rdsConfig = Mockito.mock(RdsConfigWithoutCluster.class);
        lenient().when(rdsConfig.getId()).thenReturn(generateUniqueId());
        String databaseName = databaseType.name() + rdsConfig.getId();
        lenient().when(rdsConfig.getName()).thenReturn(databaseName);
        lenient().when(rdsConfig.getConnectionPassword()).thenReturn("iamsoosecure");
        lenient().when(rdsConfig.getConnectionUserName()).thenReturn("heyitsme");
        lenient().when(rdsConfig.getConnectionURL()).thenReturn(getConneectionUrl(databaseType, databaseVendor));
        lenient().when(rdsConfig.getType()).thenReturn(databaseType.name());
        lenient().when(rdsConfig.getConnectionDriver()).thenReturn(databaseVendor.connectionDriver());
        lenient().when(rdsConfig.getDatabaseEngine()).thenReturn(databaseVendor);
        lenient().when(rdsConfig.getDescription()).thenReturn("someDescription");
        lenient().when(rdsConfig.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        lenient().when(rdsConfig.getSslMode()).thenReturn(sslMode);
        lenient().when(rdsConfig.getStackVersion()).thenReturn("3.2");
        lenient().when(rdsConfig.getConnectorJarUrl()).thenReturn("http://somejarurl.com");
        return rdsConfig;
    }

    public static RDSConfig rdsConfig(DatabaseType databaseType) {
        return rdsConfig(databaseType, DatabaseVendor.POSTGRES);
    }

    public static RdsConfigWithoutCluster rdsConfigWithoutCluster(DatabaseType databaseType, RdsSslMode sslMode) {
        return rdsConfigWithoutCluster(databaseType, DatabaseVendor.POSTGRES, sslMode);
    }

    private static String getConneectionUrl(DatabaseType databaseType, DatabaseVendor databaseVendor) {
        String connectionUrl;
        if (databaseVendor == DatabaseVendor.ORACLE12 || databaseVendor == DatabaseVendor.ORACLE11) {
            connectionUrl = "jdbc:" + databaseVendor.jdbcUrlDriverId() + ":@10.1.1.1:1521:"
                    + databaseType.name().toLowerCase(Locale.ROOT);
        } else if (databaseVendor == DatabaseVendor.MYSQL) {
            connectionUrl = "jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:3306/"
                    + databaseType.name().toLowerCase(Locale.ROOT);
        } else {
            connectionUrl = "jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:5432/"
                    + databaseType.name().toLowerCase(Locale.ROOT);
        }
        return connectionUrl;
    }

    private static void setGatewayTopology(Gateway gateway) {
        setGatewayTopology(gateway, "topology");
    }

    private static void setGatewayTopology(Gateway gateway, String topologyName) {
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(topologyName);
        gatewayTopology.setExposedServices(getEmptyJson());
        gateway.getTopologies().add(gatewayTopology);
    }

    public static Gateway gatewayEnabled() {
        Gateway gateway = new Gateway();
        setGatewayTopology(gateway);
        gateway.setPath("/path");
        gateway.setSsoProvider("simple");
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setSignCert("signcert");
        gateway.setSignKey("signkey");
        gateway.setTokenCert("tokencert");
        gateway.setSignPub("signpub");
        return gateway;
    }

    public static Gateway gatewayEnabledWithExposedKnoxServices(String... exposedKnoxServices) {
        Gateway gateway = gatewayEnabled();
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setExposedServices(new Json(exposedKnoxServices));
        gateway.setTopologies(Sets.newHashSet(gatewayTopology));
        return gateway;
    }

    public static Gateway gatewayEnabledWithoutSSOAndWithRanger() {
        Gateway gateway = new Gateway();
        setGatewayTopology(gateway);
        gateway.setPath("/path");
        gateway.setSsoType(SSOType.NONE);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setSignCert("signcert");
        gateway.setSignKey("signkey");
        gateway.setTokenCert("tokencert");
        gateway.setSignPub("signpub");
        return gateway;
    }

    public static StorageLocations emptyStorageLocations() {
        return new StorageLocations();
    }

    public static StorageLocations storageLocations() {
        StorageLocations storageLocations = new StorageLocations();
        for (int i = 0; i < 10; i++) {
            storageLocations.getLocations().add(storageLocation(i));
        }
        return storageLocations;
    }

    public static StorageLocation storageLocation(int i) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setValue(i + "_test/test/end");
        storageLocation.setProperty(i + "_property");
        storageLocation.setConfigFile(i + "_file");
        return storageLocation;
    }

    public static StorageLocation storageLocation(String configFile, int i) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setValue("random.value." + i);
        storageLocation.setProperty("random.property." + i);
        storageLocation.setConfigFile(configFile);
        return storageLocation;
    }

    private static Json getEmptyJson() {
        return new Json("{}");
    }

    public static Long generateUniqueId() {
        return UNIQUE_ID.incrementAndGet();
    }

    public static void setSecretField(Class<?> clazz, String fieldName, Object target, String raw, String secret) {
        Field field = ReflectionUtils.findField(clazz, fieldName);
        ReflectionUtils.makeAccessible(field);
        try {
            field.set(target, new Secret(raw, secret));
        } catch (IllegalAccessException ignore) {
        }
    }

    public static Object[][] combinationOf(Object[] first, Object[] second) {

        Object[][] testData = new Object[first.length * second.length][2];

        int index = 0;
        if (first.length > second.length) {
            for (Object elementOfSecond : second) {
                for (Object elementOfFirst : first) {
                    testData[index][0] = elementOfFirst;
                    testData[index][1] = elementOfSecond;
                    index++;
                }
            }
        } else {
            for (Object elementOfFirst : first) {
                for (Object elementOfSecond : second) {
                    testData[index][0] = elementOfFirst;
                    testData[index][1] = elementOfSecond;
                    index++;
                }
            }
        }

        return testData;
    }

    public static <K, V> Map<K, V> combineMaps(Map<K, V> map1, Map<K, V> map2) {
        return Stream.of(map1, map2).flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static LdapNotificationDetails ldapNotificationDetails(String message, String type) {
        LdapNotificationDetails ldapNotificationDetails = new LdapNotificationDetails();
        ldapNotificationDetails.setLdapDetails(ldapDetails());
        ldapNotificationDetails.setNotification(message);
        ldapNotificationDetails.setNotificationType(type);
        return ldapNotificationDetails;
    }

    public static RdsNotificationDetails rdsNotificationDetails(String message, String type) {
        RdsNotificationDetails rdsNotificationDetails = new RdsNotificationDetails();
        rdsNotificationDetails.setRdsDetails(rdsDetails());
        rdsNotificationDetails.setNotification(message);
        rdsNotificationDetails.setNotificationType(type);
        return rdsNotificationDetails;
    }

    public static NotificationDetails notificationDetails(String message, String type) {
        NotificationDetails notification = new NotificationDetails();
        notification.setInstanceGroup("master");
        notification.setRegion("us");
        notification.setStackName("usagestack");
        notification.setStackId(1L);
        notification.setNotification(message);
        notification.setNotificationType(type);
        notification.setCloud(GCP);
        notification.setBlueprintName("blueprintName");
        notification.setBlueprintId(1L);
        notification.setStackStatus(AVAILABLE.name());
        notification.setNodeCount(1);
        notification.setClusterStatus(AVAILABLE.name());
        notification.setClusterId(1L);
        notification.setClusterName("test");
        notification.setStackCrn("someCrn");
        return notification;
    }

    public static LdapDetails ldapDetails() {
        LdapDetails details = new LdapDetails();
        details.setTenantName("someTenant");
        details.setUserId("someUserId");
        details.setUserName("someUsername");
        details.setWorkspaceId(123L);
        details.setAdminGroup("adminGroupValue");
        details.setCertificate("cert");
        details.setDescription(DUMMY_DESCRIPTION);
        details.setDirectoryType(DirectoryType.LDAP.name());
        details.setGroupMemberAttribute("somevalue");
        details.setGroupNameAttribute("nameattribute");
        details.setGroupObjectClass("objectclass");
        details.setGroupSearchBase("searchbase");
        details.setAdminGroup("admingroup");
        details.setUserDnPattern("userdnpattern");
        details.setUserNameAttribute("usernameattribute");
        details.setUserObjectClass("userobjectclass");
        details.setUserSearchBase("usersearchbase");
        details.setServerPort(1234);
        details.setServerHost("somehost");
        details.setProtocol("https");
        details.setDomain("somedomain");
        details.setId(111L);
        details.setName("ldapname");
        return details;
    }

    public static RdsDetails rdsDetails() {
        return new RdsDetails();
    }

    public static Node node() {
        return new Node("privateIp", "publicIp", "instanceId", "instanceType", "fqdn", "hostgroup");
    }
}
