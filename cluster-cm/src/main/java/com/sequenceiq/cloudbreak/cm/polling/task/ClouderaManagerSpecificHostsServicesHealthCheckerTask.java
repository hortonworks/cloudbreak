package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public class ClouderaManagerSpecificHostsServicesHealthCheckerTask extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSpecificHostsServicesHealthCheckerTask.class);

    private final ClouderaManagerHealthService clouderaManagerHealthService;

    private final Optional<String> runtimeVersion;

    private final Set<String> hostnamesToCheckFor;

    private final Set<InstanceMetadataView> hostsToCheckFor;

    private final Set<Long> failedHostPrivateIds = new HashSet<>();

    public ClouderaManagerSpecificHostsServicesHealthCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ClouderaManagerHealthService clouderaManagerHealthService,
            Optional<String> runtimeVersion, Set<InstanceMetadataView> hostsToCheckFor) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.clouderaManagerHealthService = clouderaManagerHealthService;
        this.runtimeVersion = runtimeVersion;
        this.hostsToCheckFor = hostsToCheckFor;
        this.hostnamesToCheckFor = hostsToCheckFor.stream()
                .filter(host -> host.getDiscoveryFQDN() != null)
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Initialized ClouderaManagerSpecificHostsServicesHealthCheckerTask with {} hosts to check: {}",
                hostnamesToCheckFor.size(), hostnamesToCheckFor);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) throws ApiException {
        failedHostPrivateIds.clear();

        ExtendedHostStatuses extendedHostStatuses = clouderaManagerHealthService.getExtendedHostStatuses(
                pollerObject.getApiClient(), runtimeVersion);

        Map<String, Optional<String>> failedHosts = extendedHostStatuses.getHostsHealth().entrySet().stream()
                .filter(e -> hostnamesToCheckFor.contains(e.getKey().value()))
                .filter(e -> !extendedHostStatuses.isHostHealthy(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey().value(),
                        e -> Optional.ofNullable(extendedHostStatuses.statusReasonForHost(e.getKey()))));

        if (!failedHosts.isEmpty()) {
            failedHostPrivateIds.addAll(resolveInstanceIdsForHosts(failedHosts.keySet()));
            LOGGER.warn("Failed hosts found among the specified hosts: {}", failedHosts);
            return false;
        } else {
            Set<String> hostsInCm = extendedHostStatuses.getHostsHealth().keySet().stream()
                    .map(hostName -> hostName.value())
                    .collect(Collectors.toSet());

            Set<String> missingHosts = hostnamesToCheckFor.stream()
                    .filter(hostname -> !hostsInCm.contains(hostname))
                    .collect(Collectors.toSet());

            if (!missingHosts.isEmpty()) {
                failedHostPrivateIds.addAll(resolveInstanceIdsForHosts(missingHosts));
                LOGGER.warn("Some specified hosts are not yet registered in Cloudera Manager: {}", missingHosts);
                return false;
            }

            LOGGER.info("All specified hosts ({}) are healthy", hostnamesToCheckFor.size());
            return true;
        }
    }

    @Override
    public Set<Long> getFailedInstancePrivateIds() {
        return new HashSet<>(failedHostPrivateIds);
    }

    @Override
    protected String getPollingName() {
        return "Specific hosts services health status";
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject pollerObject) {
        LOGGER.warn("Waiting for healthy host services timed out for hosts: {}", hostnamesToCheckFor);
    }

    private Set<Long> resolveInstanceIdsForHosts(Set<String> hostnames) {
        return hostsToCheckFor.stream()
                .filter(host -> host.getDiscoveryFQDN() != null && hostnames.contains(host.getDiscoveryFQDN()))
                .map(InstanceMetadataView::getPrivateId)
                .collect(Collectors.toSet());
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject pollerObject) {
        return String.format("Host services moved into healthy state for %d specified hosts in stack '%s'",
                hostnamesToCheckFor.size(), pollerObject.getStack().getId());
    }
}

