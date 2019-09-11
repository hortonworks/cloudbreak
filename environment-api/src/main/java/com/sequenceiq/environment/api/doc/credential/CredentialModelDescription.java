package com.sequenceiq.environment.api.doc.credential;

public class CredentialModelDescription {

    public static final String AWS_PARAMETERS = "custom parameters for AWS credential";
    public static final String GCP_PARAMETERS = "custom parameters for GCP credential";
    public static final String AZURE_PARAMETERS = "custom parameters for Azure credential";
    public static final String OPENSTACK_PARAMETERS = "custom parameters for Openstack credential";
    public static final String CUMULUS_YARN_PARAMETERS = "custom parameters for Cumulus Yarn credential";
    public static final String YARN_PARAMETERS = "custom parameters for Yarn credential";
    public static final String ACCOUNT_IDENTIFIER = "provider specific identifier of the account/subscription/project that is used by Cloudbreak";
    public static final String AWS_CREDENTIAL_PREREQUISITES = "AWS specific credential prerequisites.";
    public static final String AZURE_CREDENTIAL_PREREQUISITES = "Azure specific credential prerequisites.";
    public static final String GCP_CREDENTIAL_PREREQUISITES = "GCP specific credential prerequisites.";
    public static final String AWS_EXTERNAL_ID = "AWS specific identifier for role based credential creation - "
            + "External id for 'Another AWS account' typed roles.";
    public static final String AWS_POLICY_JSON = "AWS specific JSON file that is base64 encoded and "
            + "describes the necessary AWS policies for cloud resource provisioning.";
    public static final String AZURE_APP_CREATION_COMMAND = "Azure CLI command to create Azure AD Application as prerequisite for credential creation."
            + "The field is base64 encoded.";
    public static final String CODE_GRANT_FLOW_LOGIN_URL = "Login URL for code grant flow";
    public static final String GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND = "GCP specific 'gcloud' CLI based commands to "
            + "create prerequisites for Cloudbreak credential creation. The field is base64 encoded.";
    public static final String CLOUD_PLATFORM = "type of cloud provider";
    public static final String ATTRIBUTES = "provider specific attributes of the credential";
    public static final String VERIFICATION_STATUS_TEXT = "verification status text for credential, if empty then there is no verification issue";
    public static final String CREATOR = "crn of the creator";

    private CredentialModelDescription() {
    }
}
