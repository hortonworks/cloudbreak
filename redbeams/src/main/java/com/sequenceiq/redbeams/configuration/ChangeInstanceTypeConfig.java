package com.sequenceiq.redbeams.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class ChangeInstanceTypeConfig {

    @Value("${redbeams.change-instance-type.polling-interval-in-seconds:30}")
    private int pollingIntervalInSeconds;

    @Value("${redbeams.change-instance-type.timeout-in-minutes.aws:30}")
    private int awsTimeoutInMinutes;

    @Value("${redbeams.change-instance-type.timeout-in-minutes.azure-ha:30}")
    private int azureHaTimeoutInMinutes;

    @Value("${redbeams.change-instance-type.timeout-in-minutes.azure:15}")
    private int azureTimeoutInMinutes;

    @Value("${redbeams.change-instance-type.timeout-in-minutes.gcp:15}")
    private int gcpTimeoutInMinutes;

    @Value("${redbeams.change-instance-type.timeout-in-minutes.default:30}")
    private int defaultTimeoutInMinutes;

    public Duration getPollingInterval() {
        return Duration.ofSeconds(pollingIntervalInSeconds);
    }

    public Duration getTimeout(String cloudPlatform, boolean highAvailability) {
        int timeoutInMinutes;
        if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
            timeoutInMinutes = awsTimeoutInMinutes;
        } else if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            timeoutInMinutes = highAvailability ? azureHaTimeoutInMinutes : azureTimeoutInMinutes;
        } else if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform)) {
            timeoutInMinutes = gcpTimeoutInMinutes;
        } else {
            timeoutInMinutes = defaultTimeoutInMinutes;
        }
        return Duration.ofMinutes(timeoutInMinutes);
    }
}
