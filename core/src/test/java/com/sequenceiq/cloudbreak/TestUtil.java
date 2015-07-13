package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsEncryption;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.GcpInstanceType;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;
import com.sequenceiq.cloudbreak.domain.GcpRawDiskType;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Template;

public class TestUtil {

    public static final String DUMMY_ADDRESS_PREFIX_CIDR = "dummyAddressPrefixCIDR";
    public static final String DUMMY_DESCRIPTION = "dummyDescription";
    public static final String DUMMY_VPC_ID = "dummyVpcId";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    private static final String AZURE_CERTIFICATE_CONTENT =
            "MIICsDCCAhmgAwIBAgIJAPtq+czPZYU/MA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n"
                    + "BAYTAkFVMRMwEQYDVQQIsdfsd21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n"
                    + "aWRnaXRzIFB0eSBMdGQwHhcNMTQwNTEzMDIxNDUwWhcNMTUwNTEzMDIxNDUwWjBF\n"
                    + "MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50\n"
                    + "ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
                    + "gQCvv6nBCp3wiqDVT0g1dEAJvfLiTU6oPVau9FCaNWrxJgkR697kuxMNhY4CpLXS\n"
                    + "DgmSh/guI4iN5pmQtJ5RJsVBZRHWEu7k+GdvSFkNJ/7+i1t2DOjNtnOxGQ6TpjZg\n"
                    + "lyDGNW2m2IY9iaaTzzwhowCcfMMwC+S0OzZ5AT3YE152XQIDAQABo4GnMIGkMB0G\n"
                    + "A1UdDgQWBBR/lhZljxO+cPl9EQmfSb2sndrKFDB1BgNVHSMEbjBsgBR/lhZljxO+\n"
                    + "cPl9EQmfSb2sndrKFKFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt\n"
                    + "U3RhdGUxITAfBgNVBAoTGsdfdGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJAPtq+czP\n"
                    + "ZYU/MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEABYXu5HwJ8F9LyPrD\n"
                    + "HkUQUM6HRoybllBZWf0uwrM5Mey/pYwhouR1PNd2/y6OXt5mjzxLG/53YvidfrEG\n"
                    + "I5QW2HYwS3jZ2zlOLx5fj+wmeenxNrMxgP7XkbkVcBa76wdfZ1xBAr0ybXb13Gi2\n"
                    + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n";

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
    private static final String DUMMY_INTERNET_GATEWAT_ID = "dummyInternetGatewatId";
    private static final String DUMMY_SSH_LOCATION = "dummySshLocation";

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

    public static CbUser cbUser() {
        return new CbUser("userid", "testuser", "testaccount", Arrays.asList(CbUserRole.ADMIN, CbUserRole.USER), "givenname", "familyname");
    }

    public static Credential azureCredential() {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setPublicKey(AZURE_PUB_KEY);
        azureCredential.setPublicInAccount(false);
        azureCredential.setArchived(false);
        azureCredential.setSubscriptionId("subscription-id");
        azureCredential.setId(1L);
        return azureCredential;
    }

    public static Credential awsCredential() {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setPublicKey(AZURE_PUB_KEY);
        awsCredential.setPublicInAccount(false);
        awsCredential.setArchived(false);
        awsCredential.setRoleArn("rolearn");
        awsCredential.setDescription(DUMMY_DESCRIPTION);
        awsCredential.setId(1L);
        awsCredential.setName(DUMMY_NAME);
        return awsCredential;
    }

    public static Credential gcpCredential() {
        GcpCredential credential = new GcpCredential();
        credential.setProjectId("dummyProjectId");
        credential.setServiceAccountId("dummyServiceAccountId");
        credential.setServiceAccountPrivateKey("dummyServiceAccountPrivateKey");
        credential.setPublicKey(AZURE_PUB_KEY);
        credential.setId(1L);
        credential.setName(DUMMY_NAME);
        credential.setPublicInAccount(true);
        credential.setDescription(DUMMY_DESCRIPTION);
        return credential;
    }

    public static Credential openStackCredential() {
        OpenStackCredential credential = new OpenStackCredential();
        credential.setUserName("dummyUserName");
        credential.setPassword("dummyPassword");
        credential.setEndpoint("dummyEndpoint");
        credential.setTenantName("dummyTenant");
        credential.setDescription(DUMMY_DESCRIPTION);
        credential.setId(1L);
        credential.setName(DUMMY_NAME);
        credential.setPublicInAccount(true);
        return credential;
    }

