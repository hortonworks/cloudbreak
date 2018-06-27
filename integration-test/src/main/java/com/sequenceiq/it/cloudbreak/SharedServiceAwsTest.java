package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.AccessConfig;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.HiveRdsConfigForAws;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.RangerRdsConfigForAws;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;
import static java.util.Collections.singletonList;

public class SharedServiceAwsTest extends SharedServiceTestRoot {

    public SharedServiceAwsTest() {
        this(LoggerFactory.getLogger(SharedServiceAwsTest.class), AWS);
    }

    private SharedServiceAwsTest(@Nonnull Logger logger, String implementation) {
        super(logger, implementation);
    }

    @Override
    public void testADatalakeClusterCreation(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(RangerRdsConfigForAws.isCreatedWithParameters(getTestParameter()));
        given(HiveRdsConfigForAws.isCreatedWithParameters(getTestParameter()));
        given(LdapConfig.isCreatedWithParameters(getTestParameter()));
        given(AccessConfig.isGot());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getS3CloudStorageForDatalake())
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(getTestParameter().get("NN_AWS_DB_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_AWS_DB_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "a datalake cluster request");
        InstanceGroupV2Request hg = cloudProvider.hostgroup("master", InstanceGroupType.GATEWAY, 1);
        given(cloudProvider.aValidStackRequest().withInstanceGroups(singletonList(hg))
                .withName(clusterName));
        when(Stack.post());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Override
    public void testClusterAttachedToDatalakeCluster(CloudProvider cloudProvider, String clusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(AccessConfig.isGot());
        given(Cluster.request()
                        .withSharedService(datalakeClusterName)
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getCloudStorageForAttachedCluster(
                                getTestParameter().get("NN_AWS_S3_BUCKET_NAME"),
                                new S3CloudStorageParameters()))
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(
                                getTestParameter().get("NN_RDS_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_RDS_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "an attached cluster request");
        given(cloudProvider.aValidStackRequest().withName(clusterName));

        when(Stack.post());

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Override
    public void testTerminateAttachedCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(attachedClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Override
    public void testTerminateDatalakeCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(datalakeClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider
    @Override
    public Object[][] providerClusterNameForDatalake() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(AWS, getTestParameter());
        return new Object[][]{
                {cloudProvider, getDatalakeClusterName(), getDatalakeBlueprintName()}
        };
    }

    @DataProvider
    @Override
    public Object[][] providerClusterNameAndBlueprintForAttachedCluster() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(AWS, getTestParameter());
        return new Object[][]{
                {cloudProvider, getAttachedClusterName(), getDatalakeClusterName(), getAttachedClusterBlueprintName()}
        };
    }

    private CloudStorageRequest getS3CloudStorageForDatalake() {
        CloudStorageRequest request = new CloudStorageRequest();
        request.setS3(new S3CloudStorageParameters());
        request.setLocations(defaultDatalakeStorageLocations(getTestParameter().get("NN_AWS_S3_BUCKET_NAME")));
        return request;
    }
}
