package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.OPENSTACK;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.SssdProviderType;
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType;
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

public class TestUtil {

    public static final String DUMMY_DESCRIPTION = "dummyDescription";
    public static final String N1_HIGHCPU_16_INSTANCE = "n1-highcpu-16";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    private static final String AZURE_PUB_KEY =
            "-----BEGIN CERTIFICATE-----\n"
                    + "MIICsDCCAhmgAwIBAgIJAPtq+czPZYU/MA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n"
                    + "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n"
                    + "aWRnaXRzIFB0eSBMdGQwHhcNMTQwNTEzMDIxNDUwWhcNMTUwNTEzMDIxNDUwWjBF\n"
                    + "MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50\n"
                    + "ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
                    + "gQCvv6nBCp3wiqDVT0g1dEAJvfLiTU6oPVau9FCaNWrxJgkR697kuxMNhY4CpLXS\n"
                    + "DgmSh/guI4iN5pmQtJ5RJsVBZRHWEu7k+GdvSFkNJ/7+i1t2DOjNtnOxGQ6TpjZg\n"
                    + "lyDGNW2m2IY9iaaTzzwhowCcfMMwC+S0OzZ5AT3YE152XQIDAQABo4GnMIGkMB0G\n"
                    + "A1UdDgQWBBR/lhZljxO+cPl9EQmfSb2sndrKFDB1BgNVHSMEbjBsgBR/lhZljxO+\n"
                    + "cPl9EQmfSb2sndrKFKFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt\n"
                    + "U3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJAPtq+czP\n"
                    + "ZYU/MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEABYXu5HwJ8F9LyPrD\n"
                    + "HkUQUM6HRoybllBZWf0uwrM5Mey/pYwhouR1PNd2/y6OXt5mjzxLG/53YvidfrEG\n"
                    + "I5QW2HYwS3jZ2zlOLx5fj+wmeenxNrMxgP7XkbkVcBa76wdfZ1xBAr0ybXb13Gi2\n"
                    + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n"
                    + "-----END CERTIFICATE-----";
    private static final String DUMMY_NAME = "dummyName";

    private TestUtil() {
    }

    public static String getFilePath(Class clazz, String fileName) {
        try {
            URL resource = clazz.getResource(fileName);
            return resource.toURI().getPath();
        } catch (Exception ex) {
            LOGGER.error("{}: {}", ex.getMessage(), ex);
            return "";
        }
    }

    public static CbUser cbAdminUser() {
        return new CbUser("userid", "testuser", "testaccount", Arrays.asList(CbUserRole.ADMIN, CbUserRole.USER), "givenname", "familyname", new Date());
    }

    public static CbUser cbUser() {
        return new CbUser("userid", "testuser", "testaccount", Collections.singletonList(CbUserRole.USER), "givenname", "familyname", new Date());
    }

    public static Credential awsCredential() {
        Credential awsCredential = new Credential();
        awsCredential.setPublicKey(AZURE_PUB_KEY);
        awsCredential.setPublicInAccount(false);
        awsCredential.setArchived(false);
        awsCredential.setCloudPlatform(AWS);
        awsCredential.setDescription(DUMMY_DESCRIPTION);
        awsCredential.setId(1L);
        awsCredential.setLoginUserName("cb");
        awsCredential.setName(DUMMY_NAME);
        return awsCredential;
    }

