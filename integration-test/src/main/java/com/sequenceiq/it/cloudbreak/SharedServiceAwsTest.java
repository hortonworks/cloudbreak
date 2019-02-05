package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.AccessConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.AttachedClusterStackPostStrategy;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.DatalakeCluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Ranger;

public class SharedServiceAwsTest extends SharedServiceTestRoot {

    public SharedServiceAwsTest() {
        this(LoggerFactory.getLogger(SharedServiceAwsTest.class), AWS, Hive.CONFIG_NAME, Ranger.CONFIG_NAME);
    }

    private SharedServiceAwsTest(@Nonnull Logger logger, String implementation, String hiveConfigKey, String rangerConfigKey) {
        super(logger, implementation, hiveConfigKey, rangerConfigKey);
    }

    @Override
    @Priority(10)
    @Test
    public void testADatalakeClusterCreation() throws Exception {
        given(CloudbreakClient.created());
        given(getCloudProvider().aValidCredential());
        given(getResourceHelper().aValidHiveDatabase());
        given(getResourceHelper().aValidRangerDatabase());
        given(getResourceHelper().aValidLdap());
        given(AccessConfigEntity.isGot());
        given(getCloudProvider().aValidDatalakeCluster(), "a datalake cluster request");
        given(getCloudProvider().aValidStackRequest()
                .withInstanceGroups(getCloudProvider().instanceGroups(HostGroupType.MASTER))
                .withName(getDatalakeClusterName()));
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
    @Priority(20)
    @Test
    public void testClusterAttachedToDatalakeCluster() throws Exception {
        given(CloudbreakClient.created());
        given(getCloudProvider().aValidCredential());
        given(AccessConfigEntity.isGot());
        given(DatalakeCluster.isCreatedWithName(getDatalakeClusterName()));
        given(getCloudProvider().aValidAttachedCluster(), "an attached cluster request");
        given(getCloudProvider().aValidAttachedStackRequest(getDatalakeClusterName()).withName(getAttachedClusterName()));

        when(Stack.post(new AttachedClusterStackPostStrategy()));

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Override
    @Priority(30)
    @Test
    public void testTerminateAttachedCluster() throws Exception {
        given(CloudbreakClient.created());
        given(getCloudProvider().aValidCredential());
        given(getCloudProvider().aValidStackCreated()
                .withName(getAttachedClusterName()), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Override
    @Priority(40)
    @Test
    public void testTerminateDatalakeCluster() throws Exception {
        given(CloudbreakClient.created());
        given(getCloudProvider().aValidCredential());
        given(getCloudProvider().aValidStackCreated()
                .withName(getDatalakeClusterName()), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }
}
