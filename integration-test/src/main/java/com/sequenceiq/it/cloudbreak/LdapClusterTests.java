package com.sequenceiq.it.cloudbreak;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class LdapClusterTests extends CloudbreakTest {

    private static final String BLUEPRINT_HDP26_NAME = "EDW-ETL: Apache Hive, Apache Spark 2";

    private static final String VALID_LDAP_CONFIG = "e2e-ldap-cl";

    private static final Integer VALID_SERVER_PORT = 389;

    private static final String VALID_LDAP_DESC = "Valid ldap config description";

    private static final String LDAP = "LDAP";

    private static final String BIND_DN = "CN=Administrator,CN=Users,DC=ad,DC=hwx,DC=com";

    private static final String SEARCH_BASE = "CN=Users,DC=ad,DC=hwx,DC=com";

    private static final String USER_NAME_ATTRIBUTE = "sAMAccountName";

    private static final String USER_OBJECT_CLASS = "person";

    private static final String GROUP_MEMBER_ATTRIBUTE = "member";

    private static final String GROUP_NAME_ATTRIBUTE = "cn";

    private static final String GROUP_OBJECT_CLASS  = "group";

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClusterTests.class);

    private CloudProvider cloudProvider;

    public LdapClusterTests() {
    }

    public LdapClusterTests(CloudProvider cp, TestParameter tp) {
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
    public void setupLdap() throws Exception {
        given(CloudbreakClient.isCreated());
        String ldapServerHost = getTestParameter().get("integrationtest.ldapconfig.ldapServerHost");
        String bindPassword = getTestParameter().get("integrationtest.ldapconfig.bindPassword");
        given(LdapConfig.isCreated()
                .withName(VALID_LDAP_CONFIG)
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE)
                .withDescription(VALID_LDAP_DESC), "create ldap config with LDAP directory type"
        );
    }

    @Test
    public void testCreateClusterWithLdap() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME))
                        .withLdapConfigName(VALID_LDAP_CONFIG),
                "a cluster request with ldap config");
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
                (stack, t) -> Assert.assertTrue(stack.getResponse().getCluster().getLdapConfig().getName().equals(VALID_LDAP_CONFIG))
        ));
    }

    @Test(priority = 1, expectedExceptions = BadRequestException.class)
    public void testTryToDeleteAttachedLdap() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG)
        );
        when(LdapConfig.delete());
    }

    @Test(priority = 2)
    public void testTerminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(priority = 3)
    public void deleteLdap() throws Exception {
        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG)
        );
        when(LdapConfig.delete());
    }
}