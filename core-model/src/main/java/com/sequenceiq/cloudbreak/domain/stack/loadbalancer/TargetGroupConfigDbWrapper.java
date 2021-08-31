package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpTargetGroupConfigDb;

/**
 * A wrapper for the cloud provider specific target group metadata stored in the database. Only one
 * type of config should be set, depending on what the cloud platform for the stack is. This class
 * is intended to be serialized into a JSON string and stored in the database, and deserialized
 * when fetched.
 */
public class TargetGroupConfigDbWrapper {

    private AwsTargetGroupConfigDb awsConfig;

    private AzureTargetGroupConfigDb azureConfig;

    private GcpTargetGroupConfigDb gcpConfig;

    public AwsTargetGroupConfigDb getAwsConfig() {
        return awsConfig;
    }

    public void setAwsConfig(AwsTargetGroupConfigDb awsConfig) {
        this.awsConfig = awsConfig;
    }

    public AzureTargetGroupConfigDb getAzureConfig() {
        return azureConfig;
    }

    public void setAzureConfig(AzureTargetGroupConfigDb azureConfig) {
        this.azureConfig = azureConfig;
    }

    public GcpTargetGroupConfigDb getGcpConfig() {
        return gcpConfig;
    }

    public void setGcpConfig(GcpTargetGroupConfigDb gcpConfig) {
        this.gcpConfig = gcpConfig;
    }

    @Override
    public String toString() {
        return "TargetGroupConfigDbWrapper{" +
            "awsConfig=" + awsConfig +
            "azureConfig=" + azureConfig +
            "gcpConfig=" + gcpConfig +
            '}';
    }
}
