package com.sequenceiq.it.cloudbreak.test;

import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakMockClusterTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Mock;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.verification.Verification;

public class MockClusterTests extends CloudbreakMockClusterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockClusterTests.class);

    @Test
    public void testCreateNewRegularCluster() throws Exception {
        String clusterDefinitionName = "Data Science: Apache Spark 2, Apache Zeppelin";
        String clusterName = "mockcluster";

        given(Cluster.request()
                        .withAmbariRequest(getMockProvider().ambariRequestWithBlueprintName(clusterDefinitionName)),
                "a cluster request");
        given(getMockProvider().aValidStackRequest()
                .withName(clusterName), "a stack request");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Mock.assertCalls(verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=saltutil.sync_all").atLeast(2)));
    }

    private Verification verify(String s, String post) {
        return new Verification(s, post, false);
    }
}
