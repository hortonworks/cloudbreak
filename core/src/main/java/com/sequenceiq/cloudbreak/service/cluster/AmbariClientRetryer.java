package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;

@Service
public class AmbariClientRetryer {

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, String> getHostStatuses(AmbariClient ambariClient) {
        return ambariClient.getHostStatuses();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Set<Entry<String, Map<String, String>>> getServiceConfigMapByHostGroup(AmbariClient ambariClient, String hostGroup) {
        return ambariClient.getServiceConfigMapByHostGroup(hostGroup).entrySet();
    }
}
