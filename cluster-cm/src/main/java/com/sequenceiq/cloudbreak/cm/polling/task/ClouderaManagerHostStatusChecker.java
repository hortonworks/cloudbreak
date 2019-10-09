package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.stream.Collectors;

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

    public ClouderaManagerHostStatusChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory) {
        super(clouderaManagerApiPojoFactory);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        HostsResourceApi hostsResourceApi = new HostsResourceApi(pollerObject.getApiClient());
        String viewType = "SUMMARY";
        ApiHostList hostList = hostsResourceApi.readHosts(viewType);
        List<String> hostIpsFromManager = hostList.getItems().stream()
                .map(ApiHost::getIpAddress)
                .collect(Collectors.toList());
        LOGGER.debug("Hosts in the list from manager: " + hostIpsFromManager);

        List<InstanceMetaData> notKnownInstancesByManager = pollerObject.getStack().getInstanceMetaDataAsList().stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .filter(metaData -> !hostIpsFromManager.contains(metaData.getPrivateIp()))
                .collect(Collectors.toList());

        if (!notKnownInstancesByManager.isEmpty()) {
            LOGGER.warn("there are missing nodes from cloudera manager, not known instances: {}", notKnownInstancesByManager);
            return false;
        } else {
            return true;
        }
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
