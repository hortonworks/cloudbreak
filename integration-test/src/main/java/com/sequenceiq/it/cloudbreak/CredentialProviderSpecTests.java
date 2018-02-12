package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

public class CredentialProviderSpecTests extends CloudbreakTest {

    public static final String VALID_AWSKEY_CRED_NAME = "valid-keybased-credential";

    public static final String VALID_OSV3_CRED_NAME = "valid-v3-credential";

    public static final String INVALID_AWSROLE_CRED_NAME = "temp-role-credential";

    public static final String INVALID_AWSACCESS_CRED_NAME = "temp-access-credential";

    public static final String INVALID_AWSSECRET_CRED_NAME = "temp-secret-credential";

    public static final String INVALID_OSUSER_CRED_NAME = "temp-user-credential";

    public static final String INVALID_OSENDPOINT_CRED_NAME = "temp-endpoint-credential";

    public static final String INVALID_ARMACCESS_CRED_NAME = "temp-access-credential";

    public static final String INVALID_ARMSECRET_CRED_NAME = "temp-secret-credential";

    public static final String INVALID_ARMTENANT_CRED_NAME = "temp-tenant-credential";

    public static final String INVALID_ARMSUBSCRIPTION_CRED_NAME = "temp-subscription-credential";

    public static final String INVALID_GCP12_CRED_NAME = "temp-p12-credential";

    public static final String INVALID_GCPROJECT_CRED_NAME = "temp-project-credential";

    public static final String INVALID_GCPSERVICEACCOUNT_CRED_NAME = "temp-serviceacc-credential";

    public static final String CRED_DESCRIPTION = "temporary credential for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialProviderSpecTests.class);

    private final OpenstackCloudProvider openstackCloudProvider = new OpenstackCloudProvider(getTestParameter());

    private final AwsCloudProvider awsCloudProvider = new AwsCloudProvider(getTestParameter());

    private final AzureCloudProvider azureCloudProvider = new AzureCloudProvider(getTestParameter());

    private final GcpCloudProvider gcpCloudProvider = new GcpCloudProvider(getTestParameter());

    private String errorMessage = "";

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_AWSKEY_CRED_NAME, VALID_OSV3_CRED_NAME};

        for (int i = 0; i < nameArray.length; i++) {
            LOGGER.info("Delete credential: \'{}\'", nameArray[i].toLowerCase().trim());
            try {
                given(CloudbreakClient.isCreated());
                given(Credential.request()
                        .withName(nameArray[i]));
                when(Credential.delete());
            } catch (ForbiddenException e) {
                String exceptionMessage = e.getResponse().readEntity(String.class);
                this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
                LOGGER.info("ForbiddenException message ::: " + this.errorMessage);
            }
        }
    }

    @Test(groups = { "os" })
    public void testCreateValidOSV3Credential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(VALID_OSV3_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackV3CredentialDetails()));
        when(Credential.post());
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertEquals(credential.getResponse().getName(), VALID_OSV3_CRED_NAME);
                })
        );
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "os" })
    public void testCreateInvalidOSUserCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_OSUSER_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackCredentialDetailsInvalidUser()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "os" })
    public void testCreateInvalidOSEndpointCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_OSENDPOINT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackCredentialDetailsInvalidEndpoint()));
        when(Credential.post());
    }

    @Test(groups = { "aws" })
    public void testCreateValidAWSKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(VALID_AWSKEY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsKey()));
        when(Credential.post());
        then(Credential.assertThis(
                (credential, t) -> {
                    Assert.assertEquals(credential.getResponse().getName(), VALID_AWSKEY_CRED_NAME);
                })
        );
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "aws" })
    public void testCreateInvalidAWSAccessKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSACCESS_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidAccessKey()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "aws" })
    public void testCreateInvalidAWSSecrectKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSSECRET_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidSecretKey()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "aws" })
    public void testCreateInvalidAWSRoleARNCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSROLE_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidArn()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "azure" })
    public void testCreateInvalidAzureSecretKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMSECRET_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidSecretKey()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "azure" })
    public void testCreateInvalidAzureAccessKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMACCESS_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidAccessKey()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "azure" })
    public void testCreateInvalidAzureInvalidTenantIDCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMTENANT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidTenantID()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "azure" })
    public void testCreateInvalidAzureSubscriptionIDCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMSUBSCRIPTION_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidSubscriptionID()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "gcp" })
    public void testCreateEmptyGCPP12FileCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCP12_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyP12File()));
        when(Credential.post());
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "gcp" })
    public void testCreateEmptyGCPProjectIdCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCPROJECT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyProjectId()));
        when(Credential.post());
    }

    // BUG-96615
    // GCP Credential Creation: serviceAccountId validation is missing on API
    @Test(expectedExceptions = BadRequestException.class, enabled = false, groups = { "gcp" })
    public void testCreateEmptyGCPServiceAccountCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCPSERVICEACCOUNT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyServiceAccount()));
        when(Credential.post());
    }

}
