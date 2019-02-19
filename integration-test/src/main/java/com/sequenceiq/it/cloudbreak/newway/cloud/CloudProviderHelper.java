package com.sequenceiq.it.cloudbreak.newway.cloud;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigRequestDataCollector;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public abstract class CloudProviderHelper extends CloudProvider {

    public static final String DEFAULT_AMBARI_USER = "integrationtest.ambari.defaultAmbariUser";

    public static final String DEFAULT_AMBARI_PASSWORD = "integrationtest.ambari.defaultAmbariPassword";

    public static final String DEFAULT_AMBARI_PORT = "integrationtest.ambari.defaultAmbariPort";

    public static final int BEGIN_INDEX = 4;

    public static final String INTEGRATIONTEST_PUBLIC_KEY_FILE = "integrationtest.publicKeyFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderHelper.class);

    private static final String DEFAULT_DATALAKE_AMBARI_BLUEPRINT = "Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String RECOVERY_MODE = "RecoveryMode";

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

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
            case MockCloudProvider.MOCK:
                cloudProvider = new MockCloudProvider(testParameter);
                break;
            default:
                LOGGER.warn("could not determine cloud provider!");
                cloudProvider = null;

        }
        return cloudProvider;
    }

    @Override
    public CredentialTestDto aValidCredential() {
        return aValidCredential(true);
    }

    public StackEntity aValidStackRequest() {
        return (StackEntity) Stack.request()
                .withName(getClusterName())
                .withEnvironmentSettings(getEnvironmentSettings())
                .withInstanceGroups(instanceGroups())
                .withNetwork(newNetwork())
                .withStackAuthentication(stackauth());
    }

    @Override
    public Stack aValidStackCreated() {
        return (Stack) Stack.created()
                .withName(getClusterName())
                .withEnvironmentSettings(getEnvironmentSettings())
                .withInstanceGroups(instanceGroups())
                .withNetwork(newNetwork())
                .withStackAuthentication(stackauth());
    }

    public Stack aValidAttachedClusterStackCreated(HostGroupType... groupTypes) {
        return (Stack) Stack.request()
                .withName(getClusterName())
                .withEnvironmentSettings(getEnvironmentSettings())
                .withInstanceGroups(instanceGroups(groupTypes))
                .withNetwork(newNetwork())
                .withStackAuthentication(stackauth());
    }

    public abstract StackAuthenticationV4Request stackauth();

    public abstract NetworkV4Request newNetwork();

    public abstract NetworkV4Request existingNetwork();

    public abstract NetworkV4Request existingSubnet();

    public List<InstanceGroupV4Request> instanceGroups() {
        return instanceGroups(MASTER, COMPUTE, WORKER);
    }

    public List<InstanceGroupV4Request> instanceGroups(HostGroupType... groupTypes) {
        List<InstanceGroupV4Request> requests = new ArrayList<>(groupTypes != null ? groupTypes.length : 0);
        if (groupTypes != null) {
            for (HostGroupType groupType : groupTypes) {
                requests.add(groupType.hostgroupRequest(this, testParameter));
            }
        }
        return requests;
    }

    public List<InstanceGroupV4Request> instanceGroups(String securityGroupId, HostGroupType... groupTypes) {
        List<InstanceGroupV4Request> requests = new ArrayList<>(groupTypes != null ? groupTypes.length : 0);
        if (groupTypes != null) {
            for (HostGroupType groupType : groupTypes) {
                requests.add(groupType.hostgroupRequest(this, testParameter, securityGroupId));
            }
        }
        return requests;
    }

    public List<InstanceGroupV4Request> instanceGroups(Set<String> recipes, HostGroupType... groupTypes) {
        List<InstanceGroupV4Request> requests = new ArrayList<>(groupTypes != null ? groupTypes.length : 0);
        if (groupTypes != null) {
            for (HostGroupType groupType : groupTypes) {
                requests.add(groupType.hostgroupRequest(this, testParameter, recipes));
            }
        }
        return requests;
    }

    @Override
    public RecoveryMode getRecoveryModeParam(String hostgroupName) {
        String argumentName = String.join("", hostgroupName, RECOVERY_MODE);
        String argumentValue = getTestParameter().getWithDefault(argumentName, MANUAL);
        if (argumentValue.equals(AUTO)) {
            return RecoveryMode.AUTO;
        } else {
            return RecoveryMode.MANUAL;
        }
    }

    public List<InstanceGroupV4Request> instanceGroups(String securityGroupId) {
        return instanceGroups(securityGroupId, MASTER, COMPUTE, WORKER);

    }

    public List<InstanceGroupV4Request> instanceGroups(Set<String> recipes) {
        return instanceGroups(recipes, MASTER, COMPUTE, WORKER);
    }

    @Override
    public AmbariV4Request ambariRequestWithBlueprintNameAndCustomAmbari(String bluePrintName, String customAmbariVersion,
            String customAmbariRepoUrl, String customAmbariRepoGpgKey) {
        return ambariRequestWithBlueprintName(bluePrintName);
    }

    @Override
    public AmbariV4Request ambariRequestWithBlueprintName(String bluePrintName) {
        var req = new AmbariV4Request();
        req.setUserName(testParameter.get(DEFAULT_AMBARI_USER));
        req.setPassword(testParameter.get(DEFAULT_AMBARI_PASSWORD));
        req.setClusterDefinitionName(bluePrintName);
        req.setValidateClusterDefinition(false);
        req.setValidateRepositories(Boolean.TRUE);
        req.setStackRepository(new StackRepositoryV4Request());
        return req;
    }

    public LdapV4Request getLdap() {
        return LdapConfigRequestDataCollector.createLdapRequestWithProperties(testParameter);
    }

    public TestParameter getTestParameter() {
        return testParameter;
    }

    protected Set<StorageLocationV4Request> defaultDatalakeStorageLocations(CloudStorageTypePathPrefix type, String parameterToInsert) {
        Set<StorageLocationV4Request> request = new LinkedHashSet<>(2);
        request.add(createLocation(
                String.format("%s://%s/apps/hive/warehouse", type.getPrefix(), parameterToInsert),
                "hive-site",
                "hive.metastore.warehouse.dir"));
        request.add(createLocation(
                String.format("%s://%s/apps/ranger/audit", type.getPrefix(), parameterToInsert),
                "ranger-env",
                "xasecure.audit.destination.hdfs.dir"));
        return request;
    }

    protected CloudStorageV4Request getCloudStorageForAttachedCluster(CloudStorageTypePathPrefix type, String parameterToInsert,
            CloudStorageV4Parameters emptyType) {
        var request = new CloudStorageV4Request();
        var locations = new LinkedHashSet<StorageLocationV4Request>(1);
        locations.add(
                createLocation(
                        String.format("%s://%s/attached/apps/hive/warehouse", type.getPrefix(), parameterToInsert),
                        "hive-site",
                        "hive.metastore.warehouse.dir"));
        request.setLocations(locations);
        type.setParameterForRequest(request, emptyType);
        return request;
    }

    protected StorageLocationV4Request createLocation(String value, String propertyFile, String propertyName) {
        var location = new StorageLocationV4Request();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }

    protected String getDatalakeBlueprintName() {
        String clusterDefinitionName = testParameter.get("datalakeBlueprintName");
        return clusterDefinitionName != null ? clusterDefinitionName : DEFAULT_DATALAKE_AMBARI_BLUEPRINT;
    }
}