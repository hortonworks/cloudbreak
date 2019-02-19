package com.sequenceiq.it.cloudbreak;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class CredentialTests extends CloudbreakTest {

    private static final String VALID_CRED_NAME = "valid-credential";

    private static final String AGAIN_CRED_NAME = "again-credential";

    private static final String DELETE_CRED_NAME = "delete-credential";

    private static final String DELETE_AGAIN_CRED_NAME = "delete-again-credential";

    private static final String LONG_DC_CRED_NAME = "long-description-credential";

    private static final String SPECIAL_CRED_NAME = "@#$%|:&*;";

    private static final String INVALID_SHORT_CRED_NAME = "";

    private static final String EMPTY_CRED_NAME = "temp-empty-credential";

    private static final String CRED_DESCRIPTION = "temporary credential for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialTests.class);

    private String credentialName = "";

    private String errorMessage = "";

    private int retryQuantityOnGatewayTimeout;

    private CloudProvider cloudProvider;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    public CredentialTests() {
    }

    public CredentialTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_CRED_NAME, AGAIN_CRED_NAME, LONG_DC_CRED_NAME, DELETE_AGAIN_CRED_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete credential: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.created());
                given(Credential.request()
                        .withName(aNameArray + cloudProvider.getPlatform().toLowerCase()));
                when(Credential.delete());
            } catch (WebApplicationException webappExp) {
                try (Response response = webappExp.getResponse()) {
                    String exceptionMessage = response.readEntity(String.class);
                    errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                    LOGGER.info("Cloudbreak Delete Credential Exception message ::: " + errorMessage);
                } finally {
                    LOGGER.info("Cloudbreak Delete Credential have been done.");
                }
            }
        }
    }

    @Test(priority = 0, groups = "credentials")
    public void testCreateValidCredential() throws Exception {
        credentialName = VALID_CRED_NAME + cloudProvider.getPlatform().toLowerCase();
        retryQuantityOnGatewayTimeout = Integer.parseInt(getTestParameter().get("retryQuantity"));

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is created.");
        when(Credential.getWithRetryOnTimeout(retryQuantityOnGatewayTimeout), credentialName + " credential has been got.");
        then(Credential.assertThis(
                (credential, t) -> Assert.assertEquals(credential.getResponse().getName(), credentialName)),
                credentialName + " should be the name of the new credential."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 1, groups = "credentials")
    public void testCreateAgainCredentialException() throws Exception {
        credentialName = AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase();

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is created.");
        when(Credential.postV1(), credentialName + " credential request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 2, groups = "credentials")
    public void testCreateCredentialWithNameOnlyException() throws Exception {
        given(CloudbreakClient.created());
        given(Credential.request()
                .withName(EMPTY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), EMPTY_CRED_NAME + " credential with no parameters request.");
        when(Credential.postV1(), EMPTY_CRED_NAME + " credential with no parameters request has been posted.");
    }

    @Test(priority = 3, groups = "credentials")
    public void testCreateCredentialWithNameOnlyMessage() throws Exception {
        given(CloudbreakClient.created());
        given(Credential.request()
                .withName(EMPTY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), EMPTY_CRED_NAME + " credential with no parameters request.");
        try {
            when(Credential.postV1(), EMPTY_CRED_NAME + " credential with no parameters request has been posted.");
        } catch (BadRequestException badrequestExp) {
            try (Response response = badrequestExp.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("MissingParameterExceptionn message ::: " + errorMessage);
            } catch (WebApplicationException appExp) {
                LOGGER.info("Cloudbreak Credential WebApplication Exception message ::: " + appExp.getMessage());
            }
        }
        then(Credential.assertThis(
                (credential, t) -> Assert.assertTrue(errorMessage.contains("Missing "), "MissingParameterException is not match: " + errorMessage)),
                "Error message [Missing] should be present in response."
        );
    }

    //"The length of the credential's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 4, groups = "credentials")
    public void testCreateInvalidShortCredential() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(false)
                .withName(INVALID_SHORT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), "0 char credential name request.");
        when(Credential.postV1(), "0 char credential name request has been posted.");
    }

    //"The length of the credential's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 5, groups = "credentials")
    public void testCreateInvalidLongCredential() throws Exception {
        String invalidLongName = longStringGeneratorUtil.stringGenerator(101);

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(false)
                .withName(invalidLongName + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), invalidLongName + " credential name request.");
        when(Credential.postV1(), invalidLongName + " credential name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 6, groups = "credentials")
    public void testCreateSpecialCharacterCredential() throws Exception {
        credentialName = SPECIAL_CRED_NAME + cloudProvider.getPlatform().toLowerCase();

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(false)
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential name request.");
        when(Credential.postV1(), credentialName + " credential name request has been posted.");
    }

    //BUG-95609 - Won't fix issue
    //"The length of the credential's description has to be in range of 1 to 1000"
    @Test(expectedExceptions = BadRequestException.class, priority = 7, groups = "credentials")
    public void testCreateLongDescriptionCredential() throws Exception {
        credentialName = LONG_DC_CRED_NAME + cloudProvider.getPlatform().toLowerCase();

        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(false)
                .withName(credentialName)
                .withDescription(invalidLongDescripton)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential description request.");
        when(Credential.postV1(), credentialName + " credential description request has been posted.");
    }

    @Test(expectedExceptions = ForbiddenException.class, priority = 8, groups = "credentials")
    public void testDeleteValidCredential() throws Exception {
        credentialName = DELETE_CRED_NAME + cloudProvider.getPlatform().toLowerCase();
        retryQuantityOnGatewayTimeout = Integer.parseInt(getTestParameter().get("retryQuantity"));

        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is created.");
        when(Credential.delete(), credentialName + " credential delete request has been posted.");
        when(Credential.getWithRetryOnTimeout(retryQuantityOnGatewayTimeout),  credentialName + " credential should not be present in response.");
    }

    @Test(priority = 9, groups = "credentials")
    public void testDeleteAgainCredentialException() throws Exception {
        credentialName = DELETE_AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase();
        retryQuantityOnGatewayTimeout = Integer.parseInt(getTestParameter().get("retryQuantity"));

        given(CloudbreakClient.created());
        given(Credential.deleted((Credential) cloudProvider.aValidCredential())
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is created then immediately deleted.");
        given(cloudProvider.aValidCredential()
                .withName(credentialName)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()), credentialName + " credential is created.");
        when(Credential.getWithRetryOnTimeout(retryQuantityOnGatewayTimeout), credentialName + " credential has been got.");
        then(Credential.assertThis(
                (credential, t) -> Assert.assertEquals(credential.getResponse().getName(), credentialName)),
                credentialName + " credential name should be present in response."
        );
    }
}
