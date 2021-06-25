package com.sequenceiq.cloudbreak.util;

public class DocumentationLinkProvider {

    private DocumentationLinkProvider() {

    }

    public static String awsCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-idbroker-minimum-setup.html";
    }

    public static String awsSshKeySetupLink() {
        return "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-aws-req-ssh.html";
    }

    public static String awsDynamoDbSetupLink() {
        return "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-aws-req-dynamodb.html";
    }

    public static String googleCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp/latest/requirements-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html";
    }

    public static String azureCloudStorageSetupLink() {
        return "https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html";
    }

    public static String azureAddSubnetLink() {
        return "https://docs.cloudera.com/management-console/cloud/environments-azure/topics/mc-subnet-adding-azure.html";
    }

    public static String awsAddSubnetLink() {
        return "https://docs.cloudera.com/management-console/cloud/environments/topics/mc-subnet-adding-azure.html";
    }
}
