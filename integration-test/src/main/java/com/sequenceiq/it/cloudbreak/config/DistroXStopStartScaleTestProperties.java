package com.sequenceiq.it.cloudbreak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
@ConfigurationProperties(prefix = "distroxstopstartscaletest.config")
public class DistroXStopStartScaleTestProperties {
    private String hostGroup;

    private int scaleUpTarget;

    private int scaleDownTarget;

    private int testingTimes;

    private int awsScalingTime;

    private int azureScalingTime;

    private int gcpScalingTime;

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public int getScaleUpTarget() {
        return scaleUpTarget;
    }

    public void setScaleUpTarget(int scaleUpTarget) {
        this.scaleUpTarget = scaleUpTarget;
    }

    public int getScaleDownTarget() {
        return scaleDownTarget;
    }

    public void setScaleDownTarget(int scaleDownTarget) {
        this.scaleDownTarget = scaleDownTarget;
    }

    public int getTestingTimes() {
        return testingTimes;
    }

    public void setTestingTimes(int testingTimes) {
        this.testingTimes = testingTimes;
    }

    public int getAwsScalingTime() {
        return awsScalingTime;
    }

    public void setAwsScalingTime(int awsScalingTime) {
        this.awsScalingTime = awsScalingTime;
    }

    public int getAzureScalingTime() {
        return azureScalingTime;
    }

    public void setAzureScalingTime(int azureScalingTime) {
        this.azureScalingTime = azureScalingTime;
    }

    public int getGcpScalingTime() {
        return gcpScalingTime;
    }

    public void setGcpScalingTime(int gcpScalingTime) {
        this.gcpScalingTime = gcpScalingTime;
    }

    public int getScalingTimeByProvider(String provider) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(provider)) {
            return azureScalingTime;
        } else if (CloudPlatform.GCP.equalsIgnoreCase(provider)) {
            return gcpScalingTime;
        } else if (CloudPlatform.AWS.equalsIgnoreCase(provider)) {
            return awsScalingTime;
        } else {
            throw new IllegalArgumentException("Unsupported cloud provider: " + provider);
        }
    }
}
