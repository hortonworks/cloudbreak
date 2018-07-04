package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigRequestDataCollector;
import com.sequenceiq.it.cloudbreak.newway.Network;
import com.sequenceiq.it.cloudbreak.newway.NetworkEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

public abstract class CloudProviderHelper extends CloudProvider {

    public static final String DEFAULT_AMBARI_USER = "integrationtest.ambari.defaultAmbariUser";

    public static final String DEFAULT_AMBARI_PASSWORD = "integrationtest.ambari.defaultAmbariPassword";

    public static final String DEFAULT_AMBARI_PORT = "integrationtest.ambari.defaultAmbariPort";

    public static final int BEGIN_INDEX = 4;

    public static final String INTEGRATIONTEST_PUBLIC_KEY_FILE = "integrationtest.publicKeyFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderHelper.class);

    private static final String MASTER_INSTANCE_COUNT = "masterInstanceCount";

    private static final String WORKER_INSTANCE_COUNT = "workerInstanceCount";

    private static final String COMPUTE_INSTANCE_COUNT = "computeInstanceCount";

    private final TestParameter testParameter;

    private String clusterNamePostfix;

    protected CloudProviderHelper(TestParameter testParameter) {
        LOGGER.info("TestParemeters length: {}", testParameter.size());
        this.testParameter = testParameter;
    }

    public static CloudProvider[] providersFactory(String provider, TestParameter testParameter) {
        CloudProvider[] results;
        String[] providerArray = provider.split(",");
        results = new CloudProvider[providerArray.length];
        for (int i = 0; i < providerArray.length; i++) {
            LOGGER.info("Provider: \'{}\'", providerArray[i].toLowerCase().trim());
            results[i] = providerFactory(providerArray[i].toLowerCase().trim(), testParameter);
        }
        return results;
    }

    public static CloudProvider providerFactory(String provider, TestParameter testParameter) {
        LOGGER.info("Provider: \'{}\'", provider.toLowerCase().trim());
        CloudProvider cloudProvider;
        switch (provider.toLowerCase().trim()) {
            case AwsCloudProvider.AWS:
                cloudProvider = new AwsCloudProvider(testParameter);
                break;
            case AzureCloudProvider.AZURE:
                cloudProvider = new AzureCloudProvider(testParameter);
                break;
            case GcpCloudProvider.GCP:
                cloudProvider = new GcpCloudProvider(testParameter);
                break;
            case OpenstackCloudProvider.OPENSTACK:
                cloudProvider = new OpenstackCloudProvider(testParameter);
                break;
            default:
                LOGGER.warn("could not determine cloud provider!");
                cloudProvider = null;

        }
        return cloudProvider;
    }

    @Override
    public CredentialEntity aValidCredential() {
        return aValidCredential(true);
    }

