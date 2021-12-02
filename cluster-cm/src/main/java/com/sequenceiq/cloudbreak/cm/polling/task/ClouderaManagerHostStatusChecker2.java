package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class ClouderaManagerHostStatusChecker2 extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostStatusChecker2.class);

    private static final String VIEW_TYPE = "FULL";

    private final Instant start;

    private final Set<String> hostnamesToCheckFor;

    public ClouderaManagerHostStatusChecker2(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, Set<String> hostnamesToCheckFor) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        // TODO CB-14929: Introduce a threshold on this polling. Return hosts which have moed into a good state within a time window.
        start = Instant.now();
        this.hostnamesToCheckFor = new HashSet<>(hostnamesToCheckFor);
        LOGGER.info("ZZZ: Initialized ClouderaManagerHostStatusChecker2 with start={}, hostNamesToCheckFor.size()={}", start, hostnamesToCheckFor.size());
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        Set<String> goodHostsFromManager = fetchGoodHostsFromManager(pollerObject);

        LOGGER.info("ZZZ: Filtering hostlist");
        int pre = hostnamesToCheckFor.size();
        hostnamesToCheckFor.stream().filter(h -> goodHostsFromManager.contains(h)).forEach(hostnamesToCheckFor::remove);
        int post = hostnamesToCheckFor.size();
        LOGGER.info("ZZZ: hostsToCheckFor presize={}, postSize={}", pre, post);

        return hostnamesToCheckFor.size() == 0;
    }

    private List<InstanceMetaData> collectNotKnownInstancesByManager(ClouderaManagerCommandPollerObject pollerObject, List<String> hostIpsFromManager) {
        return pollerObject.getStack().getInstanceMetaDataAsList().stream()
                .filter(metaData -> metaData.getDiscoveryFQDN() != null)
                .filter(InstanceMetaData::isReachable)
                .filter(metaData -> !hostIpsFromManager.contains(metaData.getPrivateIp()))
                .collect(Collectors.toList());
    }

    private Set<String> fetchGoodHostsFromManager(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiPojoFactory.getHostsResourceApi(pollerObject.getApiClient());
        ApiHostList hostList = hostsResourceApi.readHosts(null, null, VIEW_TYPE);
        return filterForHealthy(hostList);
    }

    private Set<String> filterForHealthy(ApiHostList hostList) {
        // Recent heartbeat, GOOD_HEALTH
        // TODO CB-14929: fqdn being null will break this. Protect against such scenarios.
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
            LOGGER.debug("ZZZ: CM info for: [{}]: lastHeatbeat={}, lastHeartbeatInstant={}, healthSummary={}, commissionState={}, maint={}",
                    hostname, apiHost.getLastHeartbeat(), lastheatbeat, healthSummary, commissionState, inMaintenance);

            if (lastheatbeat != null && start.isBefore(lastheatbeat) && healthSummary == ApiHealthSummary.GOOD) {
                goodHealthSet.add(hostname);
            }
        }
        LOGGER.info("ZZZ: Found good host count={}", goodHealthSet.size());
        return goodHealthSet;
    }

    @Override
    protected String getCommandName() {
        return "Host status summary";
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to check cloudera manager startup.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject pollerObject) {
        return String.format("Cloudera Manager client found all hosts for stack '%s'", pollerObject.getStack().getId());
    }

}
