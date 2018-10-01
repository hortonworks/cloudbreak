package com.sequenceiq.it.cloudbreak.mock;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.mock.StackCreationMock;

public class MockKerberizedClusterCreationTest extends MockClusterCreationWithSaltSuccessTest {
    private static final String DEFAULT_KERBEROS_PASSWORD = "admin123!@#";

    private static final String DEFAULT_KERBEROS_ADMIN = "admin";

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
        // GIVEN
        // WHEN
        super.testClusterCreation(clusterName,
                ambariPort,
                ambariUser,
                ambariPassword,
                enableSecurity,
                kerberosMasterKey,
                kerberosAdmin,
                kerberosPassword,
                runRecipesOnHosts,
                checkAmbari,
                mockPort);
        // THEN
        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        stackCreationMock.verifyKerberosCalls(clusterName, kerberosAdmin, kerberosPassword);
    }
}
