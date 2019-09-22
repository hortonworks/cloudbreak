package com.sequenceiq.cloudbreak.service.cluster;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;

@Service
public class AmbariClientRetryer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClientRetryer.class);

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, String> getHostStatuses(AmbariClient ambariClient) {
        LOGGER.info("trying to get host statuses, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.getHostStatuses();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Set<Entry<String, Map<String, String>>> getServiceConfigMapByHostGroup(AmbariClient ambariClient, String hostGroup) {
        LOGGER.info("trying to get service config map by hostgroup, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.getServiceConfigMapByHostGroup(hostGroup).entrySet();
    }

    @Retryable(backoff = @Backoff(delay = 30000), maxAttempts = 5)
    public int startAllServices(AmbariClient ambariClient) throws IOException, URISyntaxException {
        LOGGER.info("trying to start all ambari services, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.startAllServices();
    }

    @Retryable(backoff = @Backoff(delay = 30000), maxAttempts = 5)
    public int stopAllServices(AmbariClient ambariClient) throws IOException, URISyntaxException {
        LOGGER.info("trying to stop all ambari services, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.stopAllServices();
    }

    @Retryable(exclude = AmbariConnectionException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public java.util.Map<String, java.util.Map<String, String>> getHostComponentsStates(AmbariClient ambariClient) {
        LOGGER.info("trying to get components states, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.getHostComponentsStates();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<String> getClusterHosts(AmbariClient ambariClient) {
        LOGGER.info("trying to get cluster hosts, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.getClusterHosts();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, String> getBlueprintsMap(AmbariClient ambariClient) {
        LOGGER.info("trying to get blueprints map, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.getBlueprintsMap();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public int addHostsWithBlueprint(AmbariClient ambariClient, String bpName, String hostGroup, List<String> hosts) throws IOException, URISyntaxException {
        LOGGER.info("trying to add hosts with blueprint, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.addHostsWithBlueprint(bpName, hostGroup, hosts);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public int addHostsAndRackInfoWithBlueprint(AmbariClient ambariClient, String bpName, String hostGroup, Map<String, String> hostsWithRackInfo)
            throws IOException, URISyntaxException {
        LOGGER.info("trying to add hosts and rack info with blueprint, ambari URI: {}", ambariClient.getAmbari().getUri());
        return ambariClient.addHostsAndRackInfoWithBlueprint(bpName, hostGroup, hostsWithRackInfo);
    }
}
