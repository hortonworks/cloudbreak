package com.sequenceiq.cloudbreak.util;

public class DocumentationLinkProvider {

    private DocumentationLinkProvider() {

    }

    public static String awsCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/" +
                "mc-idbroker-minimum-setup.html#mc-idbroker-minimum-setup";
    }

    public static String awsSshKeySetupLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-aws-req-ssh.html";
    }

    public static String awsDynamoDbSetupLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-aws-req-s3.html";
    }

    public static String googleCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-gcp/topics/" +
                "mc-gcp_minimum_setup_for_cloud_storage.html#mc-gcp_minimum_setup_for_cloud_storage";
    }

    public static String azureCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/" +
                "mc-az-minimal-setup-for-cloud-storage.html#mc-az-minimal-setup-for-cloud-storage";
    }

    public static String azureAddSubnetLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/mc-azure-vnet-and-subnets.html";
    }

    public static String azureFlexibleServerForExistingEnvironmentLink() {
        return "https://docs.cloudera.com/management-console/cloud/environments-azure/topics/" +
                "mc-enable_private_flexible_server_on_an_existing_environment.html";
    }

    public static String awsAddSubnetLink() {
        return "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-aws-req-vpc.html";
    }

    public static String awsS3guardDisableDocumentationLink() {
        return "https://docs.cloudera.com/cdp-public-cloud-preview-features/cloud/disable-s3-guard/disable-s3-guard.pdf";
    }
}
