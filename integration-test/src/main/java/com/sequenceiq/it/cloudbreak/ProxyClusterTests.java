package com.sequenceiq.it.cloudbreak;

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
import com.sequenceiq.it.cloudbreak.newway.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class ProxyClusterTests extends CloudbreakTest {

    private static final String BLUEPRINT_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String VALID_PROXY_CONFIG = "e2e-proxy-cl";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyClusterTests.class);

    private CloudProvider cloudProvider;

    public ProxyClusterTests() {
    }

    public ProxyClusterTests(CloudProvider cp, TestParameter tp) {
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
    public void setupProxy() throws Exception {
        given(CloudbreakClient.isCreated());
        String proxyHost = getTestParameter().get("integrationtest.proxyconfig.proxyHost").split(":")[0];
        String proxyUser = getTestParameter().get("integrationtest.proxyconfig.proxyUser");
        String proxyPassword = getTestParameter().get("integrationtest.proxyconfig.proxyPassword");
        Integer proxyPort = Integer.valueOf(getTestParameter().get("integrationtest.proxyconfig.proxyHost").split(":")[1]);
        given(ProxyConfig.isCreated()
                .withName(VALID_PROXY_CONFIG)
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http")
        );
    }

    @Test
    @Parameters("securityGroupId")
    public void testCreateClusterWithProxy(String securityGroupId) throws Exception {
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME))
                        .withProxyConfigName(VALID_PROXY_CONFIG), "a cluster request with proxy");
        given(cloudProvider.aValidStackRequest()
                .withInstanceGroups(cloudProvider.instanceGroups(securityGroupId)),  "a stack request with given security group");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
        then(Stack.assertThis(
                (stack, t) -> Assert.assertTrue(stack.getResponse().getCluster().getProxyName().equals(VALID_PROXY_CONFIG))
        ));
    }

    @Test(priority = 1, expectedExceptions = BadRequestException.class)
    public void testTryToDeleteAttachedProxy() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG)
        );
        when(ProxyConfig.delete());
        then(ProxyConfig.assertThis(
                (proxyConfig, t) -> Assert.assertNotNull(proxyConfig.getResponse().getId())
        ));
    }

    @Test(priority = 2)
    public void testTerminateCluster() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(priority = 3)
    public void cleanUp() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG)
        );
        when(ProxyConfig.delete());
    }
}