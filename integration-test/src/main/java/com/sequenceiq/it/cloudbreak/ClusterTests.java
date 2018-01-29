package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ClusterTests extends CloudbreakTest {

    public static final int BLUEPRINT_ID = 5;

    public static final String BLUEPRINT_HDP26_EDWANALYTICS_NAME = "EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0";

    private CloudProvider cloudProvider;

    public ClusterTests() {
        this.cloudProvider = new AwsCloudProvider();
    }

    public ClusterTests(CloudProvider cp) {
        this.cloudProvider = cp;
    }

    @BeforeClass
    public void beforeClass() {

    }

    @Test
    public void createCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_EDWANALYTICS_NAME)));
        given(cloudProvider.aValidStackRequest());
        when(Stack.post());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
    }

    /*
    @Test
    scaleUp scaleDown sync stop start
     */

    @Test
    public void terminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted());
    }
}
