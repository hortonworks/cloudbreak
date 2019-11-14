package com.sequenceiq.cloudbreak.ambari;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostStatus;

@Service
public class AmbariDeleteHostsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDeleteHostsService.class);

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteHostsButFirstQueryThemFromAmbari(AmbariClient ambariClient, List<String> hosts) throws IOException, URISyntaxException {
        LOGGER.info("Query hosts from abmari: {}", hosts);
        Map<String, HostStatus> hostStatusMap = ambariClient.getHostsStatuses(hosts);
        if (hostStatusMap != null && !hostStatusMap.isEmpty()) {
            LOGGER.info("Host status map from abmari is not empty, try to delete them: {}", hostStatusMap);
            ambariClient.deleteHosts(new ArrayList<>(hostStatusMap.keySet()));
        }
    }
}