    public static Credential gcpCredential() {
        Credential credential = new Credential();
        credential.setPublicKey(AZURE_PUB_KEY);
        credential.setId(1L);
        credential.setName(DUMMY_NAME);
        credential.setCloudPlatform(GCP);
        credential.setLoginUserName("cb");
        credential.setPublicInAccount(true);
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

    public static Stack stack(Status stackStatus, Credential credential) {
        Stack stack = new Stack();
        stack.setStatus(stackStatus);
        stack.setCredential(credential);
        stack.setName("simplestack");
        stack.setOwner("userid");
        stack.setAccount("account");
        stack.setId(1L);
        stack.setInstanceGroups(generateGcpInstanceGroups(3));
        stack.setStatusReason("statusReason");
        stack.setRegion("region");
        stack.setCreated(123L);
        stack.setCloudPlatform(credential.cloudPlatform());
        stack.setOrchestrator(orchestrator());
        stack.setRelocateDocker(true);
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
        try {
            orchestrator.setAttributes(new Json("{\"test\": \"test\"}"));
        } catch (JsonProcessingException e) {
            orchestrator.setAttributes(null);
        }
        orchestrator.setId(1L);
        return orchestrator;
    }

    public static SecurityGroup securityGroup(long id) {
        SecurityGroup sg = new SecurityGroup();
        sg.setId(id);
        sg.setName("security-group");
        sg.setPublicInAccount(true);
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

    public static InstanceGroup instanceGroup(Long id, InstanceGroupType instanceGroupType, Template template) {
        return instanceGroup(id, instanceGroupType, template, 1);
    }

    public static InstanceGroup instanceGroup(Long id, InstanceGroupType instanceGroupType, Template template, int nodeCount) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(id);
        instanceGroup.setNodeCount(nodeCount);
        instanceGroup.setGroupName("is" + id);
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setTemplate(template);
        instanceGroup.setSecurityGroup(securityGroup(1L));
        instanceGroup.setInstanceMetaData(generateInstanceMetaDatas(1, id, instanceGroup));
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
            InstanceGroup instanceGroup) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setAmbariServer(ambariServer);
        instanceMetaData.setConsulServer(true);
        instanceMetaData.setSshPort(22);
        instanceMetaData.setDiscoveryFQDN("test-" + instanceGroupId + "-" + serverNumber);
        instanceMetaData.setInstanceId("test-" + instanceGroupId + "-" + serverNumber);
        instanceMetaData.setPrivateIp("1.1." + instanceGroupId + "." + serverNumber);
        instanceMetaData.setPublicIp("2.2." + instanceGroupId + "." + serverNumber);
        instanceMetaData.setId(instanceGroupId + serverNumber);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setStartDate(new Date().getTime());
        return instanceMetaData;
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
        awsTemplate.setPublicInAccount(true);
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
        openStackTemplate.setPublicInAccount(true);
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
        gcpTemplate.setPublicInAccount(true);
        gcpTemplate.setStatus(ResourceStatus.DEFAULT);
        gcpTemplate.setName(DUMMY_NAME);
        return gcpTemplate;
    }

    public static Stack stack() {
        return stack(AVAILABLE, gcpCredential());
    }

