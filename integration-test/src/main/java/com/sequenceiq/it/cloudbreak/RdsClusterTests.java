package com.sequenceiq.it.cloudbreak;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class RdsClusterTests extends CloudbreakTest {

    private static final String BLUEPRINT_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String VALID_RDS_CONFIG = "e2e-rds-cl";

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsClusterTests.class);

    private CloudProvider cloudProvider;

    private Set<String> rdsConfigNames = new HashSet<>();

    public RdsClusterTests() {
    }

    public RdsClusterTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider != null) {
            LOGGER.info("cloud provider already set - running from factory test");
            return;
        }
        cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        cloudProvider.setClusterNamePostfix("");
    }

    @BeforeTest
    public void setupRds() throws Exception {
        given(CloudbreakClient.isCreated());
        String rdsUser = getTestParameter().get("integrationtest.rdsconfig.rdsUser");
        String rdsPassword = getTestParameter().get("integrationtest.rdsconfig.rdsPassword");
        String rdsConnectionUrl = getTestParameter().get("integrationtest.rdsconfig.rdsConnectionUrl");
        given(RdsConfig.isCreated()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType("HIVE"), "create rds config"
        );

        rdsConfigNames.add(VALID_RDS_CONFIG);
    }

    @Test
    public void testCreateClusterWithRds() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME))
                        .withRdsConfigNames(rdsConfigNames),
                "a cluster request with rds config");
        given(cloudProvider.aValidStackRequest(),  "a stack request");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
        then(Stack.assertThis(
                (stack, t) -> Assert.assertEquals(stack.getResponse().getCluster().getRdsConfigs().iterator().next().getName(), VALID_RDS_CONFIG)
        ));
    }

    @Test(priority = 1, expectedExceptions = BadRequestException.class)
    public void testTryToDeleteAttachedRds() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
        );
        when(RdsConfig.delete());
    }

    @Test (priority = 2)
    public void testTerminateCluster() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test (priority = 3)
    public void deleteRds() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
        );
        when(RdsConfig.delete());
    }
}