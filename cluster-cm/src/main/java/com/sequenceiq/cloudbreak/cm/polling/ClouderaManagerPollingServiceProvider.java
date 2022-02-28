package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerApiCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandListCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerBatchCommandsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDefaultListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostHealthyStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelDeletedListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelStatusListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelsApiListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerSingleParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartupListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerSyncApiCommandIdCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerTemplateInstallationChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDistributeListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDownloadListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.NoExceptionOnTimeoutClouderaManagerListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.SilentCMDecommissionHostListenerTask;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;

@Service
public class ClouderaManagerPollingServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingServiceProvider.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int INFINITE_ATTEMPT = -1;

    private static final long POLL_FOR_15_MINUTES = TimeUnit.MINUTES.toSeconds(15);

    private static final long POLL_FOR_5_MINUTES = TimeUnit.MINUTES.toSeconds(5);

    private static final long POLL_FOR_10_MINUTES = TimeUnit.MINUTES.toSeconds(10);

    private static final long POLL_FOR_ONE_HOUR = TimeUnit.HOURS.toSeconds(1);

    private static final long POLL_FOR_TWO_HOURS = TimeUnit.HOURS.toSeconds(2);

    @Value("${poller.resilient.enabled:false}")
    private boolean resilientPollerEnabled;

    @Inject
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Inject
    private PollingService<ClouderaManagerPollerObject> clouderaManagerPollerObjectPollingService;

    @Inject
    private PollingService<ClouderaManagerCommandPollerObject> clouderaManagerCommandPollerObjectPollingService;

    @Inject
    private PollingService<ClouderaManagerCommandListPollerObject> clouderaManagerCommandListPollerObjectPollingService;

    @Inject
    private ClusterEventService clusterEventService;

    public ExtendedPollingResult startPollingCmStartup(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager startup. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_ONE_HOUR,
                new ClouderaManagerStartupListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startPollingCmHostStatusHealthy(Stack stack, ApiClient apiClient, Set<String> hostnamesToWaitFor) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect and become healthy. NodeCount={}, Nodes=[{}], [Server address: {}]",
                hostnamesToWaitFor.size(), hostnamesToWaitFor, stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_5_MINUTES,
                new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostnamesToWaitFor));
    }

    public ExtendedPollingResult startPollingCmHostStatus(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_ONE_HOUR,
                new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, resilientPollerEnabled));
    }

    public ExtendedPollingResult startPollingCmHostStatus(Stack stack, ApiClient apiClient, List<String> targets) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}, target hosts: {}]",
                stack.getClusterManagerIp(), Joiner.on(",").join(CollectionUtils.emptyIfNull(targets)));
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_ONE_HOUR,
                new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, targets, resilientPollerEnabled));
    }

    public ExtendedPollingResult startPollingCmTemplateInstallation(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to install template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerTemplateInstallationChecker(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startDefaultPolling(Stack stack, ApiClient apiClient, BigDecimal commandId, String commandName) {
        LOGGER.debug("Waiting for Cloudera Manager for [{}]. [Server address: {}]", commandName, stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, commandName));
    }

    public ExtendedPollingResult startPollingCmParcelRepositoryRefresh(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Parcel repo sync");
    }

    public ExtendedPollingResult startPollingCmShutdown(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Stop services");
    }

    public ExtendedPollingResult startPollingCmStartup(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Service start");
    }

    public ExtendedPollingResult startPollingCmKerberosJob(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Configure for kerberos");
    }

    public ExtendedPollingResult startPollingCmParcelActivation(Stack stack, ApiClient apiClient, BigDecimal commandId, List<ClouderaManagerProduct> products) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configurations. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, products));
    }

    public ExtendedPollingResult startPollingCmSingleParcelActivation(Stack stack, ApiClient apiClient, BigDecimal commandId, ClouderaManagerProduct product) {
        LOGGER.debug("Waiting for Cloudera Manager to activate {} parcel. [Server address: {}]", product.getName(), stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerSingleParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, product));
    }

    public ExtendedPollingResult startPollingCmParcelStatus(Stack stack, ApiClient apiClient, Multimap<String, String> parcelVersions,
            ParcelStatus parcelStatus) {
        LOGGER.debug("Waiting for Cloudera Manager parcels {} to become to status [{}]. [Server address: {}]", parcelVersions, parcelStatus,
                stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, BigDecimal.ZERO, POLL_FOR_ONE_HOUR,
                new ClouderaManagerParcelStatusListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelVersions, parcelStatus));
    }

    public ExtendedPollingResult startPollingCmParcelDelete(Stack stack, ApiClient apiClient, Multimap<String, String> parcelVersions) {
        LOGGER.debug("Waiting for Cloudera Manager parcels {} to be deleted. [Server address: {}]", parcelVersions,
                stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, BigDecimal.ZERO, POLL_FOR_ONE_HOUR,
                new ClouderaManagerParcelDeletedListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelVersions));
    }

    public ExtendedPollingResult startPollingCmClientConfigDeployment(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Deploy client configurations");
    }

    public ExtendedPollingResult startPollingCmConfigurationRefresh(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Refresh cluster");
    }

    public ExtendedPollingResult startPollingCmApplyHostTemplate(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Apply host template");
    }

    public ExtendedPollingResult startPollingCmHostsRecommission(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to re-commission host with commandId: {}. [Server address: {}]", commandId, stack.getClusterManagerIp());
        long timeout = POLL_FOR_10_MINUTES;
        return pollCommandWithTimeListener(stack, apiClient, commandId, timeout,
                new NoExceptionOnTimeoutClouderaManagerListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "RecommissionHosts"));
    }

    public ExtendedPollingResult startPollingCmHostsDecommission(Stack stack, ApiClient apiClient, BigDecimal commandId, long pollingTimeout) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, pollingTimeout,
                new NoExceptionOnTimeoutClouderaManagerListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "DecommissionHosts"));
    }

    public ExtendedPollingResult startPollingCmHostDecommissioning(Stack stack, ApiClient apiClient, BigDecimal commandId,
            boolean onlyLostNodesAffected, int removableHostsCount) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getClusterManagerIp());
        if (onlyLostNodesAffected) {
            long timeout = POLL_FOR_10_MINUTES + removableHostsCount * POLL_FOR_5_MINUTES;
            LOGGER.info("Cloudera Manager decommission host command will have {} minutes timeout, " +
                    "since all affected nodes are already deleted from provider side.", TimeUnit.SECONDS.toMinutes(timeout));
            return pollCommandWithTimeListener(stack, apiClient, commandId, timeout,
                    new SilentCMDecommissionHostListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
        } else {
            return pollCommandWithAttemptListener(stack, apiClient, commandId, INFINITE_ATTEMPT,
                    new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "Decommission host"));
        }
    }

    public ExtendedPollingResult startPollingCmManagementServiceStartup(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to start management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService,
                        "Start Cloudera Manager management service"));
    }

    public ExtendedPollingResult startPollingCmManagementServiceShutdown(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to stop management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService,
                        "Stop Cloudera Manager management service"));
    }

    public ExtendedPollingResult startPollingCmServicesRestart(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Restart services");
    }

    public ExtendedPollingResult startPollingParcelsApiAvailable(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Parcels API to become available. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_ONE_HOUR,
                new ClouderaManagerParcelsApiListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startPollingCdpRuntimeUpgrade(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Upgrade CDP Runtime services");
    }

    public ExtendedPollingResult startPollingCdpRuntimeParcelDownload(Stack stack, ApiClient apiClient, BigDecimal commandId, ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to download CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_TWO_HOURS,
                new ClouderaManagerUpgradeParcelDownloadListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource));
    }

    public ExtendedPollingResult startPollingCdpRuntimeParcelDistribute(Stack stack, ApiClient apiClient, BigDecimal commandId, ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to distribute CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerUpgradeParcelDistributeListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource));
    }

    public ExtendedPollingResult startPollingCmGenerateCredentials(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Generate Credentials");
    }

    public ExtendedPollingResult startPollingCollectDiagnostics(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Collect diagnostics");
    }

    public ExtendedPollingResult startPollingRemoveHostsFromCluster(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to finish removal of hosts from cluster. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_FOR_ONE_HOUR,
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "Remove hosts from cluster"));
    }

    public ExtendedPollingResult startPollingCommandList(Stack stack, ApiClient apiClient, List<BigDecimal> commandIds, String commandName) {
        LOGGER.debug("Waiting for Cloudera Manager to finish the following commands: {}. [Server address: {}]", commandIds, stack.getClusterManagerIp());
        return pollCommandListWithTimeListener(stack, apiClient, commandIds, POLL_FOR_ONE_HOUR,
                new ClouderaManagerBatchCommandsListenerTask(clouderaManagerApiPojoFactory, clusterEventService, commandName));
    }

    public ExtendedPollingResult checkSyncApiCommandId(Stack stack, ApiClient apiClient, String commandName, BigDecimal recentCommandId,
            SyncApiCommandRetriever syncApiCommandRetriever) {
        LOGGER.debug("Waiting for Cloudera Manager until it will have a new deploy cluster client config command id. [Server address: {}]",
                stack.getClusterManagerIp());
        ClouderaManagerSyncCommandPollerObject pollerObject = new ClouderaManagerSyncCommandPollerObject(stack, apiClient, recentCommandId, commandName);
        ClouderaManagerSyncApiCommandIdCheckerTask listenerTask =
                new ClouderaManagerSyncApiCommandIdCheckerTask(clouderaManagerApiPojoFactory, syncApiCommandRetriever, clusterEventService);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                pollerObject,
                POLL_INTERVAL,
                POLL_FOR_15_MINUTES);
    }

    private ExtendedPollingResult pollCommandListWithTimeListener(Stack stack, ApiClient apiClient, List<BigDecimal> commandIds, long maximumWaitTimeInSeconds,
            AbstractClouderaManagerCommandListCheckerTask<ClouderaManagerCommandListPollerObject> listenerTask) {
        ClouderaManagerCommandListPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandListPollerObject(stack, apiClient, commandIds);
        return clouderaManagerCommandListPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerCommandPollerObject,
                POLL_INTERVAL,
                maximumWaitTimeInSeconds);
    }

    private ExtendedPollingResult pollApiWithTimeListener(Stack stack, ApiClient apiClient, long maximumWaitTimeInSeconds,
            AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                maximumWaitTimeInSeconds);
    }

    private ExtendedPollingResult pollCommandWithTimeListener(Stack stack, ApiClient apiClient, BigDecimal commandId, long maximumWaitTimeInSeconds,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> listenerTask) {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerCommandPollerObject,
                POLL_INTERVAL,
                maximumWaitTimeInSeconds);
    }

    private ExtendedPollingResult pollCommandWithAttemptListener(Stack stack, ApiClient apiClient, BigDecimal commandId, int numAttempts,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> listenerTask) {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAttempt(
                listenerTask,
                clouderaManagerCommandPollerObject,
                POLL_INTERVAL,
                numAttempts);
    }
}
