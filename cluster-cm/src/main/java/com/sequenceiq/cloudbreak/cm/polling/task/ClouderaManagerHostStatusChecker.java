package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public class ClouderaManagerHostStatusChecker extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostStatusChecker.class);

    private static final String VIEW_TYPE = "FULL";

    private final Instant start;

    private final List<String> targets;

    private Set<Long> notKnownInstanceIds;

    private boolean resilientPollerEnabled;

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, List<String> targets, boolean resilientPollerEnabled) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        start = Instant.now();
        this.targets = targets;
        this.resilientPollerEnabled = resilientPollerEnabled;
    }

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, boolean resilientPollerEnabled) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        start = Instant.now();
        this.targets = List.of();
        this.resilientPollerEnabled = resilientPollerEnabled;
    }

    @Override
    public Set<Long> getFailedInstancePrivateIds() {
        return notKnownInstanceIds;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) throws ApiException {
        List<String> hostIpsFromManager = fetchHeartbeatedHostIpsFromManager(pollerObject);
        List<InstanceMetadataView> notKnownInstancesByManager = collectNotKnownInstancesByManager(pollerObject, hostIpsFromManager);
        if (!notKnownInstancesByManager.isEmpty()) {
            notKnownInstanceIds = notKnownInstancesByManager.stream()
                    .map(InstanceMetadataView::getId)
                    .collect(Collectors.toSet());
            LOGGER.warn("there are missing nodes from cloudera manager, not known instances: {}", notKnownInstanceIds);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject pollerObject) {
        if (!resilientPollerEnabled) {
            super.handleTimeout(pollerObject);
        }
    }

    private List<InstanceMetadataView> collectNotKnownInstancesByManager(ClouderaManagerPollerObject pollerObject, List<String> hostIpsFromManager) {
        return pollerObject.getStack().getNotTerminatedInstanceMetaData().stream()
                .filter(metaData -> metaData.getDiscoveryFQDN() != null)
                .filter(InstanceMetadataView::isReachable)
                .filter(instance -> !InstanceStatus.SERVICES_UNHEALTHY.equals(instance.getInstanceStatus()))
                .filter(md -> CollectionUtils.isEmpty(targets) || targets.contains(md.getPrivateIp()))
                .filter(metaData -> !hostIpsFromManager.contains(metaData.getPrivateIp()))
                .collect(Collectors.toList());
    }

    private List<String> fetchHeartbeatedHostIpsFromManager(ClouderaManagerPollerObject pollerObject) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiPojoFactory.getHostsResourceApi(pollerObject.getApiClient());
        ApiHostList hostList = hostsResourceApi.readHosts(null, null, VIEW_TYPE);
        List<String> hostIpsFromManager = filterForHeartBeatedIps(hostList);
        LOGGER.debug("Hosts in the list from manager: " + hostIpsFromManager);
        return hostIpsFromManager;
    }

    private List<String> filterForHeartBeatedIps(ApiHostList hostList) {
        return hostList.getItems().stream()
                .filter(item -> StringUtils.isNotBlank(item.getLastHeartbeat()))
                .filter(item -> start.isBefore(Instant.parse(item.getLastHeartbeat())))
                .map(ApiHost::getIpAddress)
                .collect(Collectors.toList());
    }

    @Override
    protected String getPollingName() {
        return "Host status summary";
    }
}
