package com.sequenceiq.cloudbreak.cloud.doc;

public class CredentialPrerequisiteModelDescription {
    public static final String CLOUD_PLATFORM = "type of cloud provider";
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
    public static final String GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND = "GCP specific 'gcloud' CLI based commands to "
            + "create prerequisites for Cloudbreak credential creation. The field is base64 encoded.";

    private CredentialPrerequisiteModelDescription() {
    }
}
