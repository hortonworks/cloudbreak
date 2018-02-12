package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

public class CredentialTests extends CloudbreakTest {

    public static final String VALID_CRED_NAME = "valid-credential";

    public static final String VALID_AWSKEY_CRED_NAME = "valid-keybased-credential";

    public static final String VALID_OSV3_CRED_NAME = "valid-v3-credential";

    public static final String AGAIN_CRED_NAME = "again-credential";

    public static final String DELETE_CRED_NAME = "delete-credential";

    public static final String DELETE_AGAIN_CRED_NAME = "delete-again-credential";

    public static final String LONG_DC_CRED_NAME = "long-description-credential";

    public static final String SPECIAL_CRED_NAME = "@#$%|:&*;";

    public static final String INVALID_SHORT_CRED_NAME = "";

    public static final String INVALID_LONG_CRED_NAME = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String EMPTY_CRED_NAME = "temp-empty-credential";

    public static final String CRED_DESCRIPTION = "temporary credential for API E2E tests";

    public static final String INVALID_LONG_DESCRIPTION = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialTests.class);

    private String errorMessage = "";

    private CloudProvider cloudProvider;

    public CredentialTests() {
    }

    public CredentialTests(CloudProvider cp, TestParameter tp) {
        this.cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters({ "provider" })
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (this.cloudProvider != null) {
            LOGGER.info("cloud provider already set - running from factory test");
            return;
        }
        this.cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter())[0];
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_CRED_NAME, VALID_AWSKEY_CRED_NAME, AGAIN_CRED_NAME, VALID_OSV3_CRED_NAME, LONG_DC_CRED_NAME, DELETE_AGAIN_CRED_NAME};

        for (int i = 0; i < nameArray.length; i++) {
            LOGGER.info("Delete credential: \'{}\'", nameArray[i].toLowerCase().trim());
            try {
                given(CloudbreakClient.isCreated());
                given(Credential.request()
                        .withName(nameArray[i] + cloudProvider.getPlatform().toLowerCase()));
                when(Credential.delete());
            } catch (ForbiddenException e) {
                String exceptionMessage = e.getResponse().readEntity(String.class);
                this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
                LOGGER.info("ForbiddenException message ::: " + this.errorMessage);
            }
        }
    }

    @Test(priority = 0, groups = { "common" })
    public void testCreateValidCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(VALID_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.get());
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertEquals(credential.getResponse().getName(), VALID_CRED_NAME + cloudProvider.getPlatform().toLowerCase());
                })
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 1, groups = { "common" })
    public void testCreateAgainCredentialException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        given(cloudProvider.aValidCredential()
                .withName(AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 2, groups = { "common" })
    public void testCreateCredentialWithNameOnlyException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(EMPTY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.post());
    }

    @Test(priority = 3, groups = { "common" })
    public void testCreateCredentialWithNameOnlyMessage() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(EMPTY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        try {
            when(Credential.post());
        } catch (BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
            LOGGER.info("MissingParameterException message ::: " + this.errorMessage);
        }
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertTrue(this.errorMessage.contains("Missing "), "MissingParameterException is not match: " + this.errorMessage);
                })
        );
    }

    //"The length of the credential's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 4, groups = { "common" })
    public void testCreateInvalidShortCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(INVALID_SHORT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.post());
    }

    //"The length of the credential's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 5, groups = { "common" })
    public void testCreateInvalidLongCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(INVALID_LONG_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.post());
    }

    //"The length of the credential's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 6, groups = { "common" })
    public void testCreateSpecialCharacterCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(SPECIAL_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.post());
    }

    //BUG-95609 - Won't fix issue
    @Test(priority = 7, groups = { "common" })
    public void testCreateLongDescriptionCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(LONG_DC_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(INVALID_LONG_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.get());
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertEquals(credential.getResponse().getName(), LONG_DC_CRED_NAME + cloudProvider.getPlatform().toLowerCase());
                })
        );
    }

    @Test(expectedExceptions = ForbiddenException.class, priority = 8, groups = { "common" })
    public void testDeleteValidCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential()
                .withName(DELETE_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.delete());
        when(Credential.get());
    }

    @Test(priority = 9, groups = { "common" })
    public void testDeleteAgainCredentialException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.isDeleted((Credential) cloudProvider.aValidCredential())
                .withName(DELETE_AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        given(cloudProvider.aValidCredential()
                .withName(DELETE_AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase())
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform()));
        when(Credential.get());
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertEquals(credential.getResponse().getName(), DELETE_AGAIN_CRED_NAME + cloudProvider.getPlatform().toLowerCase());
                })
        );
    }
}
