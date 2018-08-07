package com.sequenceiq.it.cloudbreak;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class CredentialProviderSpecTests extends CloudbreakTest {

    private static final String VALID_AWSKEY_CRED_NAME = "valid-keybased-credential";

    private static final String VALID_OSV3_CRED_NAME = "valid-v3-credential";

    private static final String INVALID_AWSROLE_CRED_NAME = "temp-role-credential";

    private static final String INVALID_AWSACCESS_CRED_NAME = "temp-access-credential";

    private static final String INVALID_AWSSECRET_CRED_NAME = "temp-secret-credential";

    private static final String INVALID_OSUSER_CRED_NAME = "temp-user-credential";

    private static final String INVALID_OSENDPOINT_CRED_NAME = "temp-endpoint-credential";

    private static final String INVALID_ARMACCESS_CRED_NAME = "temp-access-credential";

    private static final String INVALID_ARMSECRET_CRED_NAME = "temp-secret-credential";

    private static final String INVALID_ARMTENANT_CRED_NAME = "temp-tenant-credential";

    private static final String INVALID_ARMSUBSCRIPTION_CRED_NAME = "temp-subscription-credential";

    private static final String INVALID_GCP12_CRED_NAME = "temp-p12-credential";

    private static final String INVALID_GCPROJECT_CRED_NAME = "temp-project-credential";

    private static final String INVALID_GCPSERVICEACCOUNT_CRED_NAME = "temp-serviceacc-credential";

    private static final String CRED_DESCRIPTION = "temporary credential for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialProviderSpecTests.class);

    private final OpenstackCloudProvider openstackCloudProvider = new OpenstackCloudProvider(getTestParameter());

    private final AwsCloudProvider awsCloudProvider = new AwsCloudProvider(getTestParameter());

    private final AzureCloudProvider azureCloudProvider = new AzureCloudProvider(getTestParameter());

    private final GcpCloudProvider gcpCloudProvider = new GcpCloudProvider(getTestParameter());

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_AWSKEY_CRED_NAME, VALID_OSV3_CRED_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete credential: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.isCreated());
                given(Credential.request()
                        .withName(aNameArray));
                when(Credential.delete());
            } catch (ForbiddenException e) {
                String exceptionMessage = e.getResponse().readEntity(String.class);
                String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("ForbiddenException message ::: " + errorMessage);
            }
        }
    }

    @Test(groups = { "credentials", "os" })
    public void testCreateValidOSV3Credential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(VALID_OSV3_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackV3CredentialDetails()), "OpenStack V3 credential request.");
        when(Credential.post(), "OpenStack V3 credential request has been posted.");
        then(Credential.assertThis(
                (credential, t) -> Assert.assertEquals(credential.getResponse().getName(), VALID_OSV3_CRED_NAME)),
                VALID_OSV3_CRED_NAME + " should be the name of the new credential."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "os" })
    public void testCreateInvalidOSUserCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_OSUSER_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackCredentialDetailsInvalidUser()), "OpenStack user is not valid credential request.");
        when(Credential.post(), "OpenStack user is not valid credential request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "os" })
    public void testCreateInvalidOSEndpointCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_OSENDPOINT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(OpenstackCloudProvider.OPENSTACK_CAPITAL)
                .withParameters(openstackCloudProvider.openstackCredentialDetailsInvalidEndpoint()), "OpenStack endpoint is not valid credential request.");
        when(Credential.post(), "OpenStack endpoint is not valid credential request has been posted.");
    }

    @Test(groups = { "credentials", "aws" })
    public void testCreateValidAWSKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(VALID_AWSKEY_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsKey()), "AWS key based credential request.");
        when(Credential.post(), "AWS key based credential request has been posted.");
        then(Credential.assertThis(
                (credential, t) -> Assert.assertEquals(credential.getResponse().getName(), VALID_AWSKEY_CRED_NAME)),
                VALID_AWSKEY_CRED_NAME + " should be the name of the new credential."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "aws" })
    public void testCreateInvalidAWSAccessKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSACCESS_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidAccessKey()), "AWS credential with not valid Access Key request.");
        when(Credential.post(), "AWS credential with not valid Access Key request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "aws" })
    public void testCreateInvalidAWSSecrectKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSSECRET_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidSecretKey()), "AWS credential with not valid Secret Key request.");
        when(Credential.post(), "AWS credential with not valid Secret Key request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "aws" })
    public void testCreateInvalidAWSRoleARNCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_AWSROLE_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AwsCloudProvider.AWS_CAPITAL)
                .withParameters(awsCloudProvider.awsCredentialDetailsInvalidArn()), "AWS credential with not valid Role ARN request.");
        when(Credential.post(), "AWS credential with not valid Role ARN request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "azure" })
    public void testCreateInvalidAzureSecretKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMSECRET_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidSecretKey()), "Azure credential with not valid Secret Key request.");
        when(Credential.post(), "Azure credential with not valid Secret Key request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "azure" })
    public void testCreateInvalidAzureAccessKeyCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMACCESS_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidAccessKey()), "Azure credential with not valid Access Key request.");
        when(Credential.post(), "Azure credential with not valid Access Key request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "azure" })
    public void testCreateInvalidAzureTenantIDCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMTENANT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidTenantID()), "Azure credential with not valid Tenant ID request.");
        when(Credential.post(), "Azure credential with not valid Tenant ID request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "azure" })
    public void testCreateInvalidAzureSubscriptionIDCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_ARMSUBSCRIPTION_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(AzureCloudProvider.AZURE_CAPITAL)
                .withParameters(azureCloudProvider.azureCredentialDetailsInvalidSubscriptionID()), "Azure credential with not valid Subscription ID request.");
        when(Credential.post(), "Azure credential with not valid Subscription ID request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "gcp" })
    public void testCreateEmptyGCPP12FileCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCP12_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyP12File()), "GCP credential with no P12 file request.");
        when(Credential.post(), "GCP credential with no P12 file request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "gcp" })
    public void testCreateEmptyGCPProjectIdCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCPROJECT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyProjectId()), "GCP credential with no Project ID request.");
        when(Credential.post(), "GCP credential with no Project ID request has been posted.");
    }

    // BUG-96615
    // GCP Credential Creation: serviceAccountId validation is missing on API
    @Test(expectedExceptions = BadRequestException.class, groups = { "credentials", "gcp" })
    public void testCreateEmptyGCPServiceAccountCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Credential.request()
                .withName(INVALID_GCPSERVICEACCOUNT_CRED_NAME)
                .withDescription(CRED_DESCRIPTION)
                .withCloudPlatform(GcpCloudProvider.GCP_CAPITAL)
                .withParameters(gcpCloudProvider.gcpCredentialDetailsEmptyServiceAccount()), "GCP credential with no Service Account request.");
        when(Credential.post(), "GCP credential with no Service Account request has been posted.");
    }

}