    public StackEntity aValidStackRequest() {
        return Stack.request()
                .withName(getClusterName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(newNetwork())
                .withStackAuthentication(stackauth());
    }

    // TODO: 2018. 06. 19. aValidDatalakeStackRequest

    @Override
    public Stack aValidStackIsCreated() {
        return (Stack) Stack.isCreated()
                .withName(getClusterName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(newNetwork())
                .withStackAuthentication(stackauth());
    }

    // TODO: 2018. 06. 19. aValidDatalakeStackIsCreated

    abstract StackAuthenticationRequest stackauth();

    public abstract NetworkV2Request newNetwork();

    public abstract NetworkV2Request existingNetwork();

    public abstract NetworkV2Request existingSubnet();

    public NetworkEntity aValidNetworkIsCreated() {
        return Network.isCreated()
                .withName(getNetworkName())
                .withCloudPlatform(getPlatform())
                .withSubnetCIDR(getSubnetCIDR())
                .withParameters(subnetProperties());
    }

    List<InstanceGroupV2Request> instanceGroups() {
        List<InstanceGroupV2Request> requests = new ArrayList<>();
        requests.add(master());
        requests.add(compute());
        requests.add(worker());
        return requests;
    }

    List<InstanceGroupV2Request> instanceGroups(HostGroupType... groupTypes) {
        List<InstanceGroupV2Request> requests = new ArrayList<>(groupTypes != null ? groupTypes.length : 0);
        if (groupTypes != null) {
            for (HostGroupType groupType : groupTypes) {
                switch (groupType) {
                    case MASTER:
                        requests.add(master());
                        break;
                    case WORKER:
                        requests.add(worker());
                        break;
                    case COMPUTE:
                        requests.add(compute());
                        break;
                    default:
                        break;
                }
            }
        }
        return requests;
    }

    public List<InstanceGroupV2Request> instanceGroups(String securityGroupId) {
        List<InstanceGroupV2Request> requests = new ArrayList<>();
        requests.add(master(securityGroupId));
        requests.add(compute(securityGroupId));
        requests.add(worker(securityGroupId));
        return requests;
    }

    public List<InstanceGroupV2Request> instanceGroups(Set<String> recipes) {
        List<InstanceGroupV2Request> requests = new ArrayList<>();
        requests.add(master(recipes));
        requests.add(compute(recipes));
        requests.add(worker(recipes));
        return requests;
    }

    @Override
    public AmbariV2Request ambariRequestWithBlueprintId(Long id) {
        AmbariV2Request req = new AmbariV2Request();
        req.setUserName(testParameter.get(DEFAULT_AMBARI_USER));
        req.setPassword(testParameter.get(DEFAULT_AMBARI_PASSWORD));
        req.setBlueprintId(id);
        return req;
    }

    @Override
    public AmbariV2Request ambariRequestWithBlueprintName(String name) {
        AmbariV2Request req = new AmbariV2Request();
        req.setUserName(testParameter.get(DEFAULT_AMBARI_USER));
        req.setPassword(testParameter.get(DEFAULT_AMBARI_PASSWORD));
        req.setBlueprintName(name);
        req.setValidateBlueprint(false);
        return req;
    }

    InstanceGroupV2Request master(String securityGroupId) {
        return hostgroup(MASTER, InstanceGroupType.GATEWAY, determineInstanceCount(MASTER_INSTANCE_COUNT), securityGroupId);
    }

    InstanceGroupV2Request compute(String securityGroupId) {
        return hostgroup(COMPUTE, InstanceGroupType.CORE, determineInstanceCount(COMPUTE_INSTANCE_COUNT), securityGroupId);

    }

    InstanceGroupV2Request worker(String securityGroupId) {
        return hostgroup(WORKER, InstanceGroupType.CORE, determineInstanceCount(WORKER_INSTANCE_COUNT), securityGroupId);

    }

    InstanceGroupV2Request master() {
        return hostgroup(MASTER.getName(), InstanceGroupType.GATEWAY, determineInstanceCount(MASTER_INSTANCE_COUNT));
    }

    InstanceGroupV2Request compute() {
        return hostgroup(COMPUTE.getName(), InstanceGroupType.CORE, determineInstanceCount(COMPUTE_INSTANCE_COUNT));
    }

    InstanceGroupV2Request worker() {
        return hostgroup(WORKER.getName(), InstanceGroupType.CORE, determineInstanceCount(WORKER_INSTANCE_COUNT));
    }

    InstanceGroupV2Request master(Set<String> recipes) {
        return hostgroup(MASTER, InstanceGroupType.GATEWAY, determineInstanceCount(MASTER_INSTANCE_COUNT), recipes);
    }

    InstanceGroupV2Request compute(Set<String> recipes) {
        return hostgroup(COMPUTE, InstanceGroupType.CORE, determineInstanceCount(COMPUTE_INSTANCE_COUNT), recipes);
    }

    InstanceGroupV2Request worker(Set<String> recipes) {
        return hostgroup(WORKER, InstanceGroupType.CORE, determineInstanceCount(WORKER_INSTANCE_COUNT), recipes);
    }

    public InstanceGroupV2Request hostgroup(HostGroupType groupName, InstanceGroupType groupType, int nodeCount, String securityGroupId) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(nodeCount);
        r.setGroup(groupName.getName());
        r.setType(groupType);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityGroupId(securityGroupId);
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    public InstanceGroupV2Request hostgroup(String groupName, InstanceGroupType groupType, int nodeCount) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(nodeCount);
        r.setGroup(groupName);
        r.setType(groupType);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    public InstanceGroupV2Request hostgroup(HostGroupType groupName, InstanceGroupType groupType, int nodeCount, Set<String> recipes) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(nodeCount);
        r.setGroup(groupName.getName());
        r.setType(groupType);
        r.setRecipeNames(recipes);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    abstract TemplateV2Request template();

    List<SecurityRuleRequest> rules() {
        List<SecurityRuleRequest> rules = new ArrayList<>();
        SecurityRuleRequest a = new SecurityRuleRequest();
        a.setSubnet("0.0.0.0/0");
        a.setProtocol("tcp");
        a.setPorts("22,443,8443,9443,8080");
        rules.add(a);

        return rules;
    }

    // TODO: 2018. 06. 19. ide az rds és ldap konfigokat CloudProviderből
    public LdapConfigRequest getLdap() {
        return LdapConfigRequestDataCollector.createLdapRequestWithProperties(testParameter);
    }

    public TestParameter getTestParameter() {
        return testParameter;
    }

    @Override
    public void setClusterNamePostfix(String clusterNamePostfix) {
        this.clusterNamePostfix = clusterNamePostfix;
    }

    @Override
    public String getClusterNamePostfix() {
        return clusterNamePostfix;
    }

    private int determineInstanceCount(String key) {
        String instanceCount = testParameter.get(key);
        int instanceCountInt;
        try {
            instanceCountInt = Integer.parseInt(instanceCount);
        } catch (NumberFormatException e) {
            instanceCountInt = 1;
        }
        return instanceCountInt;
    }
}