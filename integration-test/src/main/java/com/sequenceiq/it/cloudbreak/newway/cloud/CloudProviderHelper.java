package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public abstract class CloudProviderHelper extends CloudProvider {

    public static final String DEFAULT_AMBARI_USER = "integrationtest.ambari.defaultAmbariUser";

    public static final String DEFAULT_AMBARI_PASSWORD = "integrationtest.ambari.defaultAmbariPassword";

    public static final String DEFAULT_AMBARI_PORT = "integrationtest.ambari.defaultAmbariPort";

    public static final int BEGIN_INDEX = 4;

    public static final String INTEGRATIONTEST_PUBLIC_KEY_FILE = "integrationtest.publicKeyFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderHelper.class);

    private final TestParameter testParameter;

    protected CloudProviderHelper(TestParameter testParameter) {
        LOGGER.info("TestParemeters length: {}");
        this.testParameter = testParameter;
    }

    public static CloudProvider[] providerFactory(String provider, TestParameter testParameter) {
        CloudProvider[] results;
        String[] providerArray = provider.split(",");
        results = new CloudProvider[providerArray.length];
        for (int i = 0; i < providerArray.length; i++) {
            LOGGER.info("Provider: \'{}\'", providerArray[i].toLowerCase().trim());
            CloudProvider cloudProvider;
            switch (providerArray[i].toLowerCase().trim()) {
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
            results[i] = cloudProvider;
        }
        return results;
    }

    public StackEntity aValidStackRequest() {
        return Stack.request()
                .withName(getClusterName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(network())
                .withStackAuthentication(stackauth());
    }

    @Override
    public Entity aValidStackIsCreated() {
        return Stack.isCreated()
                .withName(getClusterName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(network())
                .withStackAuthentication(stackauth());
    }

    abstract StackAuthenticationRequest stackauth();

    abstract NetworkV2Request network();

    List<InstanceGroupV2Request> instanceGroups() {
        List<InstanceGroupV2Request> requests = new ArrayList<>();
        requests.add(master());
        requests.add(compute());
        requests.add(worker());
        return requests;
    }

    public List<InstanceGroupV2Request> instanceGroups(String securityGroupId) {
        List<InstanceGroupV2Request> requests = new ArrayList<>();
        requests.add(master(securityGroupId));
        requests.add(compute(securityGroupId));
        requests.add(worker(securityGroupId));
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
        return req;
    }

    InstanceGroupV2Request master() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("master");
        r.setType(InstanceGroupType.GATEWAY);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    public InstanceGroupV2Request master(String securityGroupId) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("master");
        r.setType(InstanceGroupType.GATEWAY);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityGroupId(securityGroupId);
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request compute() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("compute");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request compute(String securityGroupId) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("compute");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityGroupId(securityGroupId);
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request worker() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("worker");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request worker(String securityGroupId) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("worker");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityGroupId(securityGroupId);
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

    public TestParameter getTestParameter() {
        return testParameter;
    }
}
