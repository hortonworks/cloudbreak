package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;

import java.util.Map;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class MockKerberizedClusterCreationTest extends MockClusterCreationWithSaltSuccessTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockKerberizedClusterCreationTest.class);

    private static final String DEFAULT_KERBEROS_PASSWORD = "admin123!@#";

    private static final String DEFAULT_KERBEROS_ADMIN = "admin";

    private static final String DEFAULT_KERBEROS_PRINCIPAL = "/admin";

    private String kerberosPassword;

    private String kerberosAdmin;

    @Before
    public void setup() { }

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({"clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "mockPort"})
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("8080") String ambariPort, @Optional("admin") String ambariUser,
            @Optional("admin123!@#") String ambariPassword, @Optional("false") boolean emailNeeded,
            @Optional("true") boolean enableSecurity, @Optional(DEFAULT_KERBEROS_PASSWORD) String kerberosMasterKey,
            @Optional(DEFAULT_KERBEROS_ADMIN) String kerberosAdmin, @Optional(DEFAULT_KERBEROS_PASSWORD) String kerberosPassword,
            @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari, @Optional("9443") int mockPort) throws Exception {

        this.kerberosAdmin = kerberosAdmin;
        this.kerberosPassword = kerberosPassword;

        super.testClusterCreation(clusterName,
                ambariPort,
                ambariUser,
                ambariPassword,
                emailNeeded,
                enableSecurity,
                kerberosMasterKey,
                kerberosAdmin,
                kerberosPassword,
                runRecipesOnHosts,
                checkAmbari,
                mockPort);
    }

    @Override
    protected void customMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        LOGGER.info("Adding custom mappings for Kerberized cluster");
        super.customMappings(instanceMap);
    }

    @Override
    protected void customVerifiers(Map<String, CloudVmMetaDataStatus> instanceMap, Long clusterId, String clusterName) {
        LOGGER.info("Adding custom verifiers for Kerberized cluster");
        super.customVerifiers(instanceMap, clusterId, clusterName);

        verify(SALT_API_ROOT + "/run", "POST")
                .bodyContains("fun=grains.append")
                .bodyContains("kerberos_server_master")
                .exactTimes(1)
                .verify();

        String principalKV = String.format("\"principal\": \"%s%s\"", kerberosAdmin, DEFAULT_KERBEROS_PRINCIPAL);
        String passwordKV = String.format("\"key\": \"%s\"", kerberosPassword);
        verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "POST")
                .exactTimes(1)
                .bodyContains("\"alias\": \"kdc.admin.credential\"")
                .bodyContains(principalKV)
                .bodyContains(passwordKV)
                .verify();
    }
}
