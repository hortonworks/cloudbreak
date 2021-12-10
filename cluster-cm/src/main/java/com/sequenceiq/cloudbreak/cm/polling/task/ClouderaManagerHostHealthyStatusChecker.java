package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommissionState;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerHostHealthyStatusChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostHealthyStatusChecker.class);

    private static final String VIEW_TYPE = "FULL";

    private final Instant start;

    private final int initialNodeCount;

    private final Set<String> hostnamesToCheckFor;

    public ClouderaManagerHostHealthyStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, Set<String> hostnamesToCheckFor) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        // TODO CB-15132: Introduce a threshold on this polling. Return hosts which have moved into a good state within a time window.
        start = Instant.now();
        this.hostnamesToCheckFor = new HashSet<>(hostnamesToCheckFor);
        initialNodeCount = hostnamesToCheckFor.size();
        LOGGER.info("Initialized ClouderaManagerHostHealthyStatusChecker with start={}, hostNamesToCheckFor.size()={}", start, hostnamesToCheckFor.size());
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        Set<String> goodHostsFromManager = fetchGoodHostsFromManager(pollerObject);

        int pre = hostnamesToCheckFor.size();
        hostnamesToCheckFor.stream().filter(h -> goodHostsFromManager.contains(h)).forEach(hostnamesToCheckFor::remove);
        int post = hostnamesToCheckFor.size();
        LOGGER.debug("NumHostsFoundToBeHealthy={}, pendingHostCount={}", post - pre, post);

        return hostnamesToCheckFor.size() == 0;
    }

    private Set<String> fetchGoodHostsFromManager(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiPojoFactory.getHostsResourceApi(pollerObject.getApiClient());
        ApiHostList hostList = hostsResourceApi.readHosts(null, null, VIEW_TYPE);
        return filterForHealthy(hostList);
    }

    private Set<String> filterForHealthy(ApiHostList hostList) {
        // Recent heartbeat, GOOD_HEALTH
        Set<String> goodHealthSet = new HashSet<>();
        for (ApiHost apiHost : hostList.getItems()) {
            String hostname = apiHost.getHostname();
            Instant lastheatbeat = null;
            if (StringUtils.isNotBlank(apiHost.getLastHeartbeat())) {
                lastheatbeat = Instant.parse(apiHost.getLastHeartbeat());
            }
            ApiHealthSummary healthSummary = apiHost.getHealthSummary();
            boolean inMaintenance = apiHost.getMaintenanceMode();
            ApiCommissionState commissionState = apiHost.getCommissionState();
            LOGGER.trace("CM info for: [{}]: lastHeatbeat={}, lastHeartbeatInstant={}, healthSummary={}, commissionState={}, maint={}",
                    hostname, apiHost.getLastHeartbeat(), lastheatbeat, healthSummary, commissionState, inMaintenance);

            if (lastheatbeat != null && start.isBefore(lastheatbeat) && healthSummary == ApiHealthSummary.GOOD) {
                goodHealthSet.add(hostname);
            }
        }
        LOGGER.info("Found good host count={}", goodHealthSet.size());
        return goodHealthSet;
    }

    @Override
    protected String getCommandName() {
        return "Host status summary";
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException(
                String.format("Operation timed out. Failed while waiting for %d nodes to move into health state. MissingNodeCount=%d, MissingNodes=[%s]",
                        initialNodeCount, hostnamesToCheckFor.size(), hostnamesToCheckFor));
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject pollerObject) {
        return String.format("Hosts (count=%d)moved into healthy state for stack '%s'", initialNodeCount, pollerObject.getStack().getId());
    }

}
