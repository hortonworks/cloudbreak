package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class ClouderaManagerHostStatusChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostStatusChecker.class);

    private static final String VIEW_TYPE = "FULL";

    private final Instant start;

    private final List<String> targets;

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, List<String> targets) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        start = Instant.now();
        this.targets = targets;
    }

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        start = Instant.now();
        this.targets = List.of();
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        List<String> hostIpsFromManager = fetchHeartbeatedHostIpsFromManager(pollerObject);
        List<InstanceMetaData> notKnownInstancesByManager = collectNotKnownInstancesByManager(pollerObject, hostIpsFromManager);
        if (!notKnownInstancesByManager.isEmpty()) {
            LOGGER.warn("there are missing nodes from cloudera manager, not known instances: {}", notKnownInstancesByManager);
            return false;
        } else {
            return true;
        }
    }

    private List<InstanceMetaData> collectNotKnownInstancesByManager(ClouderaManagerCommandPollerObject pollerObject, List<String> hostIpsFromManager) {
        return pollerObject.getStack().getInstanceMetaDataAsList().stream()
                .filter(metaData -> metaData.getDiscoveryFQDN() != null)
                .filter(InstanceMetaData::isReachable)
                .filter(md -> CollectionUtils.isEmpty(targets) || targets.contains(md.getPrivateIp()))
                .filter(metaData -> !hostIpsFromManager.contains(metaData.getPrivateIp()))
                .collect(Collectors.toList());
    }

    private List<String> fetchHeartbeatedHostIpsFromManager(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
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
    protected String getCommandName() {
        return "Host status summary";
    }
}
