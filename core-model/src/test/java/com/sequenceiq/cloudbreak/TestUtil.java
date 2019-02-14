package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.OPENSTACK;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class TestUtil {

    private static final String DUMMY_DESCRIPTION = "dummyDescription";

    private static final String DUMMY_SECURITY_GROUP_ID = "dummySecurityGroupId";

    private static final String N1_HIGHCPU_16_INSTANCE = "n1-highcpu-16";

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
        return new CloudbreakUser("userid", "testuser", "email", "testaccount");
    }

    public static CloudbreakUser cbUser() {
        return new CloudbreakUser("userid", "testuser", "email", "testaccount");
    }

    public static Credential awsCredential() {
        Credential awsCredential = new Credential();
        awsCredential.setArchived(false);
        awsCredential.setCloudPlatform(AWS);
        awsCredential.setDescription(DUMMY_DESCRIPTION);
        awsCredential.setId(1L);
        awsCredential.setName(DUMMY_NAME);
        return awsCredential;
    }

    public static Credential gcpCredential() {
        Credential credential = new Credential();
        credential.setId(1L);
        credential.setName(DUMMY_NAME);
        credential.setCloudPlatform(GCP);
        credential.setDescription(DUMMY_DESCRIPTION);
        return credential;
    }

    public static Stack setEphemeral(Stack stack) {
        if (stack.cloudPlatform().equals(AWS)) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                (instanceGroup.getTemplate()).setVolumeType("ephemeral");
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
        stack.setCredential(credential);
        stack.setName("simplestack");
        stack.setId(1L);
        stack.setInstanceGroups(generateGcpInstanceGroups(3));
        stack.setRegion("region");
        stack.setCreated(123L);
        stack.setCloudPlatform(credential.cloudPlatform());
        stack.setOrchestrator(orchestrator());

        switch (credential.cloudPlatform()) {
            case AWS:
                stack.setInstanceGroups(generateAwsInstanceGroups(3));
                break;
            case GCP:
                stack.setInstanceGroups(generateGcpInstanceGroups(3));
                break;
            case OPENSTACK:
                stack.setInstanceGroups(generateOpenStackInstanceGroups(3));
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

    public static UserWorkspacePermissions userWorkspacePermissions(User user, Workspace workspace, String... permissions) {
        UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
        userWorkspacePermissions.setUser(user);
        userWorkspacePermissions.setWorkspace(workspace);
        userWorkspacePermissions.setPermissionSet(Set.of(permissions));
        return userWorkspacePermissions;
    }

    public static ChangeWorkspaceUsersV4Request changeWorkspaceUsersJson(String userId, String... permissions) {
        ChangeWorkspaceUsersV4Request json1 = new ChangeWorkspaceUsersV4Request();
        json1.setUserId(userId);
        json1.setPermissions(Set.of(permissions));
        return json1;
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
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, awsTemplate(1L)));
        for (int i = 0; i < count - 1; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, awsTemplate(1L)));
        }
        return instanceGroups;
    }

    public static Set<InstanceGroup> generateOpenStackInstanceGroups(int count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, gcpTemplate(1L)));
        for (int i = 0; i < count - 1; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, gcpTemplate(1L)));
        }
        return instanceGroups;
    }

    public static Set<InstanceGroup> generateGcpInstanceGroups(int count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, openstackTemplate(1L)));
        for (int i = 2; i < count + 1; i++) {
            instanceGroups.add(instanceGroup(Integer.toUnsignedLong(i), InstanceGroupType.CORE, openstackTemplate(1L)));
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
        instanceGroup.setInstanceMetaData(generateInstanceMetaDatas(nodeCount, id, instanceGroup));
        return instanceGroup;
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

    public static InstanceMetaData instanceMetaData(Long serverNumber, Long instanceGroupId, InstanceStatus instanceStatus, boolean ambariServer,
            InstanceGroup instanceGroup, InstanceMetadataType instanceMetadataType) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setAmbariServer(ambariServer);
        instanceMetaData.setConsulServer(true);
        instanceMetaData.setSshPort(22);
        instanceMetaData.setDiscoveryFQDN("test-" + instanceGroupId + '-' + serverNumber);
        instanceMetaData.setInstanceId("test-" + instanceGroupId + '-' + serverNumber);
        instanceMetaData.setPrivateIp("1.1." + instanceGroupId + '.' + serverNumber);
        instanceMetaData.setPublicIp("2.2." + instanceGroupId + '.' + serverNumber);
        instanceMetaData.setId(instanceGroupId + serverNumber);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setStartDate(new Date().getTime());
        instanceMetaData.setInstanceMetadataType(instanceMetadataType);
        return instanceMetaData;
    }

    public static InstanceMetaData instanceMetaData(Long serverNumber, Long instanceGroupId, InstanceStatus instanceStatus, boolean ambariServer,
            InstanceGroup instanceGroup) {
        return instanceMetaData(serverNumber, instanceGroupId, instanceStatus, ambariServer, instanceGroup, InstanceMetadataType.CORE);
    }

    public static Set<InstanceMetaData> generateInstanceMetaDatas(int count, Long instanceGroupId, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            instanceMetaDatas.add(instanceMetaData(Integer.toUnsignedLong(i), instanceGroupId, InstanceStatus.REGISTERED,
                    instanceGroup.getInstanceGroupType().equals(InstanceGroupType.GATEWAY), instanceGroup));
        }
        return instanceMetaDatas;
    }

    public static Template awsTemplate(Long id) {
        Template awsTemplate = new Template();
        awsTemplate.setInstanceType("c3.2xlarge");
        awsTemplate.setId(id);
        awsTemplate.setCloudPlatform(AWS);
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType("standard");
        awsTemplate.setId(1L);
        awsTemplate.setName(DUMMY_NAME);
        awsTemplate.setDescription(DUMMY_DESCRIPTION);
        return awsTemplate;
    }

    public static Template openstackTemplate(Long id) {
        Template openStackTemplate = new Template();
        openStackTemplate.setInstanceType("Big");
        openStackTemplate.setCloudPlatform(OPENSTACK);
        openStackTemplate.setId(id);
        openStackTemplate.setVolumeCount(1);
        openStackTemplate.setVolumeSize(100);
        openStackTemplate.setName(DUMMY_NAME);
        openStackTemplate.setStatus(ResourceStatus.DEFAULT);
        openStackTemplate.setDescription(DUMMY_DESCRIPTION);
        return openStackTemplate;
    }

    public static Template gcpTemplate(Long id) {
        Template gcpTemplate = new Template();
        gcpTemplate.setInstanceType(N1_HIGHCPU_16_INSTANCE);
        gcpTemplate.setId(id);
        gcpTemplate.setCloudPlatform(GCP);
        gcpTemplate.setVolumeCount(1);
        gcpTemplate.setVolumeSize(100);
        gcpTemplate.setDescription(DUMMY_DESCRIPTION);
        gcpTemplate.setStatus(ResourceStatus.DEFAULT);
        gcpTemplate.setName(DUMMY_NAME);
        return gcpTemplate;
    }

    public static Stack stack() {
        return stack(AVAILABLE, gcpCredential());
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
        return cluster(clusterDefinition(), stack(AVAILABLE, gcpCredential()), 0L);
    }

    public static List<Cluster> generateCluster(int count) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clusters.add(cluster(clusterDefinition(), stack(AVAILABLE, gcpCredential()), (long) i));
        }
        return clusters;
    }

    public static Cluster cluster(ClusterDefinition clusterDefinition, Stack stack, Long id) {
        return cluster(clusterDefinition, stack, id, null);
    }

    public static Cluster cluster(ClusterDefinition clusterDefinition, Stack stack, Long id, KerberosConfig kerberosConfig) {
        Cluster cluster = new Cluster();
        cluster.setAmbariIp("50.51.52.100");
        cluster.setStack(stack);
        cluster.setId(id);
        cluster.setName("dummyCluster");
        cluster.setAmbariIp("10.0.0.1");
        cluster.setClusterDefinition(clusterDefinition);
        cluster.setUpSince(new Date().getTime());
        cluster.setStatus(AVAILABLE);
        cluster.setStatusReason("statusReason");
        cluster.setUserName("admin");
        cluster.setPassword("admin");
        Gateway gateway = new Gateway();
        setGatewayTopology(gateway, "cb");
        cluster.setGateway(gateway);
        cluster.setExecutorType(ExecutorType.DEFAULT);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        Set<RDSConfig> rdsConfigs = new HashSet<>();
        rdsConfigs.add(rdsConfig);
        cluster.setRdsConfigs(rdsConfigs);
        cluster.setLdapConfig(ldapConfig());
        cluster.setHostGroups(hostGroups(cluster));
        cluster.setConfigStrategy(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES);

        Map<String, String> map = new HashMap<>();
        try {
            cluster.setAttributes(new Json(map).getValue());
        } catch (JsonProcessingException ignored) {
        }

        if (kerberosConfig != null) {
            cluster.setKerberosConfig(kerberosConfig);
        }
        Workspace workspace = new Workspace();
        workspace.setName("org 1");
        workspace.setId(1L);
        cluster.setWorkspace(workspace);
        return cluster;
    }

    public static KerberosConfig kerberosConfigFreeipa() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setType(KerberosType.FREEIPA);
        kerberosConfig.setAdmin("admin");
        kerberosConfig.setPassword("passwd");
        kerberosConfig.setVerifyKdcTrust(true);
        kerberosConfig.setTcpAllowed(true);
        return kerberosConfig;
    }

    public static KerberosConfig kerberosConfigMit() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setType(KerberosType.MIT);
        kerberosConfig.setAdmin("admin");
        kerberosConfig.setPassword("passwd");
        kerberosConfig.setVerifyKdcTrust(true);
        kerberosConfig.setTcpAllowed(true);
        return kerberosConfig;
    }

    public static HostGroup hostGroup(String name) {
        return hostGroup(name, 1);
    }

    public static HostGroup hostGroup(String name, int count) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setId(1L);
        hostGroup.setName(name);
        hostGroup.setRecipes(recipes(1));
        hostGroup.setHostMetadata(hostMetadata(hostGroup, count));
        InstanceGroup instanceGroup = instanceGroup(1L, name, InstanceGroupType.CORE, gcpTemplate(1L), count);
        Constraint constraint = new Constraint();
        constraint.setInstanceGroup(instanceGroup);
        hostGroup.setConstraint(constraint);
        hostGroup.setCluster(cluster(clusterDefinition(), stack(), 1L));
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

    public static Set<HostMetadata> hostMetadata(HostGroup hostGroup, int count) {
        Set<HostMetadata> hostMetadataSet = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            HostMetadata hostMetadata = new HostMetadata();
            hostMetadata.setHostName("hostname-" + (i + 1));
            hostMetadata.setHostGroup(hostGroup);
            hostMetadataSet.add(hostMetadata);
        }
        return hostMetadataSet;
    }

    public static Set<Recipe> recipes(int count) {
        Set<Recipe> recipes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Recipe recipe = new Recipe();
            recipe.setDescription("description");
            recipe.setId((long) (i + 1));
            recipe.setName("recipe-" + (i + 1));
            recipe.setContent("base64Content");
            recipe.setRecipeType(RecipeType.POST_CLUSTER_INSTALL);
            Workspace workspace = new Workspace();
            workspace.setId(1L);
            workspace.setName("Top Secret FBI");
            recipe.setWorkspace(workspace);
            recipes.add(recipe);
        }
        return recipes;
    }

    public static LdapConfig ldapConfig() {
        LdapConfig config = new LdapConfig();
        config.setId(generateUniqueId());
        config.setName(DUMMY_NAME);
        config.setDescription(DUMMY_DESCRIPTION);
        config.setUserSearchBase("cn=users,dc=example,dc=org");
        config.setUserDnPattern("cn={0},cn=users,dc=example,dc=org");
        config.setGroupSearchBase("cn=groups,dc=example,dc=org");
        setSecretField(LdapConfig.class, "bindDn", config, "cn=admin,dc=example,dc=org", "secret/path");
        setSecretField(LdapConfig.class, "bindPassword", config, "admin", "secret/path");
        config.setServerHost("localhost");
        config.setUserNameAttribute("cn=admin,dc=example,dc=org");
        config.setDomain("ad.hdc.com");
        config.setServerPort(389);
        config.setProtocol("ldap");
        config.setDirectoryType(DirectoryType.LDAP);
        config.setUserObjectClass("person");
        config.setGroupObjectClass("groupOfNames");
        config.setGroupNameAttribute("cn");
        config.setGroupMemberAttribute("member");
        config.setAdminGroup("ambariadmins");
        config.setCertificate("-----BEGIN CERTIFICATE-----certificate-----END CERTIFICATE-----");
        return config;
    }

    public static LdapConfig ldapConfigWithSpecialChars() {
        LdapConfig config = new LdapConfig();
        config.setId(generateUniqueId());
        config.setName(DUMMY_NAME);
        config.setDescription(DUMMY_DESCRIPTION);
        config.setUserSearchBase("cn=users,dc=example,dc=org");
        config.setUserDnPattern("cn={0},cn=users,dc=example,dc=org");
        config.setGroupSearchBase("cn=groups,dc=example,dc=org");
        config.setBindDn("cn=admin,dc=example,dc=org");
        config.setBindPassword("admin<>char");
        config.setServerHost("localhost");
        config.setUserNameAttribute("cn=admin,dc=example,dc=org");
        config.setDomain("ad.hdc.com");
        config.setServerPort(389);
        config.setProtocol("ldap");
        config.setDirectoryType(DirectoryType.LDAP);
        config.setUserObjectClass("person");
        config.setGroupObjectClass("groupOfNames");
        config.setGroupNameAttribute("cn");
        config.setGroupMemberAttribute("member");
        config.setAdminGroup("ambariadmins");
        return config;
    }

    public static LdapConfig adConfig() {
        LdapConfig config = new LdapConfig();
        config.setId(generateUniqueId());
        config.setName(DUMMY_NAME);
        config.setDescription(DUMMY_DESCRIPTION);
        config.setUserSearchBase("cn=users,dc=example,dc=org");
        config.setUserDnPattern("cn={0},cn=users,dc=example,dc=org");
        config.setGroupSearchBase("cn=groups,dc=example,dc=org");
        config.setBindDn("cn=admin,dc=example,dc=org");
        config.setBindPassword("admin");
        config.setServerHost("localhost");
        config.setUserNameAttribute("cn=admin,dc=example,dc=org");
        config.setDomain("ad.hdc.com");
        config.setServerPort(389);
        config.setProtocol("ldap");
        config.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        config.setUserObjectClass("person");
        config.setGroupObjectClass("groupOfNames");
        config.setGroupNameAttribute("cn");
        config.setGroupMemberAttribute("member");
        config.setAdminGroup("ambariadmins");
        return config;
    }

    public static ClusterDefinition clusterDefinition(String name) {
        return clusterDefinition(name, "{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
    }

    public static ClusterDefinition clusterDefinition(Long id, String name, String blueprintText) {
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setId(id);
        clusterDefinition.setClusterDefinitionText(blueprintText);
        clusterDefinition.setName(name);
        clusterDefinition.setStackName("multi-node-yarn");
        clusterDefinition.setStatus(ResourceStatus.DEFAULT);
        clusterDefinition.setTags(getEmptyJson());
        return clusterDefinition;
    }

    public static ClusterDefinition clusterDefinition(String name, String clusterDefinitionText) {
        return clusterDefinition(1L, name, clusterDefinitionText);
    }

    public static ClusterDefinition clusterDefinition() {
        return clusterDefinition("multi-node-yarn");
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
        return resource;
    }

    public static SmartSenseSubscription smartSenseSubscription() {
        SmartSenseSubscription smartSenseSubscription = new SmartSenseSubscription();
        smartSenseSubscription.setSubscriptionId("1234-1234-1234-1244");
        smartSenseSubscription.setId(1L);
        return smartSenseSubscription;
    }

    public static RDSConfig rdsConfig(DatabaseType databaseType, DatabaseVendor databaseVendor) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        rdsConfig.setName(databaseType.name());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        if (databaseVendor == DatabaseVendor.ORACLE12 || databaseVendor == DatabaseVendor.ORACLE11) {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + ":@10.1.1.1:1521:" + databaseType.name().toLowerCase());
        } else if (databaseVendor == DatabaseVendor.MYSQL) {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:3306/" + databaseType.name().toLowerCase());
        } else {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:5432/" + databaseType.name().toLowerCase());
        }
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

    public static RDSConfig rdsConfig(DatabaseType databaseType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        rdsConfig.setName(databaseType.name());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL("jdbc:postgresql://10.1.1.1:5432/" + databaseType.name().toLowerCase());
        rdsConfig.setType(databaseType.name());
        rdsConfig.setConnectionDriver("org.postgresql.Driver");
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        return rdsConfig;
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

    public static Stack setSpotInstances(Stack stack) {
        if (stack.cloudPlatform().equals(AWS)) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                (instanceGroup.getTemplate()).setAttributes(new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false,\"spotPrice\":0.04}"));
            }
        }
        return stack;

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
        field.setAccessible(true);
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
        RdsDetails details = new RdsDetails();
        details.setTenantName("someTenant");
        details.setUserId("someUserId");
        details.setUserName("someUsername");
        details.setWorkspaceId(123L);
        details.setDescription(DUMMY_DESCRIPTION);
        details.setId(111L);
        details.setName("ldapname");

        return details;
    }

}
