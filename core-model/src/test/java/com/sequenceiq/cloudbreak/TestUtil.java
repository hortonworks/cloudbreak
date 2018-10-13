package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.OPENSTACK;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.v2.WorkspaceStatus;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
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
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class TestUtil {

    private static final String DUMMY_DESCRIPTION = "dummyDescription";

    private static final String DUMMY_SECURITY_GROUP_ID = "dummySecurityGroupId";

    private static final String N1_HIGHCPU_16_INSTANCE = "n1-highcpu-16";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    private static final String DUMMY_NAME = "dummyName";

    private static AtomicLong uniqueId = new AtomicLong(0L);

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
        return new CloudbreakUser("userid", "testuser", "testaccount"
        );
    }

    public static CloudbreakUser cbUser() {
        return new CloudbreakUser("userid", "testuser", "testaccount"
        );
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
        return new StackView(1L, "simplestack", "userid", credential.cloudPlatform(), new StackStatusView());
    }

    public static Stack stack(Status stackStatus, Credential credential) {
        User user = new User();
        user.setUserId("horton@hortonworks.com");
        user.setUserName("Alma ur");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        Stack stack = new Stack();
        stack.setCreator(user);
        stack.setWorkspace(workspace);
        stack.setStackStatus(new StackStatus(stack, stackStatus, "statusReason", DetailedStackStatus.UNKNOWN));
        stack.setCredential(credential);
        stack.setName("simplestack");
        stack.setOwner("userid");
        stack.setAccount("account");
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

    public static User user(Long id, String name) {
        User user = new User();
        user.setUserId(name);
        user.setId(id);
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

    public static InstanceMetaData primaryGateWayInstanceMetaData(Long serverNumber, Long instanceGroupId, InstanceStatus instanceStatus, boolean ambariServer,
            InstanceGroup instanceGroup) {
        return instanceMetaData(serverNumber, instanceGroupId, instanceStatus, ambariServer, instanceGroup, InstanceMetadataType.GATEWAY_PRIMARY);
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
        return cluster(blueprint, stack, id, null);
    }

    public static Cluster cluster(Blueprint blueprint, Stack stack, Long id, KerberosConfig kerberosConfig) {
        Cluster cluster = new Cluster();
        cluster.setAmbariIp("50.51.52.100");
        cluster.setStack(stack);
        cluster.setId(id);
        cluster.setName("dummyCluster");
        cluster.setAmbariIp("10.0.0.1");
        cluster.setBlueprint(blueprint);
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
            cluster.setAttributes(new Json(map));
        } catch (JsonProcessingException ignored) {
            cluster.setAttributes(null);
        }

        if (kerberosConfig != null) {
            cluster.setSecure(true);
            cluster.setKerberosConfig(kerberosConfig);
        }
        Workspace workspace = new Workspace();
        workspace.setName("org 1");
        workspace.setId(1L);
        cluster.setWorkspace(workspace);
        return cluster;
    }

    public static KerberosConfig kerberosConfig() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setType(KerberosType.CB_MANAGED);
        kerberosConfig.setMasterKey("mk");
        kerberosConfig.setAdmin("admin");
        kerberosConfig.setPassword("passwd");
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
            recipe.setUri("https://some/url");
            recipe.setContent("base64Content");
            recipe.setRecipeType(RecipeType.POST_AMBARI_START);
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
        config.setBindDn("cn=admin,dc=example,dc=org");
        config.setBindPassword("admin");
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

    public static Blueprint blueprint(String name) {
        return blueprint(name, "{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
    }

    public static Blueprint blueprint(Long id, String name, String blueprintText) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(id);
        blueprint.setBlueprintText(blueprintText);
        blueprint.setName(name);
        blueprint.setAmbariName("multi-node-yarn");
        blueprint.setStatus(ResourceStatus.DEFAULT);
        blueprint.setTags(getEmptyJson());
        return blueprint;
    }

    public static Blueprint blueprint(String name, String blueprintText) {
        return blueprint(1L, name, blueprintText);
    }

    public static Blueprint blueprint() {
        return blueprint("multi-node-yarn");
    }

    public static List<CloudbreakUsage> generateAzureCloudbreakUsages(int count) {
        List<CloudbreakUsage> cloudbreakUsages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cloudbreakUsages.add(gcpCloudbreakUsage((long) i));
        }
        return cloudbreakUsages;
    }

    public static CloudbreakUsage gcpCloudbreakUsage(Long id) {
        CloudbreakUsage cloudbreakUsage = new CloudbreakUsage();
        cloudbreakUsage.setId(id);
        cloudbreakUsage.setInstanceGroup("master");
        cloudbreakUsage.setAccount("account");
        cloudbreakUsage.setCosts(2.0D);
        cloudbreakUsage.setDay(new Date());
        cloudbreakUsage.setInstanceHours(1L);
        cloudbreakUsage.setInstanceType("xlarge");
        cloudbreakUsage.setOwner("owner");
        cloudbreakUsage.setProvider(GCP);
        cloudbreakUsage.setRegion("Central US");
        cloudbreakUsage.setStackName("usagestack");
        cloudbreakUsage.setStackId(1L);
        cloudbreakUsage.setBlueprintId(1L);
        cloudbreakUsage.setBlueprintName("blueprint");
        cloudbreakUsage.setInstanceNum(6);
        cloudbreakUsage.setPeak(10);
        cloudbreakUsage.setFlexId("FLEX-1234567");
        cloudbreakUsage.setStackUuid("23423-sdfasdf-23423-2345");
        return cloudbreakUsage;
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
        smartSenseSubscription.setAccount("hortonworks");
        smartSenseSubscription.setOwner("hwx-user");
        smartSenseSubscription.setId(1L);
        return smartSenseSubscription;
    }

    public static RDSConfig rdsConfig(RdsType rdsType, DatabaseVendor databaseVendor) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        rdsConfig.setName(rdsType.name());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        if (databaseVendor == DatabaseVendor.ORACLE12 || databaseVendor == DatabaseVendor.ORACLE11) {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + ":@10.1.1.1:1521:" + rdsType.name().toLowerCase());
        } else if (databaseVendor == DatabaseVendor.MYSQL) {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:3306/" + rdsType.name().toLowerCase());
        } else {
            rdsConfig.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + "://10.1.1.1:5432/" + rdsType.name().toLowerCase());
        }
        rdsConfig.setType(rdsType.name());
        rdsConfig.setConnectionDriver(databaseVendor.connectionDriver());
        rdsConfig.setDatabaseEngine(databaseVendor);
        return rdsConfig;
    }

    public static RDSConfig rdsConfig(RdsType rdsType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(generateUniqueId());
        rdsConfig.setName(rdsType.name());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL("jdbc:postgresql://10.1.1.1:5432/" + rdsType.name().toLowerCase());
        rdsConfig.setType(rdsType.name());
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
        return uniqueId.incrementAndGet();
    }
}
