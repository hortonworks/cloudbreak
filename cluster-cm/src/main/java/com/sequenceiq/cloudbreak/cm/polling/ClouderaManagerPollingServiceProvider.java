package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHostList;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerApiCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandListCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerBatchCommandsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDecommissionWarningListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDefaultListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostHealthyStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostServicesHealthCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelDeletedListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelStatusListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelsApiListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerServiceDeletionListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerSingleParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartupListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStatusListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerSyncApiCommandIdCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerTemplateInstallationChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDistributeListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDownloadListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.NoExceptionOnTimeoutClouderaManagerListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.SilentCMDecommissionHostListenerTask;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class ClouderaManagerPollingServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingServiceProvider.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int INFINITE_ATTEMPT = -1;

    private static final long POLL_FOR_5_MINUTES = TimeUnit.MINUTES.toSeconds(5);

    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    private static final long POLL_FOR_10_MINUTES = TimeUnit.MINUTES.toSeconds(MAX_CONSECUTIVE_FAILURES);

    private static final int DEFAULT_BACKOFF_NODECOUNT_LIMIT = 50;

    private static final int BACKOFF_NODE_COUNT_MULTIPLIER = 25;

    @Inject
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Inject
    private PollingService<ClouderaManagerPollerObject> clouderaManagerPollerObjectPollingService;

    @Inject
    private PollingService<ClouderaManagerCommandPollerObject> clouderaManagerCommandPollerObjectPollingService;

    @Inject
    private PollingService<ClouderaManagerCommandListPollerObject> clouderaManagerCommandListPollerObjectPollingService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterEventService clusterEventService;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerUpgradePollingTimeoutProvider clouderaManagerUpgradePollingTimeoutProvider;

    public ExtendedPollingResult startPollingCmStartup(StackDtoDelegate stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager startup. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerStartupListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult checkCmStatus(StackDtoDelegate stack, ApiClient apiClient) {
        LOGGER.debug("Check Cloudera Manager status. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithAttemptListener(stack, apiClient, 0,
                new ClouderaManagerStatusListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startPollingCmHostStatusHealthy(StackDtoDelegate stack, ApiClient apiClient, Set<InstanceMetadataView> hostsToWaitFor) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect and become healthy. NodeCount={}, Nodes=[{}], [Server address: {}]",
                hostsToWaitFor.size(), hostsToWaitFor.stream().map(InstanceMetadataView::getDiscoveryFQDN)
                        .toList(), stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_5_MINUTES,
                new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostsToWaitFor));
    }

    public ExtendedPollingResult startPollingCmHostServicesHealthy(StackDtoDelegate stack, ApiClient apiClient,
            ClouderaManagerHealthService clouderaManagerHealthService, Optional<String> runtimeVersion) {
        LOGGER.debug("Waiting for Cloudera Manager services to be in a healthy status.");
        return pollApiWithTimeListener(stack, apiClient, POLL_FOR_5_MINUTES,
                new ClouderaManagerHostServicesHealthCheckerTask(clouderaManagerApiPojoFactory, clusterEventService,
                        clouderaManagerHealthService, runtimeVersion));
    }

    public ExtendedPollingResult startPollingCmHostStatus(StackDtoDelegate stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}]", stack.getClusterManagerIp());
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        return pollApiWithTimeListener(stack, apiClient, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, clusterEventService,
                        entitlementService.targetedUpscaleSupported(accountId)));
    }

    public ExtendedPollingResult startPollingCmHostStatus(StackDtoDelegate stack, ApiClient apiClient, List<String> targets) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}, target hosts: {}]",
                stack.getClusterManagerIp(), Joiner.on(",").join(CollectionUtils.emptyIfNull(targets)));
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        return pollApiWithTimeListener(stack, apiClient, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, targets,
                        entitlementService.targetedUpscaleSupported(accountId)));
    }

    public ExtendedPollingResult startPollingCmTemplateInstallation(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to install template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerTemplateInstallationChecker(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startDefaultPolling(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId, String commandName) {
        LOGGER.debug("Waiting for Cloudera Manager for [{}]. [Server address: {}]", commandName, stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, commandName));
    }

    public ExtendedPollingResult startPollingCmParcelRepositoryRefresh(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Parcel repo sync");
    }

    public ExtendedPollingResult startPollingCmShutdown(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Stop services");
    }

    public ExtendedPollingResult startPollingCmStartup(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Service start");
    }

    public ExtendedPollingResult startPollingServiceStop(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Stop CM service");
    }

    public ExtendedPollingResult startPollingServiceDeletion(StackDtoDelegate stack, ApiClient apiClient, String serviceType) {
        LOGGER.debug("Waiting for Cloudera Manager to delete service {}", serviceType);
        return pollApiWithTimeListener(stack, apiClient, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerServiceDeletionListenerTask(clouderaManagerApiPojoFactory, clusterEventService, serviceType));
    }

    public ExtendedPollingResult startPollingServiceStart(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Start CM service");
    }

    public ExtendedPollingResult startPollingCmKerberosJob(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Configure for kerberos");
    }

    public ExtendedPollingResult startPollingCmParcelActivation(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId,
            List<ClouderaManagerProduct> products) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configurations. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, products));
    }

    public ExtendedPollingResult startPollingCmSingleParcelActivation(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId,
            ClouderaManagerProduct product) {
        LOGGER.debug("Waiting for Cloudera Manager to activate {} parcel. [Server address: {}]", product.getName(), stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerSingleParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, product));
    }

    public ExtendedPollingResult startPollingCmParcelStatus(StackDtoDelegate stack, ApiClient apiClient, Multimap<String, String> parcelVersions,
            ParcelStatus parcelStatus) {
        LOGGER.debug("Waiting for Cloudera Manager parcels {} to become to status [{}]. [Server address: {}]", parcelVersions, parcelStatus,
                stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, BigDecimal.ZERO, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerParcelStatusListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelVersions, parcelStatus));
    }

    public ExtendedPollingResult startPollingCmParcelDelete(StackDtoDelegate stack, ApiClient apiClient, Multimap<String, String> parcelVersions) {
        LOGGER.debug("Waiting for Cloudera Manager parcels {} to be deleted. [Server address: {}]", parcelVersions,
                stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, BigDecimal.ZERO, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerParcelDeletedListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelVersions));
    }

    public ExtendedPollingResult startPollingCmClientConfigDeployment(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Deploy client configurations");
    }

    public ExtendedPollingResult startPollingCmConfigurationRefresh(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Refresh cluster");
    }

    public ExtendedPollingResult startPollingCmApplyHostTemplate(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Apply host template");
    }

    public ExtendedPollingResult startPollingStopRolesCommand(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Stop services on nodes");
    }

    public ExtendedPollingResult startPollingStartRolesCommand(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Start services on nodes");
    }

    public ExtendedPollingResult startPollingCmHostsRecommission(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to re-commission host with commandId: {}. [Server address: {}]", commandId, stack.getClusterManagerIp());
        long timeout = POLL_FOR_10_MINUTES;
        return pollCommandWithTimeListener(stack, apiClient, commandId, timeout,
                new NoExceptionOnTimeoutClouderaManagerListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "RecommissionHosts"));
    }

    public ExtendedPollingResult startPollingCmHostsDecommission(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId, long pollingTimeout) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, pollingTimeout,
                new NoExceptionOnTimeoutClouderaManagerListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "DecommissionHosts"));
    }

    public ExtendedPollingResult startPollingCmHostDecommissioning(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId,
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
                    new ClouderaManagerDecommissionWarningListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "Decommission host"));
        }
    }

    public ExtendedPollingResult startPollingCmManagementServiceStartup(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to start management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService,
                        "Start Cloudera Manager management service"));
    }

    public ExtendedPollingResult startPollingCmManagementServiceShutdown(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to stop management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService,
                        "Stop Cloudera Manager management service"));
    }

    public ExtendedPollingResult startPollingCmServicesRestart(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Restart services");
    }

    public ExtendedPollingResult startPollingParcelsApiAvailable(StackDtoDelegate stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Parcels API to become available. [Server address: {}]", stack.getClusterManagerIp());
        return pollApiWithTimeListener(stack, apiClient, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerParcelsApiListenerTask(clouderaManagerApiPojoFactory, clusterEventService));
    }

    public ExtendedPollingResult startPollingCdpRuntimeUpgrade(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId, boolean rollingUpgrade) {
        String commandName = "Upgrade CDP Runtime services";
        LOGGER.debug("Waiting for Cloudera Manager for [{}].", commandName);
        return pollCommandWithTimeListener(stack, apiClient, commandId,
                clouderaManagerUpgradePollingTimeoutProvider.getCdhUpgradeTimeout(stack, rollingUpgrade),
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, commandName));
    }

    public ExtendedPollingResult startPollingCdpRuntimeParcelDownload(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId,
            ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to download CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId,
                clouderaManagerUpgradePollingTimeoutProvider.getParcelDownloadTimeout(stack.getCloudPlatform()),
                new ClouderaManagerUpgradeParcelDownloadListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource));
    }

    public ExtendedPollingResult startPollingCdpRuntimeParcelDistribute(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId,
            ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to distribute CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId,
                clouderaManagerUpgradePollingTimeoutProvider.getParcelDistributeTimeout(stack.getCloudPlatform()),
                new ClouderaManagerUpgradeParcelDistributeListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource));
    }

    public ExtendedPollingResult startPollingCmGenerateCredentials(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Generate Credentials");
    }

    public ExtendedPollingResult startPollingCollectDiagnostics(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        return startDefaultPolling(stack, apiClient, commandId, "Collect diagnostics");
    }

    public ExtendedPollingResult startPollingRemoveHostsFromCluster(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to finish removal of hosts from cluster. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, ClouderaManagerPollingTimeoutProvider.getRemoveHostsTimeout(stack.getCloudPlatform()),
                new ClouderaManagerDefaultListenerTask(clouderaManagerApiPojoFactory, clusterEventService, "Remove hosts from cluster"));
    }

    public ExtendedPollingResult startPollingCommandList(StackDtoDelegate stack, ApiClient apiClient, List<BigDecimal> commandIds, String commandName) {
        LOGGER.debug("Waiting for Cloudera Manager to finish the following commands: {}. [Server address: {}]", commandIds, stack.getClusterManagerIp());
        return pollCommandListWithTimeListener(stack, apiClient, commandIds, ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(stack.getCloudPlatform()),
                new ClouderaManagerBatchCommandsListenerTask(clouderaManagerApiPojoFactory, clusterEventService, commandName));
    }

    public ExtendedPollingResult checkSyncApiCommandId(StackDtoDelegate stack, ApiClient apiClient, String commandName, BigDecimal recentCommandId,
            SyncApiCommandRetriever syncApiCommandRetriever) {
        LOGGER.debug("Waiting for Cloudera Manager until it will have a new deploy cluster client config command id. [Server address: {}]",
                stack.getClusterManagerIp());
        ClouderaManagerSyncCommandPollerObject pollerObject = new ClouderaManagerSyncCommandPollerObject(stack, apiClient, recentCommandId, commandName);
        ClouderaManagerSyncApiCommandIdCheckerTask listenerTask =
                new ClouderaManagerSyncApiCommandIdCheckerTask(clouderaManagerApiPojoFactory, syncApiCommandRetriever, clusterEventService);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                pollerObject,
                getPollInterval(stack, apiClient),
                ClouderaManagerPollingTimeoutProvider.getSyncApiCommandTimeout(stack.getCloudPlatform()));
    }

    private ExtendedPollingResult pollCommandListWithTimeListener(StackDtoDelegate stack, ApiClient apiClient, List<BigDecimal> commandIds,
            long maximumWaitTimeInSeconds, AbstractClouderaManagerCommandListCheckerTask<ClouderaManagerCommandListPollerObject> listenerTask) {
        ClouderaManagerCommandListPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandListPollerObject(stack, apiClient, commandIds);
        return clouderaManagerCommandListPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerCommandPollerObject,
                getPollInterval(stack, apiClient),
                maximumWaitTimeInSeconds);
    }

    private ExtendedPollingResult pollApiWithTimeListener(StackDtoDelegate stack, ApiClient apiClient, long maximumWaitTimeInSeconds,
            AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerPollerObject,
                getPollInterval(stack, apiClient),
                maximumWaitTimeInSeconds, MAX_CONSECUTIVE_FAILURES);
    }

    private ExtendedPollingResult pollCommandWithTimeListener(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId, long maximumWaitTimeInSeconds,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> listenerTask) {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAbsoluteTimeout(
                listenerTask,
                clouderaManagerCommandPollerObject,
                getPollInterval(stack, apiClient),
                maximumWaitTimeInSeconds);
    }

    private ExtendedPollingResult pollCommandWithAttemptListener(StackDtoDelegate stack, ApiClient apiClient, BigDecimal commandId, int numAttempts,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> listenerTask) {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAttempt(
                listenerTask,
                clouderaManagerCommandPollerObject,
                getPollInterval(stack, apiClient),
                numAttempts);
    }

    private ExtendedPollingResult pollApiWithAttemptListener(StackDtoDelegate stack, ApiClient apiClient, int numAttempts,
            AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerObjectPollingService.pollWithAttempt(
                listenerTask,
                clouderaManagerPollerObject,
                getPollInterval(stack, apiClient),
                numAttempts);
    }

    private int getPollInterval(StackDtoDelegate stackDtoDelegate, ApiClient apiClient) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        try {
            ApiHostList apiHostList = hostsResourceApi.readHosts(null, null, DataView.SUMMARY.name());
            int instanceCount = apiHostList.getItems().size();
            if (instanceCount < DEFAULT_BACKOFF_NODECOUNT_LIMIT) {
                return POLL_INTERVAL;
            }
            return POLL_INTERVAL + (instanceCount * BACKOFF_NODE_COUNT_MULTIPLIER);
        } catch (ApiException e) {
            LOGGER.warn("Could not fetch hosts from Cloudera Manager, go with default poll interval", e);
        }
        return POLL_INTERVAL;
    }
}
