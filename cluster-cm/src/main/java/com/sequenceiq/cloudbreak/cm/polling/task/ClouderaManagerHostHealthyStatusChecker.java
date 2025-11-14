package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommissionState;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

/**
 * Waits for a CM managed host to heartbeat, and the ApiHealthSummary to go into a Good state.
 * This explicitly ignores maintenance mode and the commissioning state.
 * The intended usage is to reach a point where hosts can be commissioned, and maintenance mode disabled.
 */
public class ClouderaManagerHostHealthyStatusChecker extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> {

    @VisibleForTesting
    static final String VIEW_TYPE = "FULL";

    private static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostHealthyStatusChecker.class);

    @VisibleForTesting
    final Instant start;

    @VisibleForTesting
    final int initialNodeCount;

    @VisibleForTesting
    final Set<String> hostnamesToCheckFor;

    @VisibleForTesting
    final Set<InstanceMetadataView> hostsToCheckFor;

    public ClouderaManagerHostHealthyStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, Set<InstanceMetadataView> hostsToCheckFor) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        // TODO CB-15132: Introduce a threshold on this polling. Return hosts which have moved into a good state within a time window.
        start = Instant.now();
        this.hostnamesToCheckFor = new HashSet<>(hostsToCheckFor.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet()));
        this.hostsToCheckFor = hostsToCheckFor;
        initialNodeCount = hostnamesToCheckFor.size();
        LOGGER.info("Initialized ClouderaManagerHostHealthyStatusChecker with start={}, hostNamesToCheckFor.size()={}", start, hostnamesToCheckFor.size());
    }

    @Override
    public Set<Long> getFailedInstancePrivateIds() {
        Set<Long> failedInstancePrivateIds = new HashSet<>();
        for (String hostNameToCheckFor: hostnamesToCheckFor) {
            for (InstanceMetadataView instanceMetadataView : hostsToCheckFor) {
                if (instanceMetadataView.getDiscoveryFQDN().equals(hostNameToCheckFor)) {
                    failedInstancePrivateIds.add(instanceMetadataView.getPrivateId());
                    break;
                }
            }
        }
        return failedInstancePrivateIds;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) throws ApiException {
        Set<String> goodHostsFromManager = fetchGoodHostsFromManager(pollerObject);

        int pre = hostnamesToCheckFor.size();

        Iterator<String> pendingHostsIter = hostnamesToCheckFor.iterator();
        while (pendingHostsIter.hasNext()) {
            String h = pendingHostsIter.next();
            if (goodHostsFromManager.contains(h)) {
                pendingHostsIter.remove();
            }
        }

        int post = hostnamesToCheckFor.size();
        LOGGER.debug("NumHostsFoundToBeHealthy={}, pendingHostCount={}", post - pre, post);

        return hostnamesToCheckFor.isEmpty();
    }

    private Set<String> fetchGoodHostsFromManager(ClouderaManagerPollerObject pollerObject) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiPojoFactory.getHostsResourceApi(pollerObject.getApiClient());
        ApiHostList hostList = hostsResourceApi.readHosts(null, null, VIEW_TYPE);
        return filterForHealthy(hostList);
    }

    private Set<String> filterForHealthy(ApiHostList hostList) {
        // Recent heartbeat, GOOD_HEALTH
        Set<String> goodHealthSet = new HashSet<>();
        for (ApiHost apiHost : hostList.getItems()) {
            String hostname = apiHost.getHostname();
            Instant lastHeartBeat = null;
            if (StringUtils.isNotBlank(apiHost.getLastHeartbeat())) {
                lastHeartBeat = Instant.parse(apiHost.getLastHeartbeat());
            }
            ApiHealthSummary healthSummary = apiHost.getHealthSummary();
            boolean inMaintenance = Boolean.TRUE.equals(apiHost.isMaintenanceMode());
            ApiCommissionState commissionState = apiHost.getCommissionState();
            LOGGER.trace("CM info for: [{}]: lastHeatbeat={}, lastHeartbeatInstant={}, healthSummary={}, commissionState={}, maint={}",
                    hostname, apiHost.getLastHeartbeat(), lastHeartBeat, healthSummary, commissionState, inMaintenance);

            if (lastHeartBeat != null && start.isBefore(lastHeartBeat)) {
                if (healthSummary == ApiHealthSummary.GOOD) {
                    goodHealthSet.add(hostname);
                } else if (healthSummary == ApiHealthSummary.CONCERNING) {
                    List<String> concerningHealthCheckNames = apiHost.getHealthChecks()
                            .stream()
                            .filter(apiHealthCheck -> apiHealthCheck.getSummary().equals(ApiHealthSummary.CONCERNING))
                            .map(ApiHealthCheck::getName)
                            .toList();
                    if (concerningHealthCheckNames.size() == 1) {
                        if (concerningHealthCheckNames.getFirst().equals(HOST_AGENT_CERTIFICATE_EXPIRY)) {
                            goodHealthSet.add(hostname);
                        }
                    }
                }
            }
        }
        LOGGER.info("Found good host count={}", goodHealthSet.size());
        return goodHealthSet;
    }

    @Override
    protected String getPollingName() {
        return "Host status summary";
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject pollerObject) {
        LOGGER.warn("Operation timed out. Failed while waiting for {} nodes to move into health state. MissingNodeCount={}, MissingNodes={}",
                initialNodeCount, hostnamesToCheckFor.size(), hostnamesToCheckFor);
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject pollerObject) {
        return String.format("Hosts (count=%d)moved into healthy state for stack '%s'", initialNodeCount, pollerObject.getStack().getId());
    }
}
