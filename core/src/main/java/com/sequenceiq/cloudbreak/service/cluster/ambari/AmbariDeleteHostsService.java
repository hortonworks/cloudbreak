package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostStatus;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientRetryer;

@Service
public class AmbariDeleteHostsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDeleteHostsService.class);

    @Inject
    private AmbariClientRetryer ambariClientRetryer;

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteHostsButFirstQueryThemFromAmbari(AmbariClient ambariClient, List<String> hosts) throws IOException, URISyntaxException {
        LOGGER.info("Query hosts from abmari: {}", hosts);
        Map<String, HostStatus> hostStatusMap = ambariClientRetryer.getHostsStatuses(ambariClient, hosts);
        if (hostStatusMap != null && !hostStatusMap.isEmpty()) {
            LOGGER.info("Host status map from abmari is not empty, try to delete them: {}", hostStatusMap);
            ambariClientRetryer.deleteHosts(ambariClient, new ArrayList<>(hostStatusMap.keySet()));
        }
    }
}
