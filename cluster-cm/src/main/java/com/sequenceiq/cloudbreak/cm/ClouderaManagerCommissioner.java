package com.sequenceiq.cloudbreak.cm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;

@Component
public class ClouderaManagerCommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommissioner.class);

    private static final String SUMMARY_REQUEST_VIEW = "SUMMARY";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    /**
     * Attempts to recommission the provided list of hosts
     * @param stack the stack under consideration
     * @param hostsToRecommission hosts to decommission
     * @param client api client to communicate with ClouderaManager
     * @return the hostnames for hosts which were successfully recommissioned
     */
    public Set<String> recommissionNodes(StackDtoDelegate stack, Map<String, InstanceMetaData> hostsToRecommission, ApiClient client) {

        // TODO CB-15132: Check status of target nodes / previously issued commands - in case of pod restarts etc.
        //  Ideally without needing to persist anything (commandId etc)
        // TODO CB-15132: Deal with situations where CM itself is unavailable. Go back and STOP resources on the cloud-provider.
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        ApiHostList hostRefList;
        try {
            hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
        } catch (ApiException e) {
            LOGGER.error("Failed to communicate with Cloudera Manager", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
        LOGGER.trace("Target recommissionNodes: count={}, hosts=[{}]", hostsToRecommission.size(), hostsToRecommission.keySet());
        LOGGER.debug("hostsAvailableFromCM: count={}, hosts=[{}]", hostRefList.getItems().size(),
                hostRefList.getItems().stream().map(ApiHost::getHostname));

        // Not considering the commission states of the nodes. Nodes could be COMMISSIONED with services in STOPPED state.
        List<String> hostsAvailableForRecommission = hostRefList.getItems().stream()
                .filter(apiHostRef -> hostsToRecommission.containsKey(apiHostRef.getHostname()))
                .parallel()
                .map(ApiHost::getHostname)
                .collect(Collectors.toList());

        Set<String> hostsAvailableForRecommissionSet = new HashSet<>(hostsAvailableForRecommission);
        List<String> cmHostsUnavailableForRecommission = hostsToRecommission.keySet().stream()
                .filter(h -> !hostsAvailableForRecommissionSet.contains(h)).collect(Collectors.toList());

        if (cmHostsUnavailableForRecommission.size() != 0) {
            LOGGER.info("Some recommission targets are unavailable in CM: TotalRecommissionTargetCount={}, unavailableInCMCount={}, unavailableInCM=[{}]",
                    hostsToRecommission.size(), cmHostsUnavailableForRecommission.size(), cmHostsUnavailableForRecommission);
        }

        recommissionHosts(stack, client, hostsAvailableForRecommission);

        return hostsAvailableForRecommission.stream()
                .map(hostsToRecommission::get)
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());

    }

    /**
     * Attempts to recommission the provided list of hosts
     * @param stack the stack under consideration
     * @param hostsToRecommission hosts to recommission
     * @param client api client to communicate with ClouderaManager
     * @throws CloudbreakServiceException when it fails to recommission due to timeout or cancellation
     */
    public void recommissionHosts(StackDtoDelegate stack, ApiClient client, List<String> hostsToRecommission) {
        ClouderaManagerResourceApi apiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);

        ApiHostNameList body = new ApiHostNameList().items(hostsToRecommission);

        ApiCommand apiCommand;
        try {
            apiCommand = apiInstance.hostsRecommissionAndExitMaintenanceModeCommand(body, "recommission_with_start");
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                    .startPollingCmHostsRecommission(stack, client, apiCommand.getId());
            if (pollingResult.isExited()) {
                throw new CancellationException("Cluster was terminated while waiting for host commission");
            } else if (pollingResult.isTimeout()) {
                String warningMessage = "Cloudera Manager recommission host command {} polling timed out, " +
                        "thus we are aborting the recommission operation.";
                abortRecommissionWithWarnMessage(apiCommand, client, warningMessage);
                // TODO CB-15132: What corrective action can be taken in this scenario? We have no idea whether any of the nodes
                //  were recommissioned. Could try checking the status from CM, and take corrective action.
                //  - Could collect list of nodes which are in the expected list and exclude them from corrective action.
                //    (However, this is not trivial since the SERVICE needs to have the nodes recommissioned and that may have failed)
                //  - We could go and STOP all instances, which essentially means re-trying the recommission the next time around.
                throw new CloudbreakServiceException(
                        String.format("Timeout while Cloudera Manager recommissioned hosts. CM command Id: %s", apiCommand.getId()));
            }
        } catch (ApiException e) {
            // TODO CB-15132: Evaluate whether it is possible to figure out if a partial commission succeeded.
            //  Retry / STOP other nodes where it may have failed.
            LOGGER.error("Failed to recommission hosts: {}", hostsToRecommission, e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, InstanceMetaData> collectHostsToCommission(StackDtoDelegate stack, HostGroup hostGroup, Set<String> hostNames, ApiClient client) {
        Set<InstanceMetaData> hostsInHostGroup = hostGroup.getInstanceGroup().getNotTerminatedAndNotZombieInstanceMetaDataSet();
        Map<String, InstanceMetaData> hostsToCommission = hostsInHostGroup.stream()
                .filter(hostMetadata -> hostNames.contains(hostMetadata.getDiscoveryFQDN()))
                // TODO CB-15132: Add additional checks to make sure the hosts are in the expected state. Even better,
                //  in addition to the incoming list, get additional hosts which may be in a 'strange' state in CM,
                //  and see if these can be made part of the upscale operation.
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, hostMetadata -> hostMetadata));
        if (hostsToCommission.size() != hostNames.size()) {
            List<String> missingHosts = hostNames.stream().filter(h -> !hostsToCommission.containsKey(h)).collect(Collectors.toList());
            LOGGER.debug("Not all requested hosts found in CB for host group: {}. MissingCount={}, missingHosts=[{}]. Requested hosts: [{}]",
                    hostGroup.getName(), missingHosts.size(), missingHosts, hostNames);
        }

        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> cmHostNames = hostRefList.getItems().stream()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());

            List<String> matchingCmHosts = hostsToCommission.keySet().stream()
                    .filter(hostName -> cmHostNames.contains(hostName))
                    .collect(Collectors.toList());
            Set<String> matchingCmHostSet = new HashSet<>(matchingCmHosts);

            if (matchingCmHosts.size() != hostsToCommission.size()) {
                List<String> missingHostsInCm = hostsToCommission.keySet().stream().filter(h -> !matchingCmHostSet.contains(h)).collect(Collectors.toList());

                LOGGER.debug("Not all requested hosts found in CM. MissingCount={}, missingHosts=[{}]. Requested hosts: [{}]",
                        missingHostsInCm.size(), missingHostsInCm, hostsToCommission.keySet());
            }

            Sets.newHashSet(hostsToCommission.keySet()).stream()
                    .filter(hostName -> !matchingCmHostSet.contains(hostName))
                    .forEach(hostsToCommission::remove);
            LOGGER.debug("Collected hosts to commission: [{}]", hostsToCommission);
            return hostsToCommission;
        } catch (ApiException e) {
            // TODO: CB-14929: This exception has to be handled properly. Situations where CM communications fails. There's no retries here right now.
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void abortRecommissionWithWarnMessage(ApiCommand apiCommand, ApiClient client, String warningMessage) throws ApiException {
        LOGGER.warn(warningMessage, apiCommand.getId());
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        commandsResourceApi.abortCommand(apiCommand.getId());
    }
}