    public static List<Cluster> generateCluster(int count) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clusters.add(cluster(TestUtil.blueprint(), stack(AVAILABLE, gcpCredential()), (long) i));
        }
        return clusters;
    }

    public static Cluster cluster(Blueprint blueprint, Stack stack, Long id) {
        return cluster(blueprint, null, stack, id);
    }

    public static Cluster cluster(Blueprint blueprint, SssdConfig sssdConfig, Stack stack, Long id) {
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
        cluster.setSssdConfig(sssdConfig);
        cluster.setEnableShipyard(true);
        RDSConfig rdsConfig = new RDSConfig();
        cluster.setRdsConfig(rdsConfig);
        cluster.setLdapConfig(ldapConfig());
        cluster.setHostGroups(hostGroups(cluster));
        Map<String, String> map = new HashMap<>();
        try {
            cluster.setAttributes(new Json(map));
        } catch (JsonProcessingException e) {
            cluster.setAttributes(null);
        }
        return cluster;
    }

    public static HostGroup hostGroup() {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setId(1L);
        hostGroup.setName(DUMMY_NAME);
        hostGroup.setRecipes(TestUtil.recipes(1));
        hostGroup.setHostMetadata(TestUtil.hostMetadata(hostGroup, 1));
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L));
        Constraint constraint = new Constraint();
        constraint.setInstanceGroup(instanceGroup);
        constraint.setHostCount(instanceGroup.getNodeCount());
        hostGroup.setConstraint(constraint);
        hostGroup.setCluster(TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(), 1L));
        return hostGroup;
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
            recipe.setTimeout(100);
            recipe.setPublicInAccount(true);
            recipe.setPlugins(createRecipePlugins());
            recipe.setKeyValues(new HashMap<>());
            recipes.add(recipe);
        }
        return recipes;
    }

    public static Set<SssdConfig> sssdConfigs(int count) {
        Set<SssdConfig> configs = new HashSet<>();
        for (int i = 0; i < count; i++) {
            SssdConfig config = new SssdConfig();
            config.setId((long) i);
            config.setName("config-" + (i + 1));
            config.setDescription("description");
            config.setProviderType(SssdProviderType.LDAP);
            config.setUrl("ldap://ldap.domain");
            config.setSchema(SssdSchemaType.RFC2307);
            config.setBaseSearch("dc=domain");
            config.setTlsReqcert(SssdTlsReqcertType.NEVER);
            config.setAdServer("ad.domain");
            config.setKerberosServer("kerberos.domain");
            config.setKerberosRealm("KERBEROS_DOMAIN");
            config.setConfiguration("");
            config.setPublicInAccount(true);
            configs.add(config);
        }
        return configs;
    }

    public static LdapConfig ldapConfig() {
        LdapConfig config = new LdapConfig();
        config.setId(1L);
        config.setName(DUMMY_NAME);
        config.setDescription(DUMMY_DESCRIPTION);
        config.setPublicInAccount(true);
        config.setPrincipalRegex("(.*)");
        config.setUserSearchBase("cn=users,dc=example,dc=org");
        config.setUserSearchFilter("");
        config.setGroupSearchBase("cn=groups,dc=example,dc=org");
        config.setGroupSearchFilter("");
        config.setBindDn("cn=admin,dc=example,dc=org");
        config.setBindPassword("admin");
        config.setServerHost("localhost");
        config.setServerPort(389);
        config.setServerSSL(false);
        return config;
    }

    public static Blueprint blueprint(String name) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText("{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
        blueprint.setName(name);
        blueprint.setBlueprintName("multi-node-yarn");
        blueprint.setStatus(ResourceStatus.DEFAULT);
        return blueprint;
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
        cloudbreakUsage.setCosts(2d);
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
        return cloudbreakUsage;
    }

    public static List<CloudbreakEvent> generateGcpCloudbreakEvents(int count) {
        List<CloudbreakEvent> cloudbreakEvents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cloudbreakEvents.add(gcpCloudbreakEvent((long) i));
        }
        return cloudbreakEvents;
    }

    public static CloudbreakEvent gcpCloudbreakEvent(Long id) {
        CloudbreakEvent cloudbreakEvent = new CloudbreakEvent();
        cloudbreakEvent.setId(id);
        cloudbreakEvent.setInstanceGroup("master");
        cloudbreakEvent.setAccount("account");
        cloudbreakEvent.setOwner("owner");
        cloudbreakEvent.setRegion("us");
        cloudbreakEvent.setStackName("usagestack");
        cloudbreakEvent.setStackId(1L);
        cloudbreakEvent.setEventTimestamp(new Date());
        cloudbreakEvent.setEventMessage("message");
        cloudbreakEvent.setEventType("eventType");
        cloudbreakEvent.setCloud(GCP);
        cloudbreakEvent.setBlueprintName("blueprintName");
        cloudbreakEvent.setBlueprintId(1L);
        cloudbreakEvent.setStackStatus(AVAILABLE);
        cloudbreakEvent.setNodeCount(1);
        cloudbreakEvent.setClusterStatus(AVAILABLE);
        cloudbreakEvent.setClusterId(1L);
        cloudbreakEvent.setClusterName("test");
        return cloudbreakEvent;
    }

    private static Set<Plugin> createRecipePlugins() {
        Set<Plugin> plugin = new HashSet<>();
        plugin.add(new Plugin("first"));
        plugin.add(new Plugin("second"));
        return plugin;
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
        securityGroup.setPublicInAccount(true);
        securityGroup.setDescription(DUMMY_DESCRIPTION);
        securityGroup.setId(1L);
        securityGroup.setName(DUMMY_NAME);
        securityGroup.setStatus(ResourceStatus.DEFAULT);
        securityGroup.setSecurityRules(securityRules);
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

    public static Stack setSpotInstances(Stack stack) {
        if (stack.cloudPlatform().equals(AWS)) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                (instanceGroup.getTemplate()).setAttributes(new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false,\"spotPrice\":0.04}"));
            }
        }
        return stack;

    }
}
