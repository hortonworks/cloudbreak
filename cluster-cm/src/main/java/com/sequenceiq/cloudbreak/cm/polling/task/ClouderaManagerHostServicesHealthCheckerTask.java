package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public class ClouderaManagerHostServicesHealthCheckerTask extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostServicesHealthCheckerTask.class);

    private ClouderaManagerHealthService clouderaManagerHealthService;

    private Optional<String> runtimeVersion;

    public ClouderaManagerHostServicesHealthCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory, ClusterEventService clusterEventService,
            ClouderaManagerHealthService clouderaManagerHealthService, Optional<String> runtimeVersion) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.clouderaManagerHealthService = clouderaManagerHealthService;
        this.runtimeVersion = runtimeVersion;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) throws ApiException {
        ExtendedHostStatuses extendedHostStatuses = clouderaManagerHealthService.getExtendedHostStatuses(pollerObject.getApiClient(), runtimeVersion);
        Map<String, Optional<String>> failedHosts = extendedHostStatuses.getHostsHealth().entrySet().stream()
                .filter(e -> !extendedHostStatuses.isHostHealthy(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey().value(), e -> Optional.ofNullable(extendedHostStatuses.statusReasonForHost(e.getKey()))));
        if (!failedHosts.isEmpty()) {
            LOGGER.warn("Failed hosts found: {}", failedHosts);
            return false;
        } else {
            LOGGER.info("No failed hosts found");
            return true;
        }
    }

    @Override
    protected String getPollingName() {
        return "Host services health status";
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject pollerObject) {
        LOGGER.warn("Waiting for healthy host services timed out");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject pollerObject) {
        return String.format("Host services moved into healthy state for stack '%s'", pollerObject.getStack().getId());
    }
}
