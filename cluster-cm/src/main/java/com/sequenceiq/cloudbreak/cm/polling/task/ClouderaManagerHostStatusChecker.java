package com.sequenceiq.cloudbreak.cm.polling.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class ClouderaManagerHostStatusChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostStatusChecker.class);

    private static final String VIEW_TYPE = "FULL";

    private final Instant start;

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory) {
        super(clouderaManagerApiPojoFactory);
        start = Instant.now();
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        List<String> hostIpsFromManager = fetchHeartbeatedHostIpsFromManager(pollerObject);
        List<InstanceMetaData> notKnownInstancesByManager = collectNotKnownInstancesByManager(pollerObject, hostIpsFromManager);
        if (!notKnownInstancesByManager.isEmpty()) {
            LOGGER.warn("there are missing nodes from cloudera manager, not known instances: {}", notKnownInstancesByManager);
            return false;
        } else {
            return true;
        }
    }

    private List<InstanceMetaData> collectNotKnownInstancesByManager(ClouderaManagerPollerObject pollerObject, List<String> hostIpsFromManager) {
        return pollerObject.getStack().getInstanceMetaDataAsList().stream()
                    .filter(metaData -> metaData.getDiscoveryFQDN() != null)
                    .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                    .filter(metaData -> !hostIpsFromManager.contains(metaData.getPrivateIp()))
                    .collect(Collectors.toList());
    }

    private List<String> fetchHeartbeatedHostIpsFromManager(ClouderaManagerPollerObject pollerObject) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiPojoFactory.getHostsResourceApi(pollerObject.getApiClient());
        ApiHostList hostList = hostsResourceApi.readHosts(VIEW_TYPE);
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

    @Override
    public void handleTimeout(ClouderaManagerPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to check cloudera manager startup.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject pollerObject) {
        return String.format("Cloudera Manager client found all hosts for stack '%s'", pollerObject.getStack().getId());
    }
}
