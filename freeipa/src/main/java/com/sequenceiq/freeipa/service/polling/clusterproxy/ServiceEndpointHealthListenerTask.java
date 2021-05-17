package com.sequenceiq.freeipa.service.polling.clusterproxy;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigEndpoint;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;

@Component
public class ServiceEndpointHealthListenerTask implements StatusCheckerTask<ServiceEndpointHealthPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointHealthListenerTask.class);

    private static final Set<String> HEALTHY_STATUSES = Set.of("UP");

    @Override
    public boolean checkStatus(ServiceEndpointHealthPollerObject serviceEndpointHealthPollerObject) {
        boolean healthy = false;
        String clusterIdentifer = serviceEndpointHealthPollerObject.getClusterIdentifier();
        LOGGER.debug("Check cluster proxy endpoint health for {}.", clusterIdentifer);
        try {
            ClusterProxyRegistrationClient client = serviceEndpointHealthPollerObject.getClient();
            ReadConfigResponse response = client.readConfig(clusterIdentifer);
            if (response != null) {
                long stillWaitingForHealthyCount = response.getServices().stream()
                        .flatMap(service -> service.getEndpoints().stream())
                        .filter(endpoint -> endpoint.getStatus() != null)
                        .map(ReadConfigEndpoint::getStatus)
                        .filter(status -> !HEALTHY_STATUSES.contains(status))
                        .count();
                healthy = stillWaitingForHealthyCount == 0;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to check cluster proxy endpoint health for {}, error: {}", clusterIdentifer, e.getMessage());
        }
        return healthy;
    }

    @Override
    public void handleTimeout(ServiceEndpointHealthPollerObject clusterProxyHealthPollerObject) {
        String clusterIdentifer = clusterProxyHealthPollerObject.getClusterIdentifier();
        String errorMessage;
        try {
            ClusterProxyRegistrationClient client = clusterProxyHealthPollerObject.getClient();
            errorMessage = client.readConfig(clusterIdentifer).toHumanReadableString();
        } catch (Exception e) {
            errorMessage = String.format("Failed to check cluster proxy endpoint health for %s, error: %s", clusterIdentifer, e.getMessage());
            LOGGER.error(errorMessage);
        }
        throw new CloudbreakServiceException("Operation timed out. Failed to check cluster proxy endpoint health. " + errorMessage);
    }

    @Override
    public String successMessage(ServiceEndpointHealthPollerObject clusterProxyHealthPollerObject) {
        return "Cluster proxy service endpoint is healthy.";
    }

    @Override
    public boolean exitPolling(ServiceEndpointHealthPollerObject clusterProxyHealthPollerObject) {
        return false;
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.error("Cluster proxy error", e);
    }
}
