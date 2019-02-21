package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Service
public class ClouderaManagerHostStatusChecker extends ClusterBasedStatusCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostStatusChecker.class);

    @Override
    public boolean checkStatus(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        ApiClient apiClient = clouderaManagerPollerObject.getApiClient();
        HostsResourceApi hostsResourceApi = new HostsResourceApi(apiClient);
        try {
            String viewType = "SUMMARY";
            ApiHostList hostList = hostsResourceApi.readHosts(viewType);
            List<String> hostNamesFromManager = hostList.getItems().stream()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());
            LOGGER.debug("Hosts in the list from manager: " + hostNamesFromManager);

            List<InstanceMetaData> notKnownInstancesByManager = clouderaManagerPollerObject.getStack().getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> !hostNamesFromManager.contains(instanceMetaData.getDiscoveryFQDN()))
                    .collect(Collectors.toList());

            if (notKnownInstancesByManager.size() > 0) {
                LOGGER.warn("there are missing nodes from cloudera manager, not known instances: {}", notKnownInstancesByManager);
                return false;
            } else {
                return true;
            }
        } catch (ApiException e) {
            LOGGER.info("Can not read host list");
            return false;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to check cloudera manager startup.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        return String.format("Cloudera Manager client found all hosts for stack '%s'", clouderaManagerPollerObject.getStack().getId());
    }
}