    public static Stack setEphemeral(Stack stack) {
        if (stack.cloudPlatform().equals(CloudPlatform.AWS)) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                ((AwsTemplate) instanceGroup.getTemplate()).setVolumeType(AwsVolumeType.Ephemeral);
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
        stack.setInstanceGroups(generateAzureInstanceGroups(3));
        stack.setSecurityGroup(securityGroup(1L));
        stack.setStatusReason("statusReason");
        stack.setRegion("region");
        stack.setImage("image");
        switch (credential.cloudPlatform()) {
            case AWS:
                stack.setInstanceGroups(generateAwsInstanceGroups(3));
                break;
            case AZURE:
                stack.setInstanceGroups(generateAzureInstanceGroups(3));
                break;
            case GCP:
                stack.setInstanceGroups(generateAzureInstanceGroups(3));
                break;
            case OPENSTACK:
                stack.setInstanceGroups(generateAzureInstanceGroups(3));
                break;
            default:
                break;
        }
        return stack;
    }

    private static SecurityGroup securityGroup(long id) {
        SecurityGroup sg = new SecurityGroup();
        sg.setId(id);
        sg.setName("security-group");
        sg.setPublicInAccount(true);
        sg.setSecurityRules(new HashSet<SecurityRule>());
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
        for (int i = 0; i < count - 1; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, openstackTemplate(1L)));
        }
        return instanceGroups;
    }

    public static Set<InstanceGroup> generateAzureInstanceGroups(int count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, azureTemplate(1L)));
        for (int i = 0; i < count - 1; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, azureTemplate(1L)));
        }
        return instanceGroups;
    }

    public static Set<InstanceGroup> generateAzureInstanceGroupsByNodeCount(int ...count) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup(1L, InstanceGroupType.GATEWAY, azureTemplate(1L), count[0]));
        for (int i = 1; i < count.length; i++) {
            instanceGroups.add(instanceGroup(1L, InstanceGroupType.CORE, azureTemplate(1L), count[i]));
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
        instanceGroup.setInstanceMetaData(generateInstanceMetaDatas(1, id, instanceGroup));
        return instanceGroup;
    }

    public static Network network() {
        return network("10.0.0.1/16");
    }

    public static Network network(String subnet) {
        AzureNetwork network = new AzureNetwork();
        network.setSubnetCIDR(subnet);
        network.setAddressPrefixCIDR(DUMMY_ADDRESS_PREFIX_CIDR);
        network.setId(1L);
        network.setName(DUMMY_NAME);
        return network;
    }

    public static AwsNetwork awsNetwork(String subnet) {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setInternetGatewayId(DUMMY_INTERNET_GATEWAT_ID);
        awsNetwork.setVpcId(DUMMY_VPC_ID);
        awsNetwork.setId(1L);
        awsNetwork.setDescription(DUMMY_DESCRIPTION);
        awsNetwork.setName(DUMMY_NAME);
        awsNetwork.setPublicInAccount(true);
        awsNetwork.setSubnetCIDR(subnet);
        return awsNetwork;
    }

    public static GcpNetwork gcpNetwork(String subnet) {
        GcpNetwork network = new GcpNetwork();
        network.setId(1L);
        network.setName(DUMMY_NAME);
        network.setStatus(ResourceStatus.DEFAULT);
        network.setSubnetCIDR(subnet);
        network.setPublicInAccount(true);
        network.setDescription(DUMMY_DESCRIPTION);
        return network;
    }

    public static OpenStackNetwork openStackNetwork(String subnet) {
        OpenStackNetwork network = new OpenStackNetwork();
        network.setId(1L);
        network.setName(DUMMY_NAME);
        network.setStatus(ResourceStatus.DEFAULT);
        network.setSubnetCIDR(subnet);
        network.setPublicInAccount(true);
        return network;
    }

    public static InstanceMetaData instanceMetaData(Long id, InstanceStatus instanceStatus, boolean ambariServer, InstanceGroup instanceGroup) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setVolumeCount(1);
        instanceMetaData.setAmbariServer(ambariServer);
        instanceMetaData.setConsulServer(true);
        instanceMetaData.setContainerCount(1);
        instanceMetaData.setDiscoveryFQDN("test");
        instanceMetaData.setInstanceId("test");
        instanceMetaData.setPrivateIp("1.1.1." + (id + Math.abs(new Random().nextInt(255))));
        instanceMetaData.setPublicIp("2.2.2." + (id + Math.abs(new Random().nextInt(255))));
        instanceMetaData.setId(id);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setStartDate(new Date().getTime());
        instanceMetaData.setDockerSubnet("dockerSubnet");
        return instanceMetaData;
    }

    public static Set<InstanceMetaData> generateInstanceMetaDatas(int count, Long instanceGroupId, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (int i = 0; i < count; i++) {
            instanceMetaDatas.add(instanceMetaData(Long.valueOf(i + instanceGroupId), InstanceStatus.REGISTERED,
                    instanceGroup.getInstanceGroupType().equals(InstanceGroupType.GATEWAY) ? true : false, instanceGroup));
        }
        return instanceMetaDatas;
    }

    public static Template azureTemplate(Long id) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setVmType(AzureVmType.A5);
        azureTemplate.setId(id);
        azureTemplate.setVolumeCount(1);
        azureTemplate.setVolumeSize(100);
        azureTemplate.setName("templateName");
        return azureTemplate;
    }

    public static Template awsTemplate(Long id) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(AwsInstanceType.C32xlarge);
        awsTemplate.setId(id);
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(AwsVolumeType.Standard);
        awsTemplate.setId(1L);
        awsTemplate.setName(DUMMY_NAME);
        awsTemplate.setDescription(DUMMY_DESCRIPTION);
        awsTemplate.setPublicInAccount(true);
        awsTemplate.setEncrypted(AwsEncryption.FALSE);
        awsTemplate.setSshLocation(DUMMY_SSH_LOCATION);
        awsTemplate.setSpotPrice(1.0);
        return awsTemplate;
    }

    public static Template openstackTemplate(Long id) {
        OpenStackTemplate openStackTemplate = new OpenStackTemplate();
        openStackTemplate.setInstanceType("Big");
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
        GcpTemplate gcpTemplate = new GcpTemplate();
        gcpTemplate.setGcpInstanceType(GcpInstanceType.N1_HIGHCPU_16);
        gcpTemplate.setId(id);
        gcpTemplate.setVolumeCount(1);
        gcpTemplate.setVolumeSize(100);
        gcpTemplate.setGcpRawDiskType(GcpRawDiskType.SSD);
        gcpTemplate.setDescription(DUMMY_DESCRIPTION);
        gcpTemplate.setPublicInAccount(true);
        gcpTemplate.setStatus(ResourceStatus.DEFAULT);
        gcpTemplate.setName(DUMMY_NAME);
        return gcpTemplate;
    }

    public static Stack stack() {
        return stack(AVAILABLE, azureCredential());
    }

    public static List<Cluster> generateCluster(int count) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clusters.add(cluster(TestUtil.blueprint(), stack(AVAILABLE, azureCredential()), (long) i));
        }
        return clusters;
    }

    public static Cluster cluster(Blueprint blueprint, Stack stack, Long id) {
        Cluster cluster = new Cluster();
        cluster.setAmbariIp("50.51.52.100");
        cluster.setStack(stack);
        cluster.setId(id);
        cluster.setName("dummyCluster");
        cluster.setAmbariIp("10.0.0.1");
        cluster.setBlueprint(blueprint);
        cluster.setUpSince(new Date().getTime());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason("statusReason");
        cluster.setUserName("userName");
        cluster.setPassword("password");
        AmbariStackDetails ambariStackDetails = new AmbariStackDetails();
        cluster.setAmbariStackDetails(ambariStackDetails);
        cluster.setHostGroups(hostGroups(cluster));
        return cluster;
    }

    public static HostGroup hostGroup() {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setId(1L);
        hostGroup.setName(DUMMY_NAME);
        hostGroup.setRecipes(TestUtil.recipes(1));
        hostGroup.setHostMetadata(TestUtil.hostMetadata(hostGroup, 1));
        hostGroup.setInstanceGroup(TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.azureTemplate(1L)));
        return hostGroup;
    }

    public static Set<HostGroup> hostGroups(Cluster cluster) {
        Set<HostGroup> hostGroups = Sets.newHashSet();
        HostGroup hg = new HostGroup();
        hg.setCluster(cluster);
        hg.setId(1L);
        hg.setName("slave_1");
        hostGroups.add(hg);
        return hostGroups;
    }

    public static Set<HostMetadata> hostMetadata(HostGroup hostGroup, int count) {
        Set<HostMetadata> hostMetadataSet = Sets.newHashSet();
        for (int i = 1; i <= count; i++) {
            HostMetadata hostMetadata = new HostMetadata();
            hostMetadata.setHostName("hostname-" + (i + 1));
            hostMetadata.setHostGroup(hostGroup);
            hostMetadataSet.add(hostMetadata);
        }
        return hostMetadataSet;
    }

    public static Set<Recipe> recipes(int count) {
        Set<Recipe> recipes = Sets.newHashSet();
        for (int i = 0; i < count; i++) {
            Recipe recipe = new Recipe();
            recipe.setDescription("description");
            recipe.setId((long) (i + 1));
            recipe.setName("recipe-" + (i + 1));
            recipe.setTimeout(100);
            recipe.setPublicInAccount(true);
            recipe.setPlugins(createRecipePlugins());
            recipe.setKeyValues(new HashMap<String, String>());
            recipes.add(recipe);
        }
        return recipes;
    }

    public static List<Resource> generateAzureResources(int count) {
        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            resources.add(azureResource(Long.valueOf(i), "master"));
        }
        return resources;
    }

    public static Resource azureResource(Long id, String instanceGroup) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setStack(stack());
        resource.setInstanceGroup(instanceGroup);
        resource.setResourceName("testResource");
        resource.setResourceType(ResourceType.AZURE_VIRTUAL_MACHINE);
        return resource;
    }

    public static Blueprint blueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText("{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
        blueprint.setName("multi-node-yarn");
        blueprint.setBlueprintName("multi-node-yarn");
        return blueprint;
    }

    public static List<CloudbreakUsage> generateAzureCloudbreakUsages(int count) {
        List<CloudbreakUsage> cloudbreakUsages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cloudbreakUsages.add(azureCloudbreakUsage(Long.valueOf(i)));
        }
        return cloudbreakUsages;
    }

    public static CloudbreakUsage azureCloudbreakUsage(Long id) {
        CloudbreakUsage cloudbreakUsage = new CloudbreakUsage();
        cloudbreakUsage.setId(id);
        cloudbreakUsage.setInstanceGroup("master");
        cloudbreakUsage.setAccount("account");
        cloudbreakUsage.setCosts(2d);
        cloudbreakUsage.setDay(new Date());
        cloudbreakUsage.setInstanceHours(1L);
        cloudbreakUsage.setInstanceType("xlarge");
        cloudbreakUsage.setOwner("owner");
        cloudbreakUsage.setProvider(CloudPlatform.AZURE.name());
        cloudbreakUsage.setRegion(CloudRegion.CENTRAL_US.name());
        cloudbreakUsage.setStackName("usagestack");
        cloudbreakUsage.setStackId(1L);
        return cloudbreakUsage;
    }

    public static List<CloudbreakEvent> generateAzureCloudbreakEvents(int count) {
        List<CloudbreakEvent> cloudbreakEvents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cloudbreakEvents.add(azureCloudbreakEvent(Long.valueOf(i)));
        }
        return cloudbreakEvents;
    }

    public static CloudbreakEvent azureCloudbreakEvent(Long id) {
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
        cloudbreakEvent.setCloud(CloudPlatform.AZURE.name());
        cloudbreakEvent.setBlueprintName("blueprintName");
        cloudbreakEvent.setBlueprintId(1L);
        cloudbreakEvent.setStackStatus(Status.AVAILABLE);
        cloudbreakEvent.setNodeCount(1);
        return cloudbreakEvent;
    }

    private static Map<String, PluginExecutionType> createRecipePlugins() {
        Map<String, PluginExecutionType> plugin = Maps.newHashMap();
        plugin.put("all-node-plugin", PluginExecutionType.ALL_NODES);
        plugin.put("one-node-plugin", PluginExecutionType.ONE_NODE);
        return plugin;
    }

    public static AmbariStackDetails ambariStackDetails() {
        AmbariStackDetails ambariStackDetails = new AmbariStackDetails();
        ambariStackDetails.setOs("dummyOs");
        ambariStackDetails.setStack("dummyStack");
        ambariStackDetails.setStackBaseURL("dummyStackBaseUrl");
        ambariStackDetails.setStackRepoId("dummyStackRepoId");
        ambariStackDetails.setUtilsBaseURL("dummyUtilsBaseUrl");
        ambariStackDetails.setUtilsRepoId("dummyUtilsRepoId");
        ambariStackDetails.setVerify(true);
        ambariStackDetails.setVersion("0.1.0");
        return ambariStackDetails;
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
}
